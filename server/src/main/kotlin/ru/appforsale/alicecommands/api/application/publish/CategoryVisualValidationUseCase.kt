package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ValidationException
import java.net.URI

class CategoryVisualValidationUseCase(
    private val iconUrlAllowedHosts: Set<String>,
) {
    private val hexColorRegex = Regex("^#[0-9A-Fa-f]{6}$")
    private val slugRegex = Regex("^[a-z][a-z0-9_]*$")

    companion object {
        private val LOCAL_HTTP_HOSTS = setOf("localhost", "127.0.0.1")
    }

    private val forbiddenCommandVisualFields = setOf(
        "icon_url",
        "accent_color",
        "accent_color_dark",
    )

    fun validateForPublish(bundle: ContentBundle) {
        val errors = mutableListOf<String>()
        bundle.categories.forEach { category ->
            errors += validateCategoryVisuals("categories.${category.id}", category)
        }
        bundle.command_groups.forEach { group ->
            errors += validateGroupVisuals("command_groups.${group.id}", group)
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }

    fun validateJsonForForbiddenCommandFields(jsonText: String) {
        val errors = findForbiddenCommandVisualFields(jsonText)
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }

    private fun validateCategoryVisuals(prefix: String, category: Category): List<String> =
        validateVisualFields(prefix, category.icon_url, category.accent_color, category.accent_color_dark)

    private fun validateGroupVisuals(prefix: String, group: CommandGroup): List<String> =
        validateVisualFields(prefix, group.icon_url, group.accent_color, group.accent_color_dark)

    private fun validateVisualFields(
        prefix: String,
        iconUrl: String?,
        accentColor: String?,
        accentColorDark: String?,
    ): List<String> {
        val errors = mutableListOf<String>()
        if (!iconUrl.isNullOrBlank()) {
            errors += validateIconUrl("$prefix.icon_url", iconUrl)
        }
        if (!accentColor.isNullOrBlank() && !hexColorRegex.matches(accentColor)) {
            errors += "$prefix.accent_color: invalid hex color (expected #RRGGBB)"
        }
        if (!accentColorDark.isNullOrBlank() && !hexColorRegex.matches(accentColorDark)) {
            errors += "$prefix.accent_color_dark: invalid hex color (expected #RRGGBB)"
        }
        return errors
    }

    fun validateIconUrl(field: String, iconUrl: String): List<String> {
        val errors = mutableListOf<String>()
        val uri = try {
            URI(iconUrl)
        } catch (_: Exception) {
            return listOf("$field: invalid URL")
        }
        if (uri.scheme != "https") {
            val host = uri.host?.lowercase()
            val httpLocalOk = uri.scheme == "http" && host in LOCAL_HTTP_HOSTS
            if (!httpLocalOk) {
                errors += "$field: only https URLs are allowed"
            }
        }
        val host = uri.host?.lowercase()
        if (host == null || host !in iconUrlAllowedHosts) {
            errors += "$field: host not in allowlist ($host)"
        }
        val path = uri.path.orEmpty()
        if (!path.startsWith("/icons/v1/") || !path.endsWith(".svg")) {
            errors += "$field: path must start with /icons/v1/ and end with .svg"
        }
        val slug = path.removePrefix("/icons/v1/").removeSuffix(".svg")
        if (!slugRegex.matches(slug)) {
            errors += "$field: invalid icon slug in path"
        }
        return errors
    }

    fun validateHexColor(field: String, color: String?): List<String> {
        if (color.isNullOrBlank()) return emptyList()
        return if (hexColorRegex.matches(color)) {
            emptyList()
        } else {
            listOf("$field: invalid hex color (expected #RRGGBB)")
        }
    }

    private fun findForbiddenCommandVisualFields(jsonText: String): List<String> {
        val errors = mutableListOf<String>()
        val commandBlockRegex = Regex(""""commands"\s*:\s*\[(.*?)\]\s*,\s*"(?:scenario_templates|checklist_items)"""", RegexOption.DOT_MATCHES_ALL)
        val commandsSection = commandBlockRegex.find(jsonText)?.groupValues?.get(1) ?: return emptyList()
        forbiddenCommandVisualFields.forEach { field ->
            if (Regex(""""$field"\s*:""").containsMatchIn(commandsSection)) {
                errors += "commands[]: visual field '$field' is not allowed on commands"
            }
        }
        return errors
    }
}
