package ru.appforsale.alicecommands.api.application.read

import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.application.publish.CommandOfDayPolicy
import ru.appforsale.alicecommands.api.config.AppConfig
import ru.appforsale.alicecommands.api.domain.AffiliateBlocksResponse
import ru.appforsale.alicecommands.api.domain.SmartHomeDevicesResponse
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ManifestResponse
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.HealthProbe
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository

class ManifestService(
    private val manifestRepository: ManifestRepository,
    private val config: AppConfig,
) {
    data class ManifestWithEtag(val manifest: ManifestResponse, val etag: String)

    fun getManifestWithEtag(): ManifestWithEtag? {
        val current = manifestRepository.getCurrent() ?: return null
        val manifest = ManifestResponse(
            schema_version = current.schemaVersion,
            content_version = current.contentVersion,
            published_at = current.publishedAt,
            min_app_version = current.minAppVersion,
            bundle_url = "${config.publicBaseUrl}/v1/content/bundle",
            bundle_sha256 = current.bundleSha256,
            backup_url = "${config.publicBaseUrl}/v1/content/bundle-backup/${current.bundlePath}",
            bundle_size_bytes = current.bundleSizeBytes,
        )
        return ManifestWithEtag(manifest, "\"content-${current.contentVersion}\"")
    }
}

class BundleService(
    private val manifestRepository: ManifestRepository,
    private val bundleStorage: BundleStorage,
) {
    data class BundleData(val bytes: ByteArray, val etag: String, val sha256: String)

    fun getPublishedBundle(): BundleData? {
        val current = manifestRepository.getCurrent() ?: return null
        val bytes = bundleStorage.read(current.bundlePath) ?: return null
        return BundleData(bytes, "\"content-${current.contentVersion}\"", current.bundleSha256)
    }

    fun getBackupBundle(filename: String): ByteArray? {
        if (!filename.matches(BACKUP_FILENAME)) return null
        return bundleStorage.read(filename)
    }

    companion object {
        private val BACKUP_FILENAME = Regex("content_v\\d+\\.json\\.gz")
    }
}

class AffiliateService(private val bundleStorage: BundleStorage) {
    fun getPublishedBlocks(): AffiliateBlocksResponse? = bundleStorage.readAffiliate()
}

class SmartHomeDevicesService(private val bundleStorage: BundleStorage) {
    fun getPublishedDevices(): SmartHomeDevicesResponse? = bundleStorage.readSmartHomeDevices()
}

class HealthService(
    private val healthProbe: HealthProbe,
    private val bundleStorage: BundleStorage,
) {
    data class ReadyStatus(
        val status: String,
        val database: String,
        val storage: String,
        val httpStatus: Int,
    )

    fun ready(): ReadyStatus {
        val dbOk = healthProbe.isDatabaseOk()
        val storageOk = bundleStorage.isWritable()
        val ok = dbOk && storageOk
        return ReadyStatus(
            status = if (ok) "ready" else "not_ready",
            database = if (dbOk) "ok" else "error",
            storage = if (storageOk) "ok" else "error",
            httpStatus = if (ok) 200 else 503,
        )
    }
}

class DraftPublishStatusService(
    private val draftRepository: DraftRepository,
    private val manifestRepository: ManifestRepository,
    private val bundleStorage: BundleStorage,
) {
    fun hasUnpublishedChanges(): Boolean {
        val current = manifestRepository.getCurrent()
        if (current == null) {
            val stats = draftRepository.stats()
            return stats.categoriesCount > 0 || stats.commandsCount > 0
        }
        return isCatalogChanged(current)
    }

    private fun isCatalogChanged(current: ru.appforsale.alicecommands.api.domain.CurrentManifest): Boolean {
        val publishedBytes = bundleStorage.read(current.bundlePath) ?: return true
        val publishedBundle = BundleCodec.json.decodeFromString<ContentBundle>(BundleCodec.gunzip(publishedBytes))
        val draftBundle = draftRepository.loadFull(minAppVersion = current.minAppVersion)
        return BundleCodec.contentFingerprint(draftBundle) != BundleCodec.contentFingerprint(publishedBundle)
    }

    fun hasUnpublishedCommandOfDayChanges(): Boolean {
        val settings = draftRepository.getCommandOfDaySettings() ?: return false
        val current = manifestRepository.getCurrent() ?: return true
        val publishedBytes = bundleStorage.read(current.bundlePath) ?: return true
        val publishedBundle = BundleCodec.json.decodeFromString<ContentBundle>(BundleCodec.gunzip(publishedBytes))
        return !CommandOfDayPolicy.matches(settings, publishedBundle.command_of_day)
    }

}
