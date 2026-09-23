package ru.appforsale.alicecommands.api.application.popular

import org.slf4j.LoggerFactory
import ru.appforsale.alicecommands.api.domain.PopularCommandPinDto
import ru.appforsale.alicecommands.api.domain.PopularCommandPublicItem
import ru.appforsale.alicecommands.api.domain.PopularCommandSnapshotItemDto
import ru.appforsale.alicecommands.api.domain.PopularCommandsAdminResponse
import ru.appforsale.alicecommands.api.domain.PopularCommandsPublicResponse
import ru.appforsale.alicecommands.api.domain.PopularRankHistoryListResponse
import ru.appforsale.alicecommands.api.domain.PopularRankRunDetailDto
import ru.appforsale.alicecommands.api.domain.PopularRankRunItemDto
import ru.appforsale.alicecommands.api.domain.PopularRankRunSummaryDto
import ru.appforsale.alicecommands.api.domain.UpdatePopularPinsRequest
import ru.appforsale.alicecommands.api.domain.ports.LiveCatalogReader
import ru.appforsale.alicecommands.api.domain.ports.PopularCommandsRepository
import ru.appforsale.alicecommands.api.domain.popular.PopularCommandsRanker
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class RankPopularCommandsUseCase(
    private val repository: PopularCommandsRepository,
    private val liveCatalogReader: LiveCatalogReader,
) {
    private val log = LoggerFactory.getLogger(RankPopularCommandsUseCase::class.java)

    fun execute(trigger: String, computedBy: String? = null): PopularCommandsAdminResponse =
        repository.withExclusiveRankLock {
            val catalog = liveCatalogReader.loadLiveCatalog()
            val liveIds = catalog.commandIds
            if (liveIds.isEmpty()) {
                log.warn(
                    "popular rank abort: live catalog empty (trigger={}); keeping existing snapshot",
                    trigger,
                )
                return@withExclusiveRankLock buildAdminResponse(catalog.titlesFor(liveIds))
            }

            val zone = PopularCommandsRanker.ZONE
            val today = LocalDate.now(zone)
            val windowFrom = today.minusDays(PopularCommandsRanker.WINDOW_DAYS.toLong() - 1)
            val windowTo = today
            val from = windowFrom.atStartOfDay(zone).toOffsetDateTime()
            val to = windowTo.plusDays(1).atStartOfDay(zone).toOffsetDateTime().minusNanos(1)

            val denylist = repository.listDenylistIds()
            val pins = repository.listPinsOrdered().map { it.commandId }
            val metrics = repository.queryCommandMetrics(from, to)

            val result = PopularCommandsRanker.build(
                pins = pins,
                metrics = metrics,
                liveCatalogIds = liveIds,
                denylist = denylist,
            )

            val existingSnapshot = repository.getSnapshot()
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            if (result.served.isEmpty() && existingSnapshot.isNotEmpty()) {
                log.warn(
                    "popular rank abort: empty served pool with non-empty snapshot (trigger={}); keeping snapshot",
                    trigger,
                )
                return@withExclusiveRankLock buildAdminResponse(catalog.titlesFor(liveIds))
            }

            repository.replaceSnapshot(result.served, now)
            repository.insertRankRun(
                windowFrom = windowFrom,
                windowTo = windowTo,
                windowDays = PopularCommandsRanker.WINDOW_DAYS,
                trigger = trigger,
                computedBy = computedBy,
                historyItems = result.historyItems,
            )
            val cutoff = now.minusDays(PopularCommandsRanker.HISTORY_RETENTION_DAYS)
            repository.pruneRankRunsOlderThan(cutoff)

            buildAdminResponse(catalog.titlesFor(liveIds))
        }

    fun buildAdminResponse(titles: Map<String, String>? = null): PopularCommandsAdminResponse {
        val snapshot = repository.getSnapshot()
        val pins = repository.listPinsOrdered()
        val titleMap = titles ?: liveCatalogReader.loadLiveCatalog().titlesById
        return PopularCommandsAdminResponse(
            snapshot = snapshot.map {
                PopularCommandSnapshotItemDto(
                    sort_order = it.sortOrder,
                    command_id = it.commandId,
                    title_ru = titleMap[it.commandId],
                    source = it.source,
                    unique_tts = it.uniqueTts,
                    unique_view = it.uniqueView,
                    score = it.score,
                )
            },
            pins = pins.map {
                PopularCommandPinDto(
                    command_id = it.commandId,
                    sort_order = it.sortOrder,
                    title_ru = titleMap[it.commandId],
                )
            },
            updated_at = snapshot.maxOfOrNull { it.updatedAt }?.format(ISO_FORMAT),
            window_days = PopularCommandsRanker.WINDOW_DAYS,
        )
    }
}

class GetPopularCommandsPublicUseCase(
    private val repository: PopularCommandsRepository,
) {
    fun execute(): PopularCommandsPublicResponse {
        val snapshot = repository.getSnapshot()
        val updatedAt = snapshot.maxOfOrNull { it.updatedAt }?.format(ISO_FORMAT)
            ?: OffsetDateTime.now(ZoneOffset.UTC).format(ISO_FORMAT)
        return PopularCommandsPublicResponse(
            updated_at = updatedAt,
            window_days = PopularCommandsRanker.WINDOW_DAYS,
            commands = snapshot.map {
                PopularCommandPublicItem(
                    id = it.commandId,
                    source = it.source,
                    score = it.score,
                )
            },
        )
    }

    fun etag(): String? {
        val snapshot = repository.getSnapshot()
        val updatedAt = snapshot.maxOfOrNull { it.updatedAt } ?: return null
        return "\"popular-${updatedAt.toInstant().toEpochMilli()}\""
    }
}

class PopularCommandsAdminUseCase(
    private val repository: PopularCommandsRepository,
    private val rankUseCase: RankPopularCommandsUseCase,
    private val liveCatalogReader: LiveCatalogReader,
) {
    fun get(): PopularCommandsAdminResponse = rankUseCase.buildAdminResponse()

    fun updatePins(request: UpdatePopularPinsRequest, username: String): PopularCommandsAdminResponse {
        val liveIds = liveCatalogReader.liveCommandIds()
        val denylist = repository.listDenylistIds()
        val cleaned = request.command_ids
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .filter { it in liveIds }
            .filterNot { it in denylist }
        repository.replacePins(cleaned, username)
        return rankUseCase.execute(trigger = "pins", computedBy = username)
    }

    fun recompute(username: String?): PopularCommandsAdminResponse =
        rankUseCase.execute(trigger = "recompute", computedBy = username)

    fun listHistory(limit: Int, offset: Int): PopularRankHistoryListResponse {
        val cappedLimit = limit.coerceIn(1, 100)
        val cappedOffset = offset.coerceAtLeast(0)
        val (runs, total) = repository.listRankRuns(cappedLimit, cappedOffset)
        return PopularRankHistoryListResponse(
            items = runs.map { it.toSummaryDto() },
            total = total,
            limit = cappedLimit,
            offset = cappedOffset,
        )
    }

    fun getHistory(runId: Long): PopularRankRunDetailDto? {
        val run = repository.getRankRun(runId) ?: return null
        val titles = liveCatalogReader.commandTitles()
        val items = repository.listRankRunItems(runId).map {
            PopularRankRunItemDto(
                sort_order = it.sortOrder,
                command_id = it.commandId,
                title_ru = titles[it.commandId],
                source = it.source,
                unique_tts = it.uniqueTts,
                unique_view = it.uniqueView,
                score = it.score,
                in_served_pool = it.inServedPool,
            )
        }
        return PopularRankRunDetailDto(
            run = run.toSummaryDto(),
            items = items,
        )
    }
}

private fun ru.appforsale.alicecommands.api.domain.ports.PopularRankRunRow.toSummaryDto() =
    PopularRankRunSummaryDto(
        id = id,
        computed_at = computedAt.format(ISO_FORMAT),
        window_from = windowFrom.toString(),
        window_to = windowTo.toString(),
        window_days = windowDays,
        trigger = trigger,
        computed_by = computedBy,
        item_count = itemCount,
        served_count = servedCount,
    )

private val ISO_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
