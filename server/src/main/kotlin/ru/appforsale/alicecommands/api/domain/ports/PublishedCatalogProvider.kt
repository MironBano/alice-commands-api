package ru.appforsale.alicecommands.api.domain.ports

import ru.appforsale.alicecommands.api.domain.ContentBundle

interface PublishedCatalogProvider {
    fun loadCurrentBundle(): ContentBundle?
}
