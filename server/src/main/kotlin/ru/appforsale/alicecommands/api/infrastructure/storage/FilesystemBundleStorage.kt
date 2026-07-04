package ru.appforsale.alicecommands.api.infrastructure.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.appforsale.alicecommands.api.domain.AffiliateBlocksResponse
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

class FilesystemBundleStorage(
    private val bundlePath: Path,
    private val manifestPath: Path,
    private val json: Json,
) : BundleStorage {

    init {
        bundlePath.createDirectories()
        manifestPath.createDirectories()
    }

    override fun write(filename: String, gzipBytes: ByteArray): String {
        val target = bundlePath.resolve(filename)
        target.writeBytes(gzipBytes)
        return filename
    }

    override fun read(filename: String): ByteArray? {
        val target = bundlePath.resolve(filename)
        return if (target.exists()) target.readBytes() else null
    }

    override fun exists(filename: String): Boolean = bundlePath.resolve(filename).exists()

    override fun isWritable(): Boolean {
        val probe = bundlePath.resolve(".write_probe")
        return try {
            probe.writeBytes(byteArrayOf())
            probe.toFile().delete()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun pruneOldBundles(retention: Int) {
        val bundles = bundlePath.listDirectoryEntries("content_v*.json.gz")
            .sortedByDescending { contentVersion(it.name) ?: 0 }
        bundles.drop(retention).forEach { file ->
            Files.deleteIfExists(file)
            val version = contentVersion(file.name)?.toString()
            if (version != null) {
                Files.deleteIfExists(manifestPath.resolve("affiliate_v$version.json"))
            }
        }
    }

    override fun writeAffiliate(jsonBytes: ByteArray) {
        manifestPath.resolve("affiliate_blocks.json").writeBytes(jsonBytes)
    }

    override fun readAffiliate(): AffiliateBlocksResponse? {
        val file = manifestPath.resolve("affiliate_blocks.json")
        if (!file.exists()) return null
        return json.decodeFromString(file.readBytes().decodeToString())
    }

    companion object {
        private val VERSION_IN_FILENAME = Regex("content_v(\\d+)\\.json\\.gz")

        private fun contentVersion(filename: String): Int? =
            VERSION_IN_FILENAME.find(filename)?.groupValues?.get(1)?.toIntOrNull()
    }
}
