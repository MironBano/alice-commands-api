package ru.appforsale.alicecommands.api.domain.ports

data class LiveCatalogSnapshot(
    val commandIds: Set<String>,
    val titlesById: Map<String, String>,
) {
    fun titlesFor(ids: Set<String> = commandIds): Map<String, String> =
        ids.mapNotNull { id -> titlesById[id]?.let { id to it } }.toMap()
}

/**
 * Read-only view of the live published catalog for popular ranking.
 */
interface LiveCatalogReader {
    fun loadLiveCatalog(): LiveCatalogSnapshot

    fun liveCommandIds(): Set<String> = loadLiveCatalog().commandIds

    fun commandTitles(liveIds: Set<String>? = null): Map<String, String> {
        val catalog = loadLiveCatalog()
        return catalog.titlesFor(liveIds ?: catalog.commandIds)
    }
}
