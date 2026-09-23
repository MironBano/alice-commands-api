package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.domain.popular.PopularCommandsRanker

class PopularCommandsRankerTest {

    @Test
    fun score_weightsTtsHigherThanView() {
        assertEquals(3, PopularCommandsRanker.score(1, 0))
        assertEquals(1, PopularCommandsRanker.score(0, 1))
        assertEquals(7, PopularCommandsRanker.score(2, 1))
    }

    @Test
    fun build_pinsFirstThenAnalyticsThenSeed() {
        val live = setOf(
            "pin_a", "pin_b", "hot_1", "hot_2",
            "music_muzyka", "music_luchshie_kompozitsii",
        )
        val metrics = listOf(
            PopularCommandsRanker.Metric("hot_1", uniqueTts = 10, uniqueView = 5),
            PopularCommandsRanker.Metric("hot_2", uniqueTts = 5, uniqueView = 1),
            PopularCommandsRanker.Metric("music_muzyka", uniqueTts = 1, uniqueView = 0),
        )
        val result = PopularCommandsRanker.build(
            pins = listOf("pin_b", "pin_a"),
            metrics = metrics,
            liveCatalogIds = live,
            denylist = emptySet(),
        )
        assertEquals(listOf("pin_b", "pin_a", "hot_1", "hot_2", "music_muzyka"), result.served.take(5).map { it.commandId })
        assertEquals("pinned", result.served[0].source)
        assertEquals("analytics", result.served[2].source)
        // Below MIN_SERVED_BEFORE_SEED → seed pad may append more
        assertTrue(result.served.size >= PopularCommandsRanker.MIN_SERVED_BEFORE_SEED)
    }

    @Test
    fun build_excludesDenylistAndMissingCatalog() {
        val result = PopularCommandsRanker.build(
            pins = emptyList(),
            metrics = listOf(
                PopularCommandsRanker.Metric("ok", 5, 0),
                PopularCommandsRanker.Metric("bad", 99, 0),
                PopularCommandsRanker.Metric("gone", 50, 0),
            ),
            liveCatalogIds = setOf("ok", "bad", "music_muzyka"),
            denylist = setOf("bad"),
        )
        assertEquals(listOf("ok"), result.served.filter { it.source == "analytics" }.map { it.commandId })
        assertFalse(result.served.any { it.commandId == "gone" })
        assertFalse(result.served.any { it.commandId == "bad" })
    }

    @Test
    fun build_collapsesLightPairToHigherScore() {
        val result = PopularCommandsRanker.build(
            pins = emptyList(),
            metrics = listOf(
                PopularCommandsRanker.Metric("sh_light_off", uniqueTts = 2, uniqueView = 0),
                PopularCommandsRanker.Metric("sh_light_dim", uniqueTts = 5, uniqueView = 0),
            ),
            liveCatalogIds = setOf("sh_light_dim", "sh_light_off", "music_muzyka"),
            denylist = emptySet(),
        )
        val lights = result.served.filter { it.commandId in PopularCommandsRanker.LIGHT_PAIR }
        assertEquals(1, lights.size)
        assertEquals("sh_light_dim", lights.single().commandId)
    }

    @Test
    fun build_padsWithSeedWhenBelowMin() {
        val result = PopularCommandsRanker.build(
            pins = emptyList(),
            metrics = listOf(
                PopularCommandsRanker.Metric("only_one", uniqueTts = 1, uniqueView = 0),
            ),
            liveCatalogIds = setOf(
                "only_one",
                "music_luchshie_kompozitsii",
                "music_muzyka",
                "smart_home_vkliuchi_girliandu",
                "sh_light_dim",
                "calls_pozvoni",
            ),
            denylist = emptySet(),
        )
        assertTrue(result.served.size >= PopularCommandsRanker.MIN_SERVED_BEFORE_SEED)
        assertTrue(result.served.any { it.source == "seed" })
        assertEquals("only_one", result.served.first().commandId)
    }

    @Test
    fun build_historyIncludesNearMissWithFlag() {
        val many = (1..20).map { i ->
            PopularCommandsRanker.Metric("cmd_$i", uniqueTts = 20 - i, uniqueView = 0)
        }
        val live = many.map { it.commandId }.toSet() + setOf("pin_x")
        val result = PopularCommandsRanker.build(
            pins = listOf("pin_x"),
            metrics = many,
            liveCatalogIds = live,
            denylist = emptySet(),
        )
        assertTrue(result.historyItems.size > result.served.size)
        val nearMiss = result.historyItems.first { !it.inServedPool }
        assertTrue(nearMiss.commandId.startsWith("cmd_"))
        assertTrue(result.historyItems.filter { it.inServedPool }.map { it.commandId }.toSet()
            .containsAll(result.served.map { it.commandId }))
    }
}
