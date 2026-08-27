package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.application.publish.CommandOfDayBundleBuilder
import ru.appforsale.alicecommands.api.application.publish.CommandOfDayPolicy
import ru.appforsale.alicecommands.api.application.publish.PublishCommandOfDayUseCase
import ru.appforsale.alicecommands.api.application.read.DraftPublishStatusService
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandOfDay
import ru.appforsale.alicecommands.api.domain.CommandOfDaySettings
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.CurrentManifest
import ru.appforsale.alicecommands.api.domain.DraftStats
import ru.appforsale.alicecommands.api.domain.PublishHistoryEntry
import ru.appforsale.alicecommands.api.domain.ValidationException
import ru.appforsale.alicecommands.api.domain.ports.SchemaValidator
import ru.appforsale.alicecommands.api.application.publish.CommandOfDayValidationUseCase

class CommandOfDayPublishUseCaseTest {

    private val commands = listOf(
        command("music_a", "music", 10),
        command("music_b", "music", 20),
        command("timers_a", "timers", 10),
    )

    @Test
    fun `policy ignores auto command_id snapshot`() {
        val settings = settings(mode = "auto", commandId = "ignored", autoCategoryId = "music")
        val published = CommandOfDay(
            mode = "auto",
            command_id = "music_b",
            auto_category_id = "music",
            auto_seed = 31,
            resolved_date = "2026-07-01",
            updated_at = "2026-07-01T00:00:00Z",
        )
        assertTrue(CommandOfDayPolicy.matches(settings, published))
    }

    @Test
    fun `command of day draft alone does not require catalog publish`() {
        val published = bundle(
            commands = commands,
            cod = CommandOfDay(
                mode = "manual",
                command_id = "music_a",
                resolved_date = "2026-07-08",
                updated_at = "2026-07-01T00:00:00Z",
            ),
        )
        val storage = FakeBundleStorageWithPublished(published, version = 10)
        val manifest = FakeManifestRepository(
            CurrentManifest(
                contentVersion = 10,
                bundlePath = "content_v10.json.gz",
                bundleSha256 = "sha",
                publishedAt = "2026-07-01T00:00:00Z",
                minAppVersion = "1.0",
                schemaVersion = 2,
                bundleSizeBytes = 100,
            ),
        )
        val draft = FakeDraftRepository(
            settings = settings(mode = "auto", commandId = "music_a", autoCategoryId = "music", updatedAt = "2026-07-08T12:00:00Z"),
            publishedCatalog = published,
        )
        val status = DraftPublishStatusService(draft, manifest, storage)

        assertFalse(status.hasUnpublishedChanges())
        assertTrue(status.hasUnpublishedCommandOfDayChanges())
    }

    @Test
    fun `publish command of day patches live bundle and bumps version`() {
        val published = bundle(
            commands = commands,
            cod = CommandOfDay(
                mode = "manual",
                command_id = "music_a",
                resolved_date = "2026-07-08",
                updated_at = "2026-07-01T00:00:00Z",
            ),
        )
        val storage = FakeBundleStorageWithPublished(published, version = 10)
        val manifest = FakeManifestRepository(
            CurrentManifest(
                contentVersion = 10,
                bundlePath = "content_v10.json.gz",
                bundleSha256 = "sha",
                publishedAt = "2026-07-01T00:00:00Z",
                minAppVersion = "1.0",
                schemaVersion = 2,
                bundleSizeBytes = 100,
            ),
        )
        val draft = FakeDraftRepository(
            settings = settings(mode = "auto", commandId = "music_a", autoCategoryId = "music", updatedAt = "2026-07-08T12:00:00Z"),
            publishedCatalog = published,
        )
        val status = DraftPublishStatusService(draft, manifest, storage)
        val useCase = PublishCommandOfDayUseCase(
            draft,
            manifest,
            storage,
            NoOpSchemaValidator,
            CommandOfDayValidationUseCase(),
            status,
            bundleRetentionCount = 5,
        )

        val result = useCase.execute("admin", notes = "cod test")

        assertEquals(11, result.contentVersion)
        val live = BundleCodec.json.decodeFromString<ContentBundle>(
            BundleCodec.gunzip(storage.writtenBundles["content_v11.json.gz"]!!),
        )
        assertEquals("auto", live.command_of_day?.mode)
        assertEquals("music", live.command_of_day?.auto_category_id)
        assertEquals(commands.size, live.commands.size)
        assertNotEquals(published.command_of_day?.mode, live.command_of_day?.mode)
    }

    @Test
    fun `publish command of day rejects when no changes`() {
        val cod = CommandOfDay(
            mode = "manual",
            command_id = "music_a",
            resolved_date = "2026-07-08",
            updated_at = "2026-07-08T12:00:00Z",
        )
        val published = bundle(commands = commands, cod = cod)
        val storage = FakeBundleStorageWithPublished(published, version = 3)
        val manifest = FakeManifestRepository(
            CurrentManifest(
                contentVersion = 3,
                bundlePath = "content_v3.json.gz",
                bundleSha256 = "sha",
                publishedAt = "2026-07-01T00:00:00Z",
                minAppVersion = "1.0",
                schemaVersion = 2,
                bundleSizeBytes = 100,
            ),
        )
        val draft = FakeDraftRepository(
            settings = settings(mode = "manual", commandId = "music_a", updatedAt = "2026-07-08T12:00:00Z"),
            publishedCatalog = published,
        )
        val status = DraftPublishStatusService(draft, manifest, storage)
        val useCase = PublishCommandOfDayUseCase(
            draft, manifest, storage, NoOpSchemaValidator, CommandOfDayValidationUseCase(), status, 5,
        )

        assertThrows(ValidationException::class.java) {
            useCase.execute("admin")
        }
    }

    private fun command(id: String, categoryId: String, sortOrder: Int) = Command(
        id = id,
        category_id = categoryId,
        title_ru = id,
        phrases = listOf("Алиса, $id"),
        effect_description_ru = "Effect",
        requires_alice_word = true,
        source_url = "https://example.com",
        updated_at = "2026-07-01T00:00:00Z",
        sort_order = sortOrder,
    )

    private fun bundle(commands: List<Command>, cod: CommandOfDay?) = ContentBundle(
        schema_version = 2,
        content_version = 10,
        published_at = "2026-07-01T00:00:00Z",
        min_app_version = "1.0",
        categories = listOf(
            Category(id = "music", title_ru = "Музыка", sort_order = 1, source_url = "https://example.com"),
            Category(id = "timers", title_ru = "Таймеры", sort_order = 2, source_url = "https://example.com"),
        ),
        commands = commands,
        command_of_day = cod,
    )

    private fun settings(
        mode: String,
        commandId: String,
        autoCategoryId: String? = null,
        updatedAt: String = "2026-07-08T12:00:00Z",
    ) = CommandOfDaySettings(
        mode = mode,
        command_id = commandId,
        auto_category_id = autoCategoryId,
        auto_seed = 31,
        updated_at = updatedAt,
        updated_by = "admin",
    )

    private class FakeDraftRepository(
        private val settings: CommandOfDaySettings?,
        private val publishedCatalog: ContentBundle,
    ) : AffiliatePublishUseCaseTest.FakeDraftRepository(
        stats = DraftStats(
            publishedCatalog.categories.size,
            publishedCatalog.command_groups.size,
            publishedCatalog.commands.size,
            publishedCatalog.scenario_templates.size,
            publishedCatalog.checklist_items.size,
            0,
            0,
            0,
        ),
    ) {
        override fun getCommandOfDaySettings(): CommandOfDaySettings? = settings

        override fun loadFull(contentVersion: Int, minAppVersion: String): ContentBundle {
            val cod = settings?.let { CommandOfDayBundleBuilder.build(it, publishedCatalog.commands) }
            return publishedCatalog.copy(
                content_version = contentVersion,
                min_app_version = minAppVersion,
                command_of_day = cod,
            )
        }
    }

    private class FakeManifestRepository(
        private var current: CurrentManifest?,
        private var next: Int = (current?.contentVersion ?: 0) + 1,
    ) : ru.appforsale.alicecommands.api.domain.ports.ManifestRepository {
        override fun getCurrent(): CurrentManifest? = current
        override fun nextVersion(): Int = next++
        override fun update(manifest: CurrentManifest) {
            current = manifest
        }
        override fun listHistory(limit: Int): List<PublishHistoryEntry> = emptyList()
        override fun insertHistory(entry: PublishHistoryEntry) = Unit
        override fun getHistoryByVersion(version: Int): PublishHistoryEntry? = null
    }

    private class FakeBundleStorageWithPublished(
        published: ContentBundle,
        version: Int,
    ) : ru.appforsale.alicecommands.api.domain.ports.BundleStorage {
        val writtenBundles: MutableMap<String, ByteArray> = mutableMapOf()
        private val initialPath = "content_v$version.json.gz"

        init {
            writtenBundles[initialPath] = BundleCodec.gzip(BundleCodec.toJson(published))
        }

        override fun write(filename: String, gzipBytes: ByteArray): String {
            writtenBundles[filename] = gzipBytes
            return filename
        }

        override fun read(filename: String): ByteArray? = writtenBundles[filename]
        override fun exists(filename: String): Boolean = filename in writtenBundles
        override fun isWritable(): Boolean = true
        override fun pruneOldBundles(retention: Int) = Unit
        override fun writeAffiliate(jsonBytes: ByteArray) = Unit
        override fun readAffiliate() = null
        override fun writeSmartHomeDevices(jsonBytes: ByteArray) = Unit
        override fun readSmartHomeDevices() = null
    }

    private object NoOpSchemaValidator : SchemaValidator {
        override fun validate(bundle: ContentBundle) = Unit
        override fun validateJson(json: String) = Unit
    }
}
