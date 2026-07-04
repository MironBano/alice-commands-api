package ru.appforsale.alicecommands.api.application

import kotlinx.serialization.json.Json
import ru.appforsale.alicecommands.api.domain.ContentBundle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object BundleCodec {
    val json: Json = Json {
        prettyPrint = false
        encodeDefaults = true
        explicitNulls = false
    }

    fun toJson(bundle: ContentBundle): String = json.encodeToString(bundle)

    fun gzip(text: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }

    fun gunzip(bytes: ByteArray): String {
        GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
            return input.readBytes().decodeToString()
        }
    }

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    /** Fingerprint of catalog content ignoring publish metadata. */
    fun contentFingerprint(bundle: ContentBundle): String {
        val normalized = bundle.copy(content_version = 0, published_at = "")
        return sha256(toJson(normalized).toByteArray(Charsets.UTF_8))
    }
}
