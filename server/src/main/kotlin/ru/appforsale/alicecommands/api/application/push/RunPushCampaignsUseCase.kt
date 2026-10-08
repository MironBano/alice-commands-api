package ru.appforsale.alicecommands.api.application.push

import org.slf4j.LoggerFactory
import ru.appforsale.alicecommands.api.application.read.BundleService
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsEventRepository
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository
import ru.appforsale.alicecommands.api.domain.ports.PopularCommandsRepository
import ru.appforsale.alicecommands.api.domain.ports.PushTokenRepository
import ru.appforsale.alicecommands.api.domain.push.PushCampaignContext
import ru.appforsale.alicecommands.api.domain.push.PushCampaignEvaluator
import ru.appforsale.alicecommands.api.domain.push.PushCampaignPriority
import ru.appforsale.alicecommands.api.infrastructure.push.FcmPushErrors
import ru.appforsale.alicecommands.api.infrastructure.push.ProviderAwarePushSender
import ru.appforsale.alicecommands.api.infrastructure.push.PushDeliveryQuarantine
import ru.appforsale.alicecommands.api.infrastructure.push.RuStorePushErrors
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class RunPushCampaignsUseCase(
    private val pushTokenRepository: PushTokenRepository,
    private val analyticsEventRepository: AnalyticsEventRepository,
    private val popularCommandsRepository: PopularCommandsRepository,
    private val manifestRepository: ManifestRepository,
    private val bundleService: BundleService,
    private val pushSender: ProviderAwarePushSender,
) {
    private val log = LoggerFactory.getLogger(RunPushCampaignsUseCase::class.java)

    data class Result(
        val scanned: Int,
        val sent: Int,
        val skipped: Int,
        val failed: Int,
    )

    fun execute(now: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)): Result =
        pushTokenRepository.withExclusiveCampaignLock {
            executeUnlocked(now)
        }

    private fun executeUnlocked(now: OffsetDateTime): Result {
        val ctx = buildContext(now) ?: return Result(0, 0, 0, 0)
        val tokens = pushTokenRepository.listActive()
        var sent = 0
        var skipped = 0
        var failed = 0
        var sendAttempts = 0
        val quarantineCandidates = linkedSetOf<String>()
        for (token in tokens) {
            val zone = runCatching { java.time.ZoneId.of(token.timezone) }
                .getOrDefault(java.time.ZoneId.of("Europe/Moscow"))
            val dayStartLocal = now.atZoneSameInstant(zone).toLocalDate().atStartOfDay(zone)
            val dayStartUtc = dayStartLocal.toOffsetDateTime()
            val signals = analyticsEventRepository.loadPushUserSignals(
                installId = token.installId,
                dayStartUtc = dayStartUtc,
                appInstalledAt = token.appInstalledAt ?: token.createdAt,
            )
            val candidates = PushCampaignEvaluator.evaluateCandidates(token, ctx, signals).toMutableSet()
            var sentThisToken = false
            while (candidates.isNotEmpty() && !sentThisToken) {
                val scenario = PushCampaignPriority.pickHighest(candidates) ?: break
                candidates.remove(scenario)
                val candidate = PushCampaignEvaluator.buildCandidate(scenario, token, ctx)
                if (candidate == null) continue
                val data = buildMap {
                    put("scenario", candidate.scenario)
                    put("push_scenario", candidate.scenario)
                    put("deeplink", candidate.deeplink)
                    candidate.commandId?.let { put("command_id", it) }
                }
                sendAttempts++
                val sendResult = pushSender.send(
                    provider = token.provider,
                    token = token.rustoreToken,
                    title = candidate.title,
                    body = candidate.body,
                    data = data,
                    channelId = candidate.channelId,
                    deeplink = candidate.deeplink,
                )
                if (sendResult.isFailure) {
                    failed++
                    val err = sendResult.exceptionOrNull()?.message.orEmpty()
                    log.warn(
                        "push send failed installId={} scenario={} err={} — defer remaining",
                        token.installId,
                        scenario,
                        err,
                    )
                    if (err == RuStorePushErrors.TOKEN_NOT_FOUND ||
                        err == FcmPushErrors.TOKEN_NOT_FOUND
                    ) {
                        quarantineCandidates += token.installId
                    }
                    // Do not promote lower-priority candidates (would burn S4/S5 once flags).
                    break
                }
                val weekBucket = PushCampaignEvaluator.isoWeekBucket(now, zone)
                pushTokenRepository.markSent(
                    installId = token.installId,
                    scenario = scenario,
                    now = now,
                    weekBucket = weekBucket,
                    popularHash = if (scenario == "s3") ctx.popularHash else null,
                    notifiedContentVersion = if (scenario == "s1") ctx.publishedContentVersion else null,
                )
                log.info("push sent installId={} scenario={}", token.installId, scenario)
                sent++
                sentThisToken = true
            }
            if (!sentThisToken) skipped++
        }
        applyQuarantine(quarantineCandidates, sendAttempts)
        log.info(
            "push campaign done scanned={} sent={} skipped={} failed={}",
            tokens.size,
            sent,
            skipped,
            failed,
        )
        return Result(scanned = tokens.size, sent = sent, skipped = skipped, failed = failed)
    }

    private fun applyQuarantine(candidates: Set<String>, sendAttempts: Int) {
        if (candidates.isEmpty()) return
        if (!PushDeliveryQuarantine.shouldApply(candidates.size, sendAttempts)) {
            log.warn(
                "push quarantine skipped: token_not_found={} send_attempts={} — possible RuStore misconfig",
                candidates.size,
                sendAttempts,
            )
            return
        }
        for (installId in candidates) {
            pushTokenRepository.markDeliveryBlocked(installId, RuStorePushErrors.TOKEN_NOT_FOUND)
        }
    }

    private fun buildContext(now: OffsetDateTime): PushCampaignContext? {
        val manifest = manifestRepository.getCurrent() ?: return null
        val publishedAt = runCatching {
            OffsetDateTime.parse(manifest.publishedAt)
        }.getOrNull()
            ?: runCatching {
                java.time.Instant.parse(manifest.publishedAt).atOffset(ZoneOffset.UTC)
            }.getOrNull()

        val bundleBytes = bundleService.getPublishedBundle()?.bytes
        val codId = bundleBytes?.let { PublishedBundleReader.extractCommandOfDayId(it) }
        if (codId.isNullOrBlank()) {
            log.warn(
                "push campaign: command_of_day missing or unreadable (bundleBytes={})",
                bundleBytes?.size ?: 0,
            )
        }

        val denylist = popularCommandsRepository.listDenylistIds()
        val popular = popularCommandsRepository.getSnapshot()
            .asSequence()
            .map { it.commandId }
            .filterNot { it in denylist }
            .filterNot { it.startsWith("checklist_") }
            .firstOrNull()
        val popularHash = popular?.let {
            hashIds(listOf(it) + popularCommandsRepository.getSnapshot().map { row -> row.commandId }.take(6))
        }

        return PushCampaignContext(
            nowUtc = now.truncatedTo(ChronoUnit.MINUTES),
            publishedContentVersion = manifest.contentVersion,
            codCommandId = codId,
            popularCommandId = popular,
            popularHash = popularHash,
            contentPublishedAt = publishedAt,
        )
    }

    private fun hashIds(ids: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(ids.joinToString(",").toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

}
