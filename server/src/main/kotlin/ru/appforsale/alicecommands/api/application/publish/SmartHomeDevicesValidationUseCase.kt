package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.DeviceGuide
import ru.appforsale.alicecommands.api.domain.DevicePick
import ru.appforsale.alicecommands.api.domain.SmartHomeDevicesResponse
import ru.appforsale.alicecommands.api.domain.ValidationException
import java.net.URI

class SmartHomeDevicesValidationUseCase(
    private val iconUrlAllowedHosts: Set<String>,
) {
    private val idRegex = Regex("^[a-z][a-z0-9_]*$")
    private val allowedDeviceFilters = setOf("station", "tv", "phone")
    private val allowedPlacements = setOf(
        "smart_home_devices",
        "device_guide_detail",
        "scenario_detail",
        "command_detail",
        "catalog_persona",
        "search_empty",
        "checklist_complete",
    )

    fun validateForPublish(response: SmartHomeDevicesResponse) {
        val errors = mutableListOf<String>()
        val guideIds = mutableSetOf<String>()
        val pickIds = mutableSetOf<String>()

        response.guides.forEach { guide ->
            errors += validateGuide(guide, guideIds)
        }
        response.picks.forEach { pick ->
            errors += validatePick(pick, pickIds)
        }
        response.guides.forEach { guide ->
            guide.related_device_ids.forEach { relatedId ->
                if (relatedId !in guideIds) {
                    errors += "guides.${guide.id}.related_device_ids: unknown guide '$relatedId'"
                }
            }
            guide.detail_referral_pick_ids.forEach { pickId ->
                if (pickId !in pickIds) {
                    errors += "guides.${guide.id}.detail_referral_pick_ids: unknown pick '$pickId'"
                }
            }
        }
        response.picks.forEach { pick ->
            pick.guide_ids.forEach { guideId ->
                if (guideId !in guideIds) {
                    errors += "picks.${pick.id}.guide_ids: unknown guide '$guideId'"
                }
            }
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }

    fun validateGuide(guide: DeviceGuide, knownIds: MutableSet<String>? = null): List<String> {
        val prefix = "guides.${guide.id.ifBlank { "?" }}"
        val errors = mutableListOf<String>()
        if (guide.id.isBlank()) return listOf("$prefix: id required")
        if (!idRegex.matches(guide.id)) errors += "$prefix.id: invalid id format"
        if (guide.id in (knownIds ?: emptySet())) errors += "$prefix.id: duplicate id"
        knownIds?.add(guide.id)

        if (guide.title_ru.isBlank()) errors += "$prefix.title_ru required"
        if (guide.summary_ru.isBlank()) errors += "$prefix.summary_ru required"
        if (guide.capabilities_ru.isBlank()) errors += "$prefix.capabilities_ru required"
        if (guide.setup_ru.isBlank()) errors += "$prefix.setup_ru required"
        errors += validateActionUrl("$prefix.action_url", guide.action_url)
        if (!guide.image_url.isNullOrBlank()) {
            errors += validateHttpsImageUrl("$prefix.image_url", guide.image_url)
        }
        if (!guide.command_device_filter_id.isNullOrBlank() &&
            guide.command_device_filter_id !in allowedDeviceFilters
        ) {
            errors += "$prefix.command_device_filter_id: must be station, tv, or phone"
        }
        guide.setup_steps_ru.forEachIndexed { index, step ->
            if (step.isBlank()) errors += "$prefix.setup_steps_ru[$index]: must not be blank"
        }
        return errors
    }

    fun validatePick(pick: DevicePick, knownIds: MutableSet<String>? = null): List<String> {
        val prefix = "picks.${pick.id.ifBlank { "?" }}"
        val errors = mutableListOf<String>()
        if (pick.id.isBlank()) return listOf("$prefix: id required")
        if (!idRegex.matches(pick.id)) errors += "$prefix.id: invalid id format"
        if (pick.id in (knownIds ?: emptySet())) errors += "$prefix.id: duplicate id"
        knownIds?.add(pick.id)

        if (pick.title_ru.isBlank()) errors += "$prefix.title_ru required"
        errors += validateActionUrl("$prefix.action_url", pick.action_url)
        if (!pick.image_url.isNullOrBlank()) {
            errors += validateHttpsImageUrl("$prefix.image_url", pick.image_url)
        }
        pick.placements.forEach { placement ->
            if (placement !in allowedPlacements) {
                errors += "$prefix.placements: unknown placement '$placement'"
            }
        }
        pick.device_types.forEach { deviceType ->
            if (deviceType !in allowedDeviceFilters) {
                errors += "$prefix.device_types: must be station, tv, or phone"
            }
        }
        return errors
    }

    fun validateActionUrl(field: String, url: String): List<String> {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return listOf("$field: required")
        val uri = runCatching { URI(trimmed) }.getOrNull()
            ?: return listOf("$field: invalid URL")
        val valid = when (uri.scheme) {
            "https" -> !uri.host.isNullOrBlank()
            "market" -> true
            else -> false
        }
        return if (valid) emptyList() else listOf("$field: must be https:// or market:// URL")
    }

    fun validateImageUrl(field: String, imageUrl: String): List<String> =
        validateHttpsImageUrl(field, imageUrl)

    private fun validateHttpsImageUrl(field: String, imageUrl: String): List<String> {
        val uri = runCatching { URI(imageUrl.trim()) }.getOrNull()
            ?: return listOf("$field: invalid URL")
        if (uri.scheme != "https") {
            val host = uri.host?.lowercase()
            val httpLocalOk = uri.scheme == "http" && host in LOCAL_HTTP_HOSTS
            if (!httpLocalOk) return listOf("$field: must be https URL")
        }
        val host = uri.host?.lowercase()
        if (host.isNullOrBlank()) return listOf("$field: host required")
        if (host !in iconUrlAllowedHosts && host !in LOCAL_HTTP_HOSTS) {
            return listOf("$field: host not allowed")
        }
        return emptyList()
    }

    companion object {
        private val LOCAL_HTTP_HOSTS = setOf("localhost", "127.0.0.1")
    }
}
