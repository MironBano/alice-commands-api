package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.popular.RankPopularCommandsUseCase
import ru.appforsale.alicecommands.api.domain.ports.LiveCatalogReader
import ru.appforsale.alicecommands.api.domain.ports.LiveCatalogSnapshot
import ru.appforsale.alicecommands.api.domain.ports.PopularCommandsRepository
import ru.appforsale.alicecommands.api.domain.ports.PopularPinRow
import ru.appforsale.alicecommands.api.domain.ports.PopularRankRunItemRow
import ru.appforsale.alicecommands.api.domain.ports.PopularRankRunRow
import ru.appforsale.alicecommands.api.domain.ports.PopularSnapshotRow
import ru.appforsale.alicecommands.api.domain.popular.PopularCommandsRanker
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class RankPopularCommandsUseCaseTest {

    @Test
    fun `abort keeps snapshot when live catalog empty`() {
        val repository = RecordingPopularCommandsRepository(
            initialSnapshot = listOf(snapshotRow("music_muzyka")),
        )
        val liveCatalogReader = FakeLiveCatalogReader(emptySet())
        val useCase = RankPopularCommandsUseCase(
            repository = repository,
            liveCatalogReader = liveCatalogReader,
        )

        useCase.execute(trigger = "recompute")

        assertEquals(0, repository.replaceSnapshotCalls)
        assertEquals(0, repository.insertRankRunCalls)
        assertEquals(1, liveCatalogReader.loadCount)
        assertEquals(listOf("music_muzyka"), repository.getSnapshot().map { it.commandId })
    }

    @Test
    fun `abort keeps snapshot when served empty but snapshot non-empty`() {
        val repository = RecordingPopularCommandsRepository(
            initialSnapshot = listOf(snapshotRow("music_muzyka")),
            denylist = PopularCommandsRanker.SEED_IDS.toSet(),
        )
        val liveCatalogReader = FakeLiveCatalogReader(PopularCommandsRanker.SEED_IDS.toSet())
        val useCase = RankPopularCommandsUseCase(
            repository = repository,
            liveCatalogReader = liveCatalogReader,
        )

        useCase.execute(trigger = "ticker")

        assertEquals(0, repository.replaceSnapshotCalls)
        assertEquals(1, liveCatalogReader.loadCount)
        assertEquals(listOf("music_muzyka"), repository.getSnapshot().map { it.commandId })
    }

    @Test
    fun `recompute writes snapshot when catalog and metrics available`() {
        val liveIds = setOf("hot_cmd", "music_muzyka")
        val repository = RecordingPopularCommandsRepository(
            metrics = listOf(
                PopularCommandsRanker.Metric("hot_cmd", uniqueTts = 5, uniqueView = 2),
            ),
        )
        val liveCatalogReader = FakeLiveCatalogReader(liveIds)
        val useCase = RankPopularCommandsUseCase(
            repository = repository,
            liveCatalogReader = liveCatalogReader,
        )

        val response = useCase.execute(trigger = "recompute")

        assertEquals(1, repository.replaceSnapshotCalls)
        assertEquals(1, repository.insertRankRunCalls)
        assertEquals(1, liveCatalogReader.loadCount)
        assertTrue(response.snapshot.any { it.command_id == "hot_cmd" })
        assertEquals("hot_cmd", repository.getSnapshot().first().commandId)
    }

    private fun snapshotRow(id: String) = PopularSnapshotRow(
        sortOrder = 0,
        commandId = id,
        source = "seed",
        uniqueTts = 0,
        uniqueView = 0,
        score = 0,
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC),
    )

    private class FakeLiveCatalogReader(
        private val ids: Set<String>,
    ) : LiveCatalogReader {
        var loadCount = 0
            private set

        override fun loadLiveCatalog(): LiveCatalogSnapshot {
            loadCount++
            return LiveCatalogSnapshot(
                commandIds = ids,
                titlesById = ids.associateWith { "Title $it" },
            )
        }
    }

    private class RecordingPopularCommandsRepository(
        initialSnapshot: List<PopularSnapshotRow> = emptyList(),
        private val metrics: List<PopularCommandsRanker.Metric> = emptyList(),
        private val denylist: Set<String> = PopularCommandsRanker.DENYLIST_SEED_IDS,
    ) : PopularCommandsRepository {
        private var snapshot = initialSnapshot.toMutableList()
        var replaceSnapshotCalls = 0
            private set
        var insertRankRunCalls = 0
            private set

        override fun listPinsOrdered(): List<PopularPinRow> = emptyList()

        override fun replacePins(commandIds: List<String>, createdBy: String?) = Unit

        override fun listDenylistIds(): Set<String> = denylist

        override fun queryCommandMetrics(
            from: OffsetDateTime,
            to: OffsetDateTime,
        ): List<PopularCommandsRanker.Metric> = metrics

        override fun replaceSnapshot(
            items: List<PopularCommandsRanker.RankedItem>,
            updatedAt: OffsetDateTime,
        ) {
            replaceSnapshotCalls++
            snapshot = items.mapIndexed { index, item ->
                PopularSnapshotRow(
                    sortOrder = index,
                    commandId = item.commandId,
                    source = item.source,
                    uniqueTts = item.uniqueTts,
                    uniqueView = item.uniqueView,
                    score = item.score,
                    updatedAt = updatedAt,
                )
            }.toMutableList()
        }

        override fun getSnapshot(): List<PopularSnapshotRow> = snapshot.toList()

        override fun insertRankRun(
            windowFrom: LocalDate,
            windowTo: LocalDate,
            windowDays: Int,
            trigger: String,
            computedBy: String?,
            historyItems: List<PopularCommandsRanker.RankedItem>,
        ): Long {
            insertRankRunCalls++
            return 1L
        }

        override fun listRankRuns(limit: Int, offset: Int): Pair<List<PopularRankRunRow>, Int> =
            emptyList<PopularRankRunRow>() to 0

        override fun getRankRun(runId: Long): PopularRankRunRow? = null

        override fun listRankRunItems(runId: Long): List<PopularRankRunItemRow> = emptyList()

        override fun pruneRankRunsOlderThan(cutoff: OffsetDateTime): Int = 0

        override fun <T> withExclusiveRankLock(block: () -> T): T = block()
    }
}
