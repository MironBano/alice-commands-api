package ru.appforsale.alicecommands.api.infrastructure.storage

import ru.appforsale.alicecommands.api.domain.ports.IconStorage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText

class FilesystemIconStorage(
    private val rootPath: Path,
    private val publicBaseUrl: String,
) : IconStorage {

    init {
        v1Path().createDirectories()
    }

    override fun store(slug: String, svg: String): String {
        val target = v1Path().resolve("$slug.svg")
        target.writeText(svg)
        return iconUrl(slug)
    }

    override fun iconUrl(slug: String): String =
        "${publicBaseUrl.trimEnd('/')}/icons/v1/$slug.svg"

    override fun basePublicUrl(): String = publicBaseUrl.trimEnd('/')

    override fun listSlugs(): List<String> =
        v1Path().listDirectoryEntries("*.svg")
            .filter { it.isRegularFile() }
            .map { it.fileName.toString().removeSuffix(".svg") }
            .sorted()

    override fun exists(slug: String): Boolean =
        v1Path().resolve("$slug.svg").exists()

    override fun read(slug: String): String? {
        val path = v1Path().resolve("$slug.svg")
        if (!path.isRegularFile()) return null
        return Files.readString(path)
    }

    private fun v1Path(): Path = rootPath.resolve("v1")
}
