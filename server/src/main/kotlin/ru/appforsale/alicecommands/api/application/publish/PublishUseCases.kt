package ru.appforsale.alicecommands.api.application.publish

import kotlinx.serialization.encodeToString
import ru.appforsale.alicecommands.api.domain.AffiliateBlocksResponse
import ru.appforsale.alicecommands.api.domain.DevicePick
import ru.appforsale.alicecommands.api.domain.SmartHomeDevicesResponse
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.CurrentManifest
import ru.appforsale.alicecommands.api.domain.PublishHistoryEntry
import ru.appforsale.alicecommands.api.domain.PublishResult
import ru.appforsale.alicecommands.api.domain.ValidationException
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository
import ru.appforsale.alicecommands.api.domain.ports.SchemaValidator
import ru.appforsale.alicecommands.api.application.BundleCodec
import java.time.Instant

class PublishContentUseCase(
    private val draftRepository: DraftRepository,
    private val manifestRepository: ManifestRepository,
    private val bundleStorage: BundleStorage,
    private val schemaValidator: SchemaValidator,
    private val commandGroupValidationUseCase: CommandGroupValidationUseCase,
    private val categoryVisualValidationUseCase: CategoryVisualValidationUseCase,
    private val commandOfDayValidationUseCase: CommandOfDayValidationUseCase,
    private val bundleRetentionCount: Int,
) {
    fun execute(adminUser: String, minAppVersion: String = "1.0", notes: String? = null): PublishResult {
        val version = manifestRepository.nextVersion()
        val publishedAt = Instant.now().toString()
        val draft = draftRepository.loadFull(contentVersion = version, minAppVersion = minAppVersion)
            .copy(published_at = publishedAt)

        commandGroupValidationUseCase.validateForPublish(draft)
        categoryVisualValidationUseCase.validateForPublish(draft)
        commandOfDayValidationUseCase.validateForPublish(draft)
        schemaValidator.validate(draft)

        val json = BundleCodec.toJson(draft)
        val gzip = BundleCodec.gzip(json)
        require(gzip.size <= 2 * 1024 * 1024) { "Bundle exceeds 2 MB gzip limit" }

        val sha = BundleCodec.sha256(gzip)
        val filename = "content_v$version.json.gz"
        bundleStorage.write(filename, gzip)

        val manifest = CurrentManifest(
            contentVersion = version,
            bundlePath = filename,
            bundleSha256 = sha,
            publishedAt = publishedAt,
            minAppVersion = minAppVersion,
            schemaVersion = draft.schema_version,
            bundleSizeBytes = gzip.size.toLong(),
        )
        manifestRepository.update(manifest)
        manifestRepository.insertHistory(
            PublishHistoryEntry(
                id = 0,
                contentVersion = version,
                bundleSha256 = sha,
                adminUsername = adminUser,
                publishedAt = publishedAt,
                notes = notes,
            ),
        )
        bundleStorage.pruneOldBundles(bundleRetentionCount)

        return PublishResult(contentVersion = version, bundleSha256 = sha, publishedAt = publishedAt)
    }
}

class PublishCommandOfDayUseCase(
    private val draftRepository: DraftRepository,
    private val manifestRepository: ManifestRepository,
    private val bundleStorage: BundleStorage,
    private val schemaValidator: SchemaValidator,
    private val commandOfDayValidationUseCase: CommandOfDayValidationUseCase,
    private val draftPublishStatusService: ru.appforsale.alicecommands.api.application.read.DraftPublishStatusService,
    private val bundleRetentionCount: Int,
) {
    fun execute(adminUser: String, notes: String? = null): PublishResult {
        if (!draftPublishStatusService.hasUnpublishedCommandOfDayChanges()) {
            throw ValidationException(listOf("command_of_day: no unpublished changes"))
        }
        val current = manifestRepository.getCurrent()
            ?: throw ValidationException(listOf("No published bundle yet — publish catalog first"))
        val publishedBytes = bundleStorage.read(current.bundlePath)
            ?: throw IllegalStateException("Published bundle file missing: ${current.bundlePath}")
        val published = BundleCodec.json.decodeFromString<ContentBundle>(BundleCodec.gunzip(publishedBytes))
        val settings = draftRepository.getCommandOfDaySettings()
            ?: throw ValidationException(listOf("command_of_day settings not configured"))

        val commandOfDay = CommandOfDayBundleBuilder.build(settings, published.commands)
        val version = manifestRepository.nextVersion()
        val publishedAt = Instant.now().toString()
        val patched = published.copy(
            content_version = version,
            published_at = publishedAt,
            min_app_version = current.minAppVersion,
            command_of_day = commandOfDay,
        )

        commandOfDayValidationUseCase.validateForPublish(patched)
        schemaValidator.validate(patched)

        val json = BundleCodec.toJson(patched)
        val gzip = BundleCodec.gzip(json)
        require(gzip.size <= 2 * 1024 * 1024) { "Bundle exceeds 2 MB gzip limit" }

        val sha = BundleCodec.sha256(gzip)
        val filename = "content_v$version.json.gz"
        bundleStorage.write(filename, gzip)

        val manifest = CurrentManifest(
            contentVersion = version,
            bundlePath = filename,
            bundleSha256 = sha,
            publishedAt = publishedAt,
            minAppVersion = current.minAppVersion,
            schemaVersion = patched.schema_version,
            bundleSizeBytes = gzip.size.toLong(),
        )
        manifestRepository.update(manifest)
        manifestRepository.insertHistory(
            PublishHistoryEntry(
                id = 0,
                contentVersion = version,
                bundleSha256 = sha,
                adminUsername = adminUser,
                publishedAt = publishedAt,
                notes = notes ?: "command_of_day publish",
            ),
        )
        bundleStorage.pruneOldBundles(bundleRetentionCount)

        return PublishResult(contentVersion = version, bundleSha256 = sha, publishedAt = publishedAt)
    }
}

class PublishAffiliateUseCase(
    private val draftRepository: DraftRepository,
    private val bundleStorage: BundleStorage,
) {
    fun execute(updatedAt: String = Instant.now().toString()): AffiliateBlocksResponse {
        val response = AffiliateBlocksResponse(updated_at = updatedAt, blocks = draftRepository.listAffiliateBlocks())
        val bytes = BundleCodec.json.encodeToString(response).toByteArray(Charsets.UTF_8)
        bundleStorage.writeAffiliate(bytes)
        return response
    }
}

class PublishSmartHomeDevicesUseCase(
    private val draftRepository: DraftRepository,
    private val bundleStorage: BundleStorage,
    private val validationUseCase: SmartHomeDevicesValidationUseCase,
    private val schemaValidator: ru.appforsale.alicecommands.api.domain.ports.SmartHomeDevicesSchemaValidator,
) {
    fun execute(updatedAt: String = Instant.now().toString()): SmartHomeDevicesResponse {
        val picks = draftRepository.listDevicePicks()
            .sortedWith(compareByDescending<DevicePick> { it.priority }.thenBy { it.sort_order })
        val guides = enrichGuidesWithDetailReferralPickIds(
            guides = draftRepository.listDeviceGuides().sortedBy { it.sort_order },
            picks = picks,
        )
        val response = SmartHomeDevicesResponse(
            updated_at = updatedAt,
            guides = guides,
            picks = picks,
        )
        validationUseCase.validateForPublish(response)
        schemaValidator.validate(response)
        val bytes = BundleCodec.json.encodeToString(response).toByteArray(Charsets.UTF_8)
        bundleStorage.writeSmartHomeDevices(bytes)
        return response
    }
}

class RollbackPublishUseCase(
    private val manifestRepository: ManifestRepository,
    private val bundleStorage: BundleStorage,
) {
    fun execute(contentVersion: Int, adminUser: String): PublishResult {
        val history = manifestRepository.getHistoryByVersion(contentVersion)
            ?: throw IllegalArgumentException("Version $contentVersion not found in history")

        if (!bundleStorage.exists("content_v$contentVersion.json.gz")) {
            throw IllegalArgumentException("Bundle file for version $contentVersion not found")
        }

        val current = manifestRepository.getCurrent()
        val manifest = CurrentManifest(
            contentVersion = contentVersion,
            bundlePath = "content_v$contentVersion.json.gz",
            bundleSha256 = history.bundleSha256,
            publishedAt = history.publishedAt,
            minAppVersion = current?.minAppVersion ?: "1.0",
            schemaVersion = current?.schemaVersion ?: 1,
            bundleSizeBytes = bundleStorage.read("content_v$contentVersion.json.gz")?.size?.toLong() ?: 0,
        )
        manifestRepository.update(manifest)
        manifestRepository.insertHistory(
            history.copy(
                adminUsername = adminUser,
                publishedAt = Instant.now().toString(),
                notes = "rollback to v$contentVersion",
            ),
        )
        return PublishResult(
            contentVersion = contentVersion,
            bundleSha256 = history.bundleSha256,
            publishedAt = manifest.publishedAt,
        )
    }
}

class PreviewBundleUseCase(private val draftRepository: DraftRepository) {
    fun execute(minAppVersion: String = "1.0"): ContentBundle {
        val current = draftRepository.loadFull(minAppVersion = minAppVersion)
        return current.copy(content_version = 0)
    }
}
