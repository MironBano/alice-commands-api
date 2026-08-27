package ru.appforsale.alicecommands.api

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.infrastructure.storage.FilesystemBundleStorage
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

class FilesystemBundleStorageTest {

    @Test
    fun `prune old bundles sorts content versions numerically`() {
        val root = Files.createTempDirectory("alice-storage-test")
        val bundles = root.resolve("bundles")
        val manifest = root.resolve("manifest")
        val storage = FilesystemBundleStorage(bundles, manifest, Json)

        (1..10).forEach { version ->
            storage.write("content_v$version.json.gz", byteArrayOf(version.toByte()))
            manifest.resolve("affiliate_v$version.json").writeBytes(byteArrayOf(version.toByte()))
        }

        storage.pruneOldBundles(retention = 5)

        (6..10).forEach { version ->
            assertTrue(bundles.resolve("content_v$version.json.gz").exists(), "content_v$version should remain")
            assertTrue(manifest.resolve("affiliate_v$version.json").exists(), "affiliate_v$version should remain")
        }
        (1..5).forEach { version ->
            assertFalse(bundles.resolve("content_v$version.json.gz").exists(), "content_v$version should be pruned")
            assertFalse(manifest.resolve("affiliate_v$version.json").exists(), "affiliate_v$version should be pruned")
        }
    }
}
