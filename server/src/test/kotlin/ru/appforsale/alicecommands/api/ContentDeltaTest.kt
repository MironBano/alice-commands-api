package ru.appforsale.alicecommands.api.application.read

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.CommandOfDay
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.CurrentManifest
import ru.appforsale.alicecommands.api.domain.PublishHistoryEntry
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository

class ContentDeltaTest {

    @Test
    fun `delta includes command group changes`() {
        val storage = InMemoryBundleStorage()
        val v1 = bundle(
            groups = emptyList(),
            commands = listOf(cmd("c1")),
        )
        val v2 = bundle(
            groups = listOf(
                CommandGroup(
                    id = "smart_home_light",
                    category_id = "smart_home",
                    title_ru = "Свет",
                    sort_order = 10,
                ),
            ),
            commands = listOf(cmd("c1", groupId = "smart_home_light", sortOrder = 10)),
        )
        storage.write("content_v1.json.gz", BundleCodec.gzip(BundleCodec.toJson(v1)))
        storage.write("content_v2.json.gz", BundleCodec.gzip(BundleCodec.toJson(v2)))

        val manifestRepo = FakeManifestRepo(
            CurrentManifest(
                contentVersion = 2,
                bundlePath = "content_v2.json.gz",
                bundleSha256 = "sha",
                publishedAt = "2026-06-29T01:00:00Z",
                minAppVersion = "1.0",
                schemaVersion = 2,
                bundleSizeBytes = 100,
            ),
        )
        val service = ContentDeltaService(manifestRepo, storage)
        val delta = service.getDelta(1)

        assertEquals(1, delta.from_version)
        assertEquals(2, delta.to_version)
        assertEquals(1, delta.command_groups.added.size)
        assertEquals("smart_home_light", delta.command_groups.added.single().id)
        assertEquals(1, delta.commands.updated.size)
        assertEquals("smart_home_light", delta.commands.updated.single().group_id)
    }

    @Test
    fun `delta includes command_of_day change`() {
        val storage = InMemoryBundleStorage()
        val v1 = bundle(
            groups = emptyList(),
            commands = listOf(cmd("c1")),
            commandOfDay = null,
        )
        val cod = CommandOfDay(
            mode = "manual",
            command_id = "c1",
            resolved_date = "2026-07-01",
            updated_at = "2026-07-01T08:00:00Z",
        )
        val v2 = bundle(
            groups = emptyList(),
            commands = listOf(cmd("c1")),
            commandOfDay = cod,
        )
        storage.write("content_v1.json.gz", BundleCodec.gzip(BundleCodec.toJson(v1)))
        storage.write("content_v2.json.gz", BundleCodec.gzip(BundleCodec.toJson(v2)))

        val manifestRepo = FakeManifestRepo(
            CurrentManifest(
                contentVersion = 2,
                bundlePath = "content_v2.json.gz",
                bundleSha256 = "sha",
                publishedAt = "2026-06-29T01:00:00Z",
                minAppVersion = "1.0",
                schemaVersion = 2,
                bundleSizeBytes = 100,
            ),
        )
        val service = ContentDeltaService(manifestRepo, storage)
        val delta = service.getDelta(1)

        assertEquals(cod, delta.command_of_day)
    }

    @Test
    fun `delta unavailable when from bundle missing`() {
        val storage = InMemoryBundleStorage()
        val manifestRepo = FakeManifestRepo(
            CurrentManifest(
                contentVersion = 2,
                bundlePath = "content_v2.json.gz",
                bundleSha256 = "sha",
                publishedAt = "2026-06-29T01:00:00Z",
                minAppVersion = "1.0",
                schemaVersion = 2,
                bundleSizeBytes = 100,
            ),
        )
        val service = ContentDeltaService(manifestRepo, storage)
        assertTrue(
            assertThrows(DeltaUnavailableException::class.java) {
                service.getDelta(1)
            }.message!!.contains("not available"),
        )
    }

    private fun bundle(
        groups: List<CommandGroup>,
        commands: List<Command>,
        commandOfDay: CommandOfDay? = null,
    ) = ContentBundle(
        schema_version = 2,
        content_version = 1,
        published_at = "2026-06-29T00:00:00Z",
        categories = listOf(
            Category(id = "smart_home", title_ru = "Умный дом", sort_order = 1, source_url = "https://example.com"),
        ),
        command_groups = groups,
        commands = commands,
        command_of_day = commandOfDay,
    )

    private fun cmd(id: String, groupId: String? = null, sortOrder: Int? = null) = Command(
        id = id,
        category_id = "smart_home",
        title_ru = "Cmd $id",
        phrases = listOf("Алиса, $id"),
        effect_description_ru = "Effect",
        requires_alice_word = true,
        source_url = "https://example.com",
        updated_at = "2026-06-29T00:00:00Z",
        group_id = groupId,
        sort_order = sortOrder,
    )

    private class InMemoryBundleStorage : BundleStorage {
        private val files = mutableMapOf<String, ByteArray>()

        override fun write(filename: String, gzipBytes: ByteArray): String {
            files[filename] = gzipBytes
            return filename
        }

        override fun read(filename: String): ByteArray? = files[filename]
        override fun exists(filename: String): Boolean = filename in files
        override fun isWritable(): Boolean = true
        override fun pruneOldBundles(retention: Int) = Unit
        override fun writeAffiliate(jsonBytes: ByteArray) = Unit
        override fun readAffiliate() = null
    }

    private class FakeManifestRepo(private val current: CurrentManifest) : ManifestRepository {
        override fun getCurrent(): CurrentManifest = current
        override fun nextVersion(): Int = current.contentVersion + 1
        override fun update(manifest: CurrentManifest) = Unit
        override fun listHistory(limit: Int): List<PublishHistoryEntry> = emptyList()
        override fun insertHistory(entry: PublishHistoryEntry) = Unit
        override fun getHistoryByVersion(version: Int): PublishHistoryEntry? = null
    }
}
