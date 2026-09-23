package ru.appforsale.alicecommands.api.infrastructure.persistence

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.appforsale.alicecommands.api.domain.popular.PopularCommandsRanker
import ru.appforsale.alicecommands.api.domain.ports.PopularCommandsRepository
import ru.appforsale.alicecommands.api.domain.ports.PopularPinRow
import ru.appforsale.alicecommands.api.domain.ports.PopularRankRunItemRow
import ru.appforsale.alicecommands.api.domain.ports.PopularRankRunRow
import ru.appforsale.alicecommands.api.domain.ports.PopularSnapshotRow
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ExposedPopularCommandsRepository(
    private val database: Database,
) : PopularCommandsRepository {

    override fun listPinsOrdered(): List<PopularPinRow> = transaction(database) {
        PopularCommandPinsTable
            .selectAll()
            .orderBy(PopularCommandPinsTable.sortOrder to SortOrder.ASC)
            .map {
                PopularPinRow(
                    commandId = it[PopularCommandPinsTable.commandId],
                    sortOrder = it[PopularCommandPinsTable.sortOrder],
                    createdAt = it[PopularCommandPinsTable.createdAt],
                    createdBy = it[PopularCommandPinsTable.createdBy],
                )
            }
    }

    override fun replacePins(commandIds: List<String>, createdBy: String?) {
        transaction(database) {
            PopularCommandPinsTable.deleteAll()
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            commandIds.forEachIndexed { index, id ->
                PopularCommandPinsTable.insert {
                    it[commandId] = id
                    it[sortOrder] = index
                    it[createdAt] = now
                    it[PopularCommandPinsTable.createdBy] = createdBy
                }
            }
        }
    }

    override fun listDenylistIds(): Set<String> = transaction(database) {
        PopularCommandDenylistTable.selectAll()
            .map { it[PopularCommandDenylistTable.commandId] }
            .toSet()
    }

    override fun queryCommandMetrics(
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<PopularCommandsRanker.Metric> = transaction(database) {
        val conn = connection.connection as java.sql.Connection
        val excluded = PopularCommandsRanker.EXCLUDED_TTS_SOURCES.toList()
        // Build placeholders for excluded TTS sources
        val excludePlaceholders = excluded.joinToString(",") { "?" }
        val sql = """
            SELECT command_id,
                   COUNT(DISTINCT CASE WHEN event_name = 'command_tts' THEN install_id END)::int AS unique_tts,
                   COUNT(DISTINCT CASE WHEN event_name = 'command_view' THEN install_id END)::int AS unique_view
            FROM (
                SELECT
                    NULLIF(TRIM(params->>'command_id'), '') AS command_id,
                    install_id,
                    event_name
                FROM analytics_events
                WHERE occurred_at >= ?
                  AND occurred_at <= ?
                  AND event_name IN ('command_tts', 'command_view')
                  AND NULLIF(TRIM(params->>'command_id'), '') IS NOT NULL
                  AND NOT (
                    event_name = 'command_tts'
                    AND COALESCE(params->>'source', '') IN ($excludePlaceholders)
                  )
            ) t
            GROUP BY command_id
            HAVING COUNT(*) > 0
            ORDER BY (${PopularCommandsRanker.TTS_WEIGHT} * COUNT(DISTINCT CASE WHEN event_name = 'command_tts' THEN install_id END)
                    + ${PopularCommandsRanker.VIEW_WEIGHT} * COUNT(DISTINCT CASE WHEN event_name = 'command_view' THEN install_id END)) DESC,
                     command_id ASC
        """.trimIndent()
        val metrics = mutableListOf<PopularCommandsRanker.Metric>()
        conn.prepareStatement(sql).use { ps ->
            var idx = 1
            ps.setObject(idx++, from)
            ps.setObject(idx++, to)
            for (src in excluded) {
                ps.setString(idx++, src)
            }
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    metrics += PopularCommandsRanker.Metric(
                        commandId = rs.getString(1),
                        uniqueTts = rs.getInt(2),
                        uniqueView = rs.getInt(3),
                    )
                }
            }
        }
        metrics
    }

    override fun replaceSnapshot(
        items: List<PopularCommandsRanker.RankedItem>,
        updatedAt: OffsetDateTime,
    ) {
        transaction(database) {
            PopularCommandsSnapshotTable.deleteAll()
            items.forEachIndexed { index, item ->
                PopularCommandsSnapshotTable.insert {
                    it[sortOrder] = index
                    it[commandId] = item.commandId
                    it[sourceKind] = item.source
                    it[uniqueTts] = item.uniqueTts
                    it[uniqueView] = item.uniqueView
                    it[score] = item.score
                    it[PopularCommandsSnapshotTable.updatedAt] = updatedAt
                }
            }
        }
    }

    override fun getSnapshot(): List<PopularSnapshotRow> = transaction(database) {
        PopularCommandsSnapshotTable
            .selectAll()
            .orderBy(PopularCommandsSnapshotTable.sortOrder to SortOrder.ASC)
            .map {
                PopularSnapshotRow(
                    sortOrder = it[PopularCommandsSnapshotTable.sortOrder],
                    commandId = it[PopularCommandsSnapshotTable.commandId],
                    source = it[PopularCommandsSnapshotTable.sourceKind],
                    uniqueTts = it[PopularCommandsSnapshotTable.uniqueTts],
                    uniqueView = it[PopularCommandsSnapshotTable.uniqueView],
                    score = it[PopularCommandsSnapshotTable.score],
                    updatedAt = it[PopularCommandsSnapshotTable.updatedAt],
                )
            }
    }

    override fun insertRankRun(
        windowFrom: LocalDate,
        windowTo: LocalDate,
        windowDays: Int,
        trigger: String,
        computedBy: String?,
        historyItems: List<PopularCommandsRanker.RankedItem>,
    ): Long = transaction(database) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val runId = PopularRankRunsTable.insert { row ->
            row[PopularRankRunsTable.computedAt] = now
            row[PopularRankRunsTable.windowFrom] = windowFrom
            row[PopularRankRunsTable.windowTo] = windowTo
            row[PopularRankRunsTable.windowDays] = windowDays
            row[PopularRankRunsTable.triggerKind] = trigger
            row[PopularRankRunsTable.computedBy] = computedBy
        } get PopularRankRunsTable.id
        historyItems.forEachIndexed { index, item ->
            PopularRankRunItemsTable.insert {
                it[PopularRankRunItemsTable.runId] = runId
                it[sortOrder] = index
                it[commandId] = item.commandId
                it[sourceKind] = item.source
                it[uniqueTts] = item.uniqueTts
                it[uniqueView] = item.uniqueView
                it[score] = item.score
                it[inServedPool] = item.inServedPool
            }
        }
        runId
    }

    override fun listRankRuns(limit: Int, offset: Int): Pair<List<PopularRankRunRow>, Int> =
        transaction(database) {
            val total = PopularRankRunsTable.selectAll().count().toInt()
            val runs = PopularRankRunsTable
                .selectAll()
                .orderBy(PopularRankRunsTable.computedAt to SortOrder.DESC)
                .limit(limit, offset.toLong())
                .map { row ->
                    PopularRankRunRow(
                        id = row[PopularRankRunsTable.id],
                        computedAt = row[PopularRankRunsTable.computedAt],
                        windowFrom = row[PopularRankRunsTable.windowFrom],
                        windowTo = row[PopularRankRunsTable.windowTo],
                        windowDays = row[PopularRankRunsTable.windowDays],
                        trigger = row[PopularRankRunsTable.triggerKind],
                        computedBy = row[PopularRankRunsTable.computedBy],
                        itemCount = 0,
                        servedCount = 0,
                    )
                }
            val stats = fetchRunItemStats(connection.connection as java.sql.Connection, runs.map { it.id })
            runs.map { run ->
                val (itemCount, servedCount) = stats[run.id] ?: (0 to 0)
                run.copy(itemCount = itemCount, servedCount = servedCount)
            } to total
        }

    private fun fetchRunItemStats(conn: java.sql.Connection, runIds: List<Long>): Map<Long, Pair<Int, Int>> {
        if (runIds.isEmpty()) return emptyMap()
        val placeholders = runIds.joinToString(",") { "?" }
        val sql = """
            SELECT run_id,
                   COUNT(*)::int AS item_count,
                   COUNT(*) FILTER (WHERE in_served_pool)::int AS served_count
            FROM popular_rank_run_items
            WHERE run_id IN ($placeholders)
            GROUP BY run_id
        """.trimIndent()
        val stats = mutableMapOf<Long, Pair<Int, Int>>()
        conn.prepareStatement(sql).use { ps ->
            runIds.forEachIndexed { index, id -> ps.setLong(index + 1, id) }
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    stats[rs.getLong(1)] = rs.getInt(2) to rs.getInt(3)
                }
            }
        }
        return stats
    }

    override fun getRankRun(runId: Long): PopularRankRunRow? = transaction(database) {
        val row = PopularRankRunsTable
            .selectAll()
            .where { PopularRankRunsTable.id eq runId }
            .firstOrNull() ?: return@transaction null
        val items = PopularRankRunItemsTable
            .selectAll()
            .where { PopularRankRunItemsTable.runId eq runId }
            .toList()
        PopularRankRunRow(
            id = row[PopularRankRunsTable.id],
            computedAt = row[PopularRankRunsTable.computedAt],
            windowFrom = row[PopularRankRunsTable.windowFrom],
            windowTo = row[PopularRankRunsTable.windowTo],
            windowDays = row[PopularRankRunsTable.windowDays],
            trigger = row[PopularRankRunsTable.triggerKind],
            computedBy = row[PopularRankRunsTable.computedBy],
            itemCount = items.size,
            servedCount = items.count { it[PopularRankRunItemsTable.inServedPool] },
        )
    }

    override fun listRankRunItems(runId: Long): List<PopularRankRunItemRow> = transaction(database) {
        PopularRankRunItemsTable
            .selectAll()
            .where { PopularRankRunItemsTable.runId eq runId }
            .orderBy(PopularRankRunItemsTable.sortOrder to SortOrder.ASC)
            .map {
                PopularRankRunItemRow(
                    sortOrder = it[PopularRankRunItemsTable.sortOrder],
                    commandId = it[PopularRankRunItemsTable.commandId],
                    source = it[PopularRankRunItemsTable.sourceKind],
                    uniqueTts = it[PopularRankRunItemsTable.uniqueTts],
                    uniqueView = it[PopularRankRunItemsTable.uniqueView],
                    score = it[PopularRankRunItemsTable.score],
                    inServedPool = it[PopularRankRunItemsTable.inServedPool],
                )
            }
    }

    override fun pruneRankRunsOlderThan(cutoff: OffsetDateTime): Int = transaction(database) {
        PopularRankRunsTable.deleteWhere { computedAt less cutoff }
    }

    override fun <T> withExclusiveRankLock(block: () -> T): T = transaction(database) {
        val conn = connection.connection as java.sql.Connection
        conn.prepareStatement("SELECT pg_advisory_xact_lock(?)").use { ps ->
            ps.setLong(1, POPULAR_RANK_ADVISORY_LOCK_KEY)
            ps.execute()
        }
        block()
    }

    companion object {
        /** Stable advisory lock id for popular rank (survives multi-instance deploy). */
        private const val POPULAR_RANK_ADVISORY_LOCK_KEY = 8_427_635_901L
    }
}
