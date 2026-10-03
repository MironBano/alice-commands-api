package ru.appforsale.alicecommands.api.application.push

import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.domain.ContentBundle

/**
 * Reads published content bundle bytes (gzip or raw JSON) for campaign context.
 */
object PublishedBundleReader {
    private val GZIP_MAGIC_0: Byte = 0x1f.toByte()
    private val GZIP_MAGIC_1: Byte = 0x8b.toByte()

    fun decodeBundleJson(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return runCatching {
            if (bytes.size >= 2 && bytes[0] == GZIP_MAGIC_0 && bytes[1] == GZIP_MAGIC_1) {
                BundleCodec.gunzip(bytes)
            } else {
                bytes.decodeToString()
            }
        }.getOrNull()
    }

    fun extractCommandOfDayId(bytes: ByteArray): String? {
        val jsonText = decodeBundleJson(bytes) ?: return null
        return runCatching {
            BundleCodec.json.decodeFromString(ContentBundle.serializer(), jsonText)
                .command_of_day
                ?.command_id
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
