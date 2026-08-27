package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.UploadDeviceImageRequest
import ru.appforsale.alicecommands.api.domain.UploadDeviceImageResponse
import ru.appforsale.alicecommands.api.domain.ValidationException
import ru.appforsale.alicecommands.api.domain.ports.DeviceImageStorage
import java.util.Base64

class UploadDeviceImageUseCase(
    private val deviceImageStorage: DeviceImageStorage,
    private val validationUseCase: SmartHomeDevicesValidationUseCase,
) {
    fun execute(request: UploadDeviceImageRequest): UploadDeviceImageResponse {
        val slug = request.slug.trim().lowercase()
        if (!SLUG_REGEX.matches(slug)) {
            throw ValidationException(listOf("slug: invalid format"))
        }
        val (bytes, extension) = decodeImage(request.image_base64, request.content_type)
        if (bytes.isEmpty()) {
            throw ValidationException(listOf("image_base64: empty payload"))
        }
        if (bytes.size > MAX_BYTES) {
            throw ValidationException(listOf("image_base64: exceeds 2 MB limit"))
        }
        val imageUrl = deviceImageStorage.store(slug, bytes, extension)
        validationUseCase.validateImageUrl("image_url", imageUrl).let { errors ->
            if (errors.isNotEmpty()) throw ValidationException(errors)
        }
        return UploadDeviceImageResponse(slug = slug, image_url = imageUrl)
    }

    private fun decodeImage(raw: String, contentType: String?): Pair<ByteArray, String> {
        val trimmed = raw.trim()
        val dataUrlMatch = DATA_URL_REGEX.matchEntire(trimmed)
        if (dataUrlMatch != null) {
            val mime = dataUrlMatch.groupValues[1]
            val payload = dataUrlMatch.groupValues[2]
            val ext = mimeToExtension(mime)
            return Base64.getDecoder().decode(payload) to ext
        }
        val ext = contentType?.let { mimeToExtension(it) }
            ?: throw ValidationException(listOf("content_type required when image_base64 is not a data URL"))
        return Base64.getDecoder().decode(trimmed) to ext
    }

    private fun mimeToExtension(mime: String): String = when (mime.lowercase().substringBefore(';').trim()) {
        "image/webp" -> "webp"
        "image/png" -> "png"
        "image/jpeg", "image/jpg" -> "jpg"
        else -> throw ValidationException(listOf("unsupported content_type: $mime"))
    }

    companion object {
        private val SLUG_REGEX = Regex("^[a-z][a-z0-9_]*$")
        private val DATA_URL_REGEX = Regex("^data:([^;]+);base64,(.+)$")
        private const val MAX_BYTES = 2 * 1024 * 1024
    }
}
