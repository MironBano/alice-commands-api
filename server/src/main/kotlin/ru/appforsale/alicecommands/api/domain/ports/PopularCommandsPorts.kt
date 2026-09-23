package ru.appforsale.alicecommands.api.domain.ports

import ru.appforsale.alicecommands.api.domain.popular.PopularCommandsRanker
import java.time.LocalDate
import java.time.OffsetDateTime

data class PopularPinRow(
    val commandId: String,
    val sortOrder: Int,
    val createdAt: OffsetDateTime,
    val createdBy: String?,
)

data class PopularSnapshotRow(
    val sortOrder: Int,
    val commandId: String,
    val source: String,
    val uniqueTts: Int,
    val uniqueView: Int,
    val score: Int,
    val updatedAt: OffsetDateTime,
)

data class PopularRankRunRow(
    val id: Long,
    val computedAt: OffsetDateTime,
    val windowFrom: LocalDate,
    val windowTo: LocalDate,
    val windowDays: Int,
    val trigger: String,
    val computedBy: String?,
    val itemCount: Int,
    val servedCount: Int,
)

data class PopularRankRunItemRow(
    val sortOrder: Int,
    val commandId: String,
    val source: String,
    val uniqueTts: Int,
    val uniqueView: Int,
    val score: Int,
    val inServedPool: Boolean,
)

interface PopularCommandsRepository {
    fun listPinsOrdered(): List<PopularPinRow>
    fun replacePins(commandIds: List<String>, createdBy: String?)
    fun listDenylistIds(): Set<String>
    fun queryCommandMetrics(from: OffsetDateTime, to: OffsetDateTime): List<PopularCommandsRanker.Metric>
    fun replaceSnapshot(items: List<PopularCommandsRanker.RankedItem>, updatedAt: OffsetDateTime)
    fun getSnapshot(): List<PopularSnapshotRow>
    fun insertRankRun(
        windowFrom: LocalDate,
        windowTo: LocalDate,
        windowDays: Int,
        trigger: String,
        computedBy: String?,
        historyItems: List<PopularCommandsRanker.RankedItem>,
    ): Long
    fun listRankRuns(limit: Int, offset: Int): Pair<List<PopularRankRunRow>, Int>
    fun getRankRun(runId: Long): PopularRankRunRow?
    fun listRankRunItems(runId: Long): List<PopularRankRunItemRow>
    fun pruneRankRunsOlderThan(cutoff: OffsetDateTime): Int
    fun <T> withExclusiveRankLock(block: () -> T): T
}
