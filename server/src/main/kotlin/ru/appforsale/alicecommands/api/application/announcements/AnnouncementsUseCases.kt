package ru.appforsale.alicecommands.api.application.announcements

import ru.appforsale.alicecommands.api.application.publish.SmartHomeDevicesValidationUseCase
import ru.appforsale.alicecommands.api.domain.Announcement
import ru.appforsale.alicecommands.api.domain.AnnouncementPublicItem
import ru.appforsale.alicecommands.api.domain.AnnouncementsPublicResponse
import ru.appforsale.alicecommands.api.domain.UploadDeviceImageRequest
import ru.appforsale.alicecommands.api.domain.UploadAnnouncementImageResponse
import ru.appforsale.alicecommands.api.domain.ValidationException
import ru.appforsale.alicecommands.api.domain.ports.AnnouncementImageStorage
import ru.appforsale.alicecommands.api.domain.ports.AnnouncementsRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64

class GetAnnouncementsPublicUseCase(
    private val repository: AnnouncementsRepository,
) {
    fun execute(): AnnouncementsPublicResponse {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val items = repository.listActive(now)
        val updatedAt = repository.maxActiveUpdatedAt(now)?.format(ISO_FORMAT)
            ?: now.format(ISO_FORMAT)
        return AnnouncementsPublicResponse(
            updated_at = updatedAt,
            items = items.map { it.toPublicItem() },
        )
    }

    fun etag(): String? {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val updatedAt = repository.maxActiveUpdatedAt(now) ?: return null
        return "\"announcements-${updatedAt.toInstant().toEpochMilli()}\""
    }
}

class AnnouncementsAdminUseCase(
    private val repository: AnnouncementsRepository,
    private val validationUseCase: AnnouncementsValidationUseCase,
) {
    fun list(): List<Announcement> = repository.listAll()

    fun get(id: String): Announcement? = repository.getById(id)

    fun create(announcement: Announcement): Announcement {
        validationUseCase.validate(announcement, isCreate = true).let { errors ->
            if (errors.isNotEmpty()) throw ValidationException(errors)
        }
        if (repository.getById(announcement.id) != null) {
            throw ValidationException(listOf("id: already exists"))
        }
        return repository.create(announcement.copy(revision = 1))
    }

    fun update(announcement: Announcement): Announcement {
        validationUseCase.validate(announcement, isCreate = false).let { errors ->
            if (errors.isNotEmpty()) throw ValidationException(errors)
        }
        if (repository.getById(announcement.id) == null) {
            throw ValidationException(listOf("id: not found"))
        }
        return repository.update(announcement)
    }

    fun delete(id: String): Boolean = repository.delete(id)
}

class UploadAnnouncementImageUseCase(
    private val imageStorage: AnnouncementImageStorage,
    private val validationUseCase: SmartHomeDevicesValidationUseCase,
) {
    fun execute(request: UploadDeviceImageRequest): UploadAnnouncementImageResponse {
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
        val imageUrl = imageStorage.store(slug, bytes, extension)
        validationUseCase.validateImageUrl("image_url", imageUrl).let { errors ->
            if (errors.isNotEmpty()) throw ValidationException(errors)
        }
        return UploadAnnouncementImageResponse(slug = slug, image_url = imageUrl)
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

class AnnouncementsValidationUseCase(
    private val smartHomeValidation: SmartHomeDevicesValidationUseCase,
) {
    fun validate(announcement: Announcement, isCreate: Boolean): List<String> {
        val errors = mutableListOf<String>()
        val id = announcement.id.trim()
        if (id.isBlank()) {
            errors += "id: required"
        } else if (!ID_REGEX.matches(id)) {
            errors += "id: invalid format"
        }
        if (announcement.title.trim().isBlank()) {
            errors += "title: required"
        }
        if (announcement.placement != "more") {
            errors += "placement: only 'more' supported"
        }
        if (!HEX_COLOR.matches(announcement.background_color.trim())) {
            errors += "background_color: invalid hex"
        }
        announcement.foreground_color?.trim()?.takeIf { it.isNotEmpty() }?.let { color ->
            if (!HEX_COLOR.matches(color)) {
                errors += "foreground_color: invalid hex"
            }
        }
        val hasCtaTarget = !announcement.cta_target.isNullOrBlank()
        val hasCtaAction = !announcement.cta_action.isNullOrBlank()
        if (hasCtaTarget xor hasCtaAction) {
            errors += "cta_action and cta_target must both be set or both empty"
        }
        if (hasCtaAction) {
            when (announcement.cta_action?.trim()?.lowercase()) {
                "url" -> errors += validateHttpsUrl("cta_target", announcement.cta_target?.trim().orEmpty())
                "route" -> errors += validateRoute("cta_target", announcement.cta_target?.trim().orEmpty())
                "deeplink" -> errors += validateDeepLink("cta_target", announcement.cta_target?.trim().orEmpty())
                else -> errors += "cta_action: must be url, route, or deeplink"
            }
        }
        announcement.image_url?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
            errors += smartHomeValidation.validateImageUrl("image_url", url)
        }
        val parsedStartsAt = announcement.starts_at?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            runCatching { OffsetDateTime.parse(raw) }.getOrElse {
                errors += "starts_at: invalid ISO-8601"
                null
            }
        }
        val parsedEndsAt = announcement.ends_at?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            runCatching { OffsetDateTime.parse(raw) }.getOrElse {
                errors += "ends_at: invalid ISO-8601"
                null
            }
        }
        if (parsedStartsAt != null && parsedEndsAt != null && parsedStartsAt.isAfter(parsedEndsAt)) {
            errors += "starts_at: must be before or equal to ends_at"
        }
        if (!isCreate && id.isBlank()) {
            errors += "id: required for update"
        }
        return errors
    }

    companion object {
        private val ID_REGEX = Regex("^[a-z][a-z0-9_]*$")
        private val HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}$")
        private val ALLOWED_ROUTES = setOf(
            "home/catalog",
            "home/quick",
            "home/smarthome",
            "home/favorites",
            "home/more",
            "more/checklist",
            "more/history",
            "more/faq",
            "paywall",
            "more/disclaimer",
            "more/privacy",
            "more/support",
            "more/repeat-persona",
            "more/command_of_day",
            "search",
        )

        private fun validateHttpsUrl(field: String, value: String): List<String> {
            if (!value.startsWith("https://", ignoreCase = true)) {
                return listOf("$field: must start with https://")
            }
            return runCatching { java.net.URI(value) }.fold(
                onSuccess = { if (it.host.isNullOrBlank()) listOf("$field: invalid URL") else emptyList() },
                onFailure = { listOf("$field: invalid URL") },
            )
        }

        private fun validateRoute(field: String, value: String): List<String> =
            if (value in ALLOWED_ROUTES) emptyList() else listOf("$field: unknown route")

        private val SUPPORTED_COMMAND_DEEPLINK =
            Regex("^alicecommands://command/[a-zA-Z0-9_-]+$", RegexOption.IGNORE_CASE)

        private fun validateDeepLink(field: String, value: String): List<String> =
            if (SUPPORTED_COMMAND_DEEPLINK.matches(value)) emptyList()
            else listOf("$field: must be alicecommands://command/{id}")
    }
}

private fun Announcement.toPublicItem(): AnnouncementPublicItem =
    AnnouncementPublicItem(
        id = id,
        revision = revision,
        placement = placement,
        title = title,
        body = body,
        image_url = image_url,
        background_color = background_color,
        foreground_color = foreground_color,
        cta_label = cta_label,
        cta_action = cta_action,
        cta_target = cta_target,
        dismissible = dismissible,
        priority = priority,
        min_app_version = min_app_version,
        max_app_version = max_app_version,
        starts_at = starts_at,
        ends_at = ends_at,
    )

private val ISO_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
