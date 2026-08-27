package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ContentValidationWarnings
import ru.appforsale.alicecommands.api.domain.ValidationException
import kotlin.math.pow

class CommandGroupValidationUseCase {

    fun validateForPublish(bundle: ContentBundle) {
        val errors = collectPublishErrors(bundle)
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }

    fun collectWarnings(bundle: ContentBundle): ContentValidationWarnings {
        val categoryIds = bundle.categories.map { it.id }.toSet()
        val groupIds = bundle.command_groups.map { it.id }.toSet()
        val commandsByGroup = bundle.commands.filter { it.group_id != null }.groupBy { it.group_id!! }

        val emptyGroups = bundle.command_groups
            .filter { group -> commandsByGroup[group.id].orEmpty().isEmpty() }
            .map { it.id }

        val orphanCommands = bundle.commands
            .filter { it.group_id == null && it.category_id in categoryIds && bundle.command_groups.any { g -> g.category_id == it.category_id } }
            .map { it.id }

        val missingSortOrder = bundle.commands
            .filter { it.group_id != null && it.sort_order == null }
            .map { it.id }

        val duplicateAliasCommands = bundle.commands
            .filter { cmd -> hasDuplicateAliases(cmd) }
            .map { it.id }

        val visualWarnings = collectVisualWarnings(bundle)

        return ContentValidationWarnings(
            orphan_commands = orphanCommands,
            empty_groups = emptyGroups,
            duplicate_alias_commands = duplicateAliasCommands,
            missing_sort_order_commands = missingSortOrder,
            icon_url_without_icon_key = visualWarnings.first,
            low_contrast_visuals = visualWarnings.second,
        )
    }

    private fun collectVisualWarnings(bundle: ContentBundle): Pair<List<String>, List<String>> {
        val missingIconKey = mutableListOf<String>()
        val lowContrast = mutableListOf<String>()

        bundle.categories.forEach { category ->
            if (!category.icon_url.isNullOrBlank() && category.icon_key.isNullOrBlank()) {
                missingIconKey += "category:${category.id}"
            }
            if (isLowContrastAccent(category.accent_color)) {
                lowContrast += "category:${category.id}"
            }
        }
        bundle.command_groups.forEach { group ->
            if (!group.icon_url.isNullOrBlank() && group.icon_key.isNullOrBlank()) {
                missingIconKey += "group:${group.id}"
            }
            if (isLowContrastAccent(group.accent_color)) {
                lowContrast += "group:${group.id}"
            }
        }
        return missingIconKey to lowContrast
    }

    private fun isLowContrastAccent(accentColor: String?): Boolean {
        if (accentColor.isNullOrBlank() || !accentColor.matches(Regex("^#[0-9A-Fa-f]{6}$"))) return false
        val luminance = relativeLuminance(accentColor)
        return luminance > 0.85 || luminance < 0.08
    }

    private fun relativeLuminance(hex: String): Double {
        val r = hex.substring(1, 3).toInt(16) / 255.0
        val g = hex.substring(3, 5).toInt(16) / 255.0
        val b = hex.substring(5, 7).toInt(16) / 255.0
        fun channel(c: Double): Double =
            if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    private fun collectPublishErrors(bundle: ContentBundle): List<String> {
        val errors = mutableListOf<String>()
        val categoryIds = bundle.categories.map { it.id }.toSet()
        val groupsById = bundle.command_groups.associateBy { it.id }
        if (groupsById.size != bundle.command_groups.size) {
            errors += "command_groups: duplicate group ids"
        }
        val commandsByGroup = bundle.commands.filter { it.group_id != null }.groupBy { it.group_id!! }

        bundle.command_groups.forEach { group ->
            if (group.category_id !in categoryIds) {
                errors += "command_groups.${group.id}: unknown category_id ${group.category_id}"
            }
        }

        bundle.commands.forEach { cmd ->
            val groupId = cmd.group_id ?: return@forEach
            val group = groupsById[groupId]
            if (group == null) {
                errors += "commands.${cmd.id}: unknown group_id $groupId"
            } else if (group.category_id != cmd.category_id) {
                errors += "commands.${cmd.id}: group_id $groupId belongs to category ${group.category_id}, command has ${cmd.category_id}"
            }
            if (cmd.sort_order == null) {
                errors += "commands.${cmd.id}: sort_order required when group_id is set"
            }
            // TODO: re-enable after editorial pass fixes search_aliases vs title overlaps
            // if (hasDuplicateAliases(cmd)) {
            //     errors += "commands.${cmd.id}: search_aliases overlap with title/phrases or contain empty strings"
            // }
        }

        bundle.command_groups.forEach { group ->
            val members = commandsByGroup[group.id].orEmpty()
            if (members.isEmpty()) {
                errors += "command_groups.${group.id}: group has no commands"
            }
            val primaries = members.filter { it.is_primary_in_group }
            if (primaries.size > 1) {
                errors += "command_groups.${group.id}: more than one is_primary_in_group"
            }
            val memberIds = members.map { it.id }.toSet()
            group.preview_command_ids.forEach { previewId ->
                if (previewId !in memberIds) {
                    errors += "command_groups.${group.id}: preview_command_ids contains unknown command $previewId"
                }
            }
        }

        return errors
    }

    private fun hasDuplicateAliases(cmd: Command): Boolean {
        if (cmd.search_aliases.any { it.isBlank() }) return true
        val normalizedTitle = cmd.title_ru.trim().lowercase()
        val normalizedPhrases = cmd.phrases.map { it.trim().lowercase() }.toSet()
        val normalizedAliases = cmd.search_aliases.map { it.trim().lowercase() }
        if (normalizedAliases.size != normalizedAliases.toSet().size) return true
        return normalizedAliases.any { it == normalizedTitle || it in normalizedPhrases }
    }
}
