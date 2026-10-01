package ru.appforsale.alicecommands.api.application.affiliate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import ru.appforsale.alicecommands.api.application.publish.PublishSmartHomeDevicesUseCase
import ru.appforsale.alicecommands.api.application.publish.SmartHomeDevicesValidationUseCase
import ru.appforsale.alicecommands.api.domain.DevicePick
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Refreshes device pick card fields from Yandex Market Affiliate API for known SKUs.
 * Human assigns primary/backup URLs in [pickSkuMapPath]; cron keeps title/price/url alive.
 *
 * Staging-first: enable via MARKET_AFFILIATE_REFRESH_ENABLED. Without OAuth → dry-run only.
 */
class RefreshAffiliatePicksUseCase(
    private val draftRepository: DraftRepository,
    private val publishSmartHomeDevicesUseCase: PublishSmartHomeDevicesUseCase,
    private val pickValidator: SmartHomeDevicesValidationUseCase,
    private val pickSkuMapPath: Path,
    private val oauthToken: String,
    private val clid: String,
    private val enabled: Boolean,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val linkCreateBaseUrl: String =
        "https://api.content.market.yandex.ru/v3/affiliate/partner/link/create",
) {
    private val log = LoggerFactory.getLogger("RefreshAffiliatePicks")

    data class Result(
        val skipped: Boolean,
        val reason: String? = null,
        val refreshed: Int = 0,
        val failedOver: Int = 0,
        val deactivated: Int = 0,
    )

    fun execute(): Result {
        if (!enabled) {
            return Result(skipped = true, reason = "MARKET_AFFILIATE_REFRESH_ENABLED=false")
        }
        val map = loadMap()
        if (map.picks.isEmpty()) {
            return Result(skipped = true, reason = "empty pick SKU map at $pickSkuMapPath")
        }
        if (oauthToken.isBlank() || clid.isBlank()) {
            log.info(
                "Dry-run: {} SKU slots mapped; set MARKET_AFFILIATE_OAUTH_TOKEN and MARKET_AFFILIATE_CLID to refresh",
                map.picks.size,
            )
            return Result(skipped = true, reason = "oauth_or_clid_missing", refreshed = 0)
        }

        val existing = draftRepository.listDevicePicks().associateBy { it.id }
        var refreshed = 0
        var failedOver = 0
        var deactivated = 0
        var dirty = false

        for (slot in map.picks) {
            val pick = existing[slot.pickId] ?: continue
            val primary = refreshOffer(slot.pickId, slot.marketUrlPrimary, slot.erid)
            val offer = when {
                primary != null && primary.stockOk && primary.promiseOk -> primary
                else -> {
                    val backupUrl = slot.marketUrlBackup?.takeIf { it.isNotBlank() }
                    if (backupUrl != null) {
                        val backup = refreshOffer(slot.pickId, backupUrl, slot.erid)
                        if (backup != null && backup.stockOk && backup.promiseOk) {
                            failedOver++
                            backup
                        } else null
                    } else null
                }
            }

            if (offer == null) {
                if (pick.placements.isNotEmpty()) {
                    draftRepository.updateDevicePick(pick.copy(placements = emptyList()))
                    dirty = true
                    deactivated++
                    log.warn("Deactivated pick {} — no live primary/backup offer", slot.pickId)
                }
                continue
            }

            val updated = pick.copy(
                title_ru = offer.title?.takeIf { it.isNotBlank() } ?: pick.title_ru,
                // Keep admin-managed price_hint (often null — no in-app price line).
                price_hint_ru = pick.price_hint_ru,
                action_url = offer.wrappedUrl,
                erid = slot.erid.ifBlank { pick.erid },
                advertiser_name = slot.advertiserName.ifBlank {
                    pick.advertiser_name ?: "ООО «Яндекс Маркет»"
                },
                // Do not write mds.yandex.net into image_url — Coil/CDN allowlist rejects it and would break publish.
                image_url = pick.image_url,
            )
            val pickErrors = pickValidator.validatePick(updated)
            if (pickErrors.isNotEmpty()) {
                log.warn(
                    "Skip draft update for {}: validation failed {}",
                    slot.pickId,
                    pickErrors.joinToString("; "),
                )
                continue
            }
            if (updated != pick) {
                draftRepository.updateDevicePick(updated)
                dirty = true
                refreshed++
            }
        }

        if (dirty) {
            publishSmartHomeDevicesUseCase.execute()
        }
        return Result(
            skipped = false,
            refreshed = refreshed,
            failedOver = failedOver,
            deactivated = deactivated,
        )
    }

    private fun loadMap(): PickSkuMap {
        if (!pickSkuMapPath.exists()) return PickSkuMap()
        return runCatching {
            json.decodeFromString<PickSkuMap>(pickSkuMapPath.readText())
        }.getOrElse {
            log.warn("Failed to read {}: {}", pickSkuMapPath, it.message)
            PickSkuMap()
        }
    }

    private fun refreshOffer(pickId: String, marketUrl: String, erid: String): LiveOffer? {
        val encodedUrl = URLEncoder.encode(marketUrl, StandardCharsets.UTF_8)
        val encodedErid = URLEncoder.encode(erid, StandardCharsets.UTF_8)
        val encodedVid = URLEncoder.encode(pickId, StandardCharsets.UTF_8)
        val uri = URI.create(
            "$linkCreateBaseUrl?url=$encodedUrl&clid=$clid&erid=$encodedErid&vid=$encodedVid",
        )
        val request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "OAuth $oauthToken")
            .GET()
            .build()
        val response = runCatching {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        }.getOrElse {
            log.warn("link/create failed for {}: {}", marketUrl, it.message)
            return null
        }
        if (response.statusCode() !in 200..299) {
            log.warn("link/create HTTP {} for {}", response.statusCode(), marketUrl)
            return null
        }
        val body = runCatching {
            json.decodeFromString<LinkCreateResponse>(response.body())
        }.getOrElse {
            log.warn("link/create parse failed: {}", it.message)
            return null
        }
        val wrapped = body.url ?: body.shortUrl ?: return null
        val stock = body.stockAmount ?: 1
        val promiseOk = body.promise == null || body.promise > 0
        return LiveOffer(
            wrappedUrl = wrapped,
            title = body.title,
            priceHint = body.price?.let { "${it.toInt()} ₽" },
            imageUrl = body.productPhoto,
            stockOk = stock > 0,
            promiseOk = promiseOk,
        )
    }

    @Serializable
    data class PickSkuMap(
        val picks: List<PickSkuSlot> = emptyList(),
    )

    @Serializable
    data class PickSkuSlot(
        @SerialName("pick_id") val pickId: String,
        @SerialName("market_url_primary") val marketUrlPrimary: String,
        @SerialName("market_url_backup") val marketUrlBackup: String? = null,
        val erid: String = "",
        @SerialName("advertiser_name") val advertiserName: String = "ООО «Яндекс Маркет»",
    )

    @Serializable
    private data class LinkCreateResponse(
        val url: String? = null,
        val shortUrl: String? = null,
        val title: String? = null,
        val productPhoto: String? = null,
        val price: Double? = null,
        val stockAmount: Int? = null,
        val promise: Double? = null,
    )

    private data class LiveOffer(
        val wrappedUrl: String,
        val title: String?,
        val priceHint: String?,
        val imageUrl: String?,
        val stockOk: Boolean,
        val promiseOk: Boolean,
    )
}
