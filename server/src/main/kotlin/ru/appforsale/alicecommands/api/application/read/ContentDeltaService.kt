package ru.appforsale.alicecommands.api.application.read

import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ContentDeltaResponse
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository

class DeltaUnavailableException(message: String) : Exception(message)

class ContentDeltaService(
    private val manifestRepository: ManifestRepository,
    private val bundleStorage: BundleStorage,
) {
    fun getDelta(fromVersion: Int): ContentDeltaResponse {
        val current = manifestRepository.getCurrent()
            ?: throw IllegalStateException("No published content")
        if (fromVersion == current.contentVersion) {
            return ContentDeltaResponse(
                from_version = fromVersion,
                to_version = current.contentVersion,
                schema_version = current.schemaVersion,
                published_at = current.publishedAt,
                full_bundle_required = false,
            )
        }
        if (fromVersion > current.contentVersion) {
            throw IllegalArgumentException("from version $fromVersion is newer than current ${current.contentVersion}")
        }

        val fromBundle = loadPublishedBundle(fromVersion)
            ?: throw DeltaUnavailableException(
                "Bundle for version $fromVersion not available; use GET /v1/content/bundle",
            )
        val toBundle = loadPublishedBundle(current.contentVersion)
            ?: throw IllegalStateException("Current bundle missing")

        return ContentDeltaResponse(
            from_version = fromVersion,
            to_version = current.contentVersion,
            schema_version = current.schemaVersion,
            published_at = current.publishedAt,
            full_bundle_required = false,
            categories = ContentBundleDiffer.diffCategories(fromBundle.categories, toBundle.categories),
            command_groups = ContentBundleDiffer.diffCommandGroups(fromBundle.command_groups, toBundle.command_groups),
            commands = ContentBundleDiffer.diffCommands(fromBundle.commands, toBundle.commands),
            scenario_templates = ContentBundleDiffer.diffScenarios(fromBundle.scenario_templates, toBundle.scenario_templates),
            checklist_items = ContentBundleDiffer.diffChecklist(fromBundle.checklist_items, toBundle.checklist_items),
            command_of_day = ContentBundleDiffer.diffCommandOfDay(fromBundle.command_of_day, toBundle.command_of_day),
        )
    }

    private fun loadPublishedBundle(version: Int): ContentBundle? {
        val filename = "content_v$version.json.gz"
        if (!bundleStorage.exists(filename)) return null
        val bytes = bundleStorage.read(filename) ?: return null
        return BundleCodec.json.decodeFromString<ContentBundle>(BundleCodec.gunzip(bytes))
    }
}
