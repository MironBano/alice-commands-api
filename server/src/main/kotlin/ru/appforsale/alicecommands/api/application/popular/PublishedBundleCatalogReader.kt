package ru.appforsale.alicecommands.api.application.popular

import ru.appforsale.alicecommands.api.domain.ports.LiveCatalogReader
import ru.appforsale.alicecommands.api.domain.ports.LiveCatalogSnapshot
import ru.appforsale.alicecommands.api.domain.ports.PublishedCatalogProvider

class PublishedBundleCatalogReader(
    private val catalogProvider: PublishedCatalogProvider,
) : LiveCatalogReader {
    override fun loadLiveCatalog(): LiveCatalogSnapshot {
        val commands = catalogProvider.loadCurrentBundle()?.commands.orEmpty()
        return LiveCatalogSnapshot(
            commandIds = commands.map { it.id }.toSet(),
            titlesById = commands.associate { it.id to it.title_ru },
        )
    }
}
