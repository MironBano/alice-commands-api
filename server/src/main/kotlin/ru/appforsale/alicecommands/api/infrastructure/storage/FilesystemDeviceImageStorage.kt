package ru.appforsale.alicecommands.api.infrastructure.storage

import ru.appforsale.alicecommands.api.domain.ports.DeviceImageStorage
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

class FilesystemDeviceImageStorage(
    private val rootPath: Path,
    private val publicBaseUrl: String,
) : DeviceImageStorage {

    init {
        v1Path().createDirectories()
    }

    override fun store(slug: String, bytes: ByteArray, extension: String): String {
        val ext = normalizeExtension(extension)
        val target = v1Path().resolve("$slug.$ext")
        target.writeBytes(bytes)
        return imageUrl(slug, ext)
    }

    override fun imageUrl(slug: String, extension: String): String =
        "${publicBaseUrl.trimEnd('/')}/devices/v1/$slug.${normalizeExtension(extension)}"

    override fun basePublicUrl(): String = publicBaseUrl.trimEnd('/')

    override fun exists(slug: String, extension: String): Boolean =
        v1Path().resolve("$slug.${normalizeExtension(extension)}").exists()

    private fun v1Path(): Path = rootPath.resolve("v1")

    private fun normalizeExtension(extension: String): String =
        extension.trim().lowercase().removePrefix(".").let {
            when (it) {
                "jpeg" -> "jpg"
                in ALLOWED_EXTENSIONS -> it
                else -> error("Unsupported image extension: $extension")
            }
        }

    companion object {
        val ALLOWED_EXTENSIONS = setOf("webp", "png", "jpg")
    }
}
