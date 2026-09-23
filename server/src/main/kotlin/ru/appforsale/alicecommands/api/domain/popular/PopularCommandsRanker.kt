package ru.appforsale.alicecommands.api.domain.popular

import java.time.ZoneId

/**
 * Pure ranking policy for popular commands pool.
 * Score = 3 * unique_tts + 1 * unique_view (unique install_id).
 */
object PopularCommandsRanker {
    const val WINDOW_DAYS = 7
    const val SERVED_POOL_LIMIT = 12
    const val HISTORY_TOP_LIMIT = 50
    const val MIN_SERVED_BEFORE_SEED = 6
    const val TTS_WEIGHT = 3
    const val VIEW_WEIGHT = 1
    const val HISTORY_RETENTION_DAYS = 180L

    /** Calendar zone for analytics windows (same as admin analytics). */
    val ZONE: ZoneId = ZoneId.of("Europe/Moscow")

    /** TTS sources that would create a feedback loop if counted. */
    val EXCLUDED_TTS_SOURCES: Set<String> = setOf("try_now", "quick", "search")

    /** Must stay in sync with app [OrganicPopularCommands.IDS] / [PopularCommandsSeedCanon]. */
    val SEED_IDS: List<String> = PopularCommandsSeedCanon.SEED_IDS

    /** Must stay in sync with Flyway V11 denylist seed / app [OrganicPopularCommands.CONTAMINATED_IDS]. */
    val DENYLIST_SEED_IDS: Set<String> = PopularCommandsSeedCanon.DENYLIST_IDS

    val LIGHT_PAIR: Set<String> = setOf("sh_light_dim", "sh_light_off")

    data class Metric(
        val commandId: String,
        val uniqueTts: Int,
        val uniqueView: Int,
    ) {
        val score: Int get() = TTS_WEIGHT * uniqueTts + VIEW_WEIGHT * uniqueView
    }

    data class RankedItem(
        val commandId: String,
        val source: String,
        val uniqueTts: Int,
        val uniqueView: Int,
        val score: Int,
        val inServedPool: Boolean,
    )

    data class RankResult(
        val served: List<RankedItem>,
        val historyItems: List<RankedItem>,
    )

    fun score(uniqueTts: Int, uniqueView: Int): Int =
        TTS_WEIGHT * uniqueTts + VIEW_WEIGHT * uniqueView

    /**
     * Collapse light pair to the higher-score id; drop denylist / missing catalog ids.
     */
    fun filterMetrics(
        metrics: List<Metric>,
        liveCatalogIds: Set<String>,
        denylist: Set<String>,
    ): List<Metric> {
        val eligible = metrics
            .filter { it.commandId.isNotBlank() }
            .filter { it.commandId in liveCatalogIds }
            .filterNot { it.commandId in denylist }
            .sortedWith(compareByDescending<Metric> { it.score }.thenBy { it.commandId })

        var sawLight = false
        return eligible.filter { m ->
            if (m.commandId in LIGHT_PAIR) {
                if (sawLight) return@filter false
                sawLight = true
            }
            true
        }
    }

    fun build(
        pins: List<String>,
        metrics: List<Metric>,
        liveCatalogIds: Set<String>,
        denylist: Set<String>,
    ): RankResult {
        val filtered = filterMetrics(metrics, liveCatalogIds, denylist)
        val metricsById = filtered.associateBy { it.commandId }

        val pinIds = pins
            .filter { it in liveCatalogIds }
            .filterNot { it in denylist }
            .distinct()

        val served = mutableListOf<RankedItem>()
        val used = mutableSetOf<String>()

        for (id in pinIds) {
            if (served.size >= SERVED_POOL_LIMIT) break
            if (!used.add(id)) continue
            val m = metricsById[id]
            served += RankedItem(
                commandId = id,
                source = "pinned",
                uniqueTts = m?.uniqueTts ?: 0,
                uniqueView = m?.uniqueView ?: 0,
                score = m?.score ?: 0,
                inServedPool = true,
            )
        }

        for (m in filtered) {
            if (served.size >= SERVED_POOL_LIMIT) break
            if (!used.add(m.commandId)) continue
            served += RankedItem(
                commandId = m.commandId,
                source = "analytics",
                uniqueTts = m.uniqueTts,
                uniqueView = m.uniqueView,
                score = m.score,
                inServedPool = true,
            )
        }

        if (served.size < MIN_SERVED_BEFORE_SEED) {
            var sawLightInSeed = served.any { it.commandId in LIGHT_PAIR }
            for (id in SEED_IDS) {
                if (served.size >= SERVED_POOL_LIMIT) break
                if (id !in liveCatalogIds || id in denylist) continue
                if (id in LIGHT_PAIR) {
                    if (sawLightInSeed) continue
                    sawLightInSeed = true
                }
                if (!used.add(id)) continue
                val m = metricsById[id]
                served += RankedItem(
                    commandId = id,
                    source = "seed",
                    uniqueTts = m?.uniqueTts ?: 0,
                    uniqueView = m?.uniqueView ?: 0,
                    score = m?.score ?: 0,
                    inServedPool = true,
                )
            }
        }

        val servedIds = served.map { it.commandId }.toSet()

        val history = mutableListOf<RankedItem>()
        val historyUsed = mutableSetOf<String>()

        for (id in pinIds) {
            if (!historyUsed.add(id)) continue
            val m = metricsById[id]
            history += RankedItem(
                commandId = id,
                source = "pinned",
                uniqueTts = m?.uniqueTts ?: 0,
                uniqueView = m?.uniqueView ?: 0,
                score = m?.score ?: 0,
                inServedPool = id in servedIds,
            )
        }

        for (m in filtered.take(HISTORY_TOP_LIMIT)) {
            if (!historyUsed.add(m.commandId)) {
                continue
            }
            history += RankedItem(
                commandId = m.commandId,
                source = if (m.commandId in servedIds &&
                    served.firstOrNull { it.commandId == m.commandId }?.source == "seed"
                ) {
                    "seed"
                } else {
                    "analytics"
                },
                uniqueTts = m.uniqueTts,
                uniqueView = m.uniqueView,
                score = m.score,
                inServedPool = m.commandId in servedIds,
            )
        }

        for (item in served) {
            if (item.source != "seed") continue
            if (!historyUsed.add(item.commandId)) continue
            history += item
        }

        return RankResult(served = served, historyItems = history)
    }
}
