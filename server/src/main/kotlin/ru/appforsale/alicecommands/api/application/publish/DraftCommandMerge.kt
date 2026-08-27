package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.EditorialRecordDto
import ru.appforsale.alicecommands.api.domain.InventoryItemRecord

/**
 * Safe merge when editorial / inventory updates draft commands.
 * Editorial changes titles and effects only; schema v2 layout and device flags stay on draft.
 */
object DraftCommandMerge {
    fun fromEditorial(
        editorial: EditorialRecordDto,
        inventory: InventoryItemRecord?,
        existing: Command?,
        now: String,
    ): Command? {
        if (inventory == null && existing == null) return null

        if (existing != null) {
            return existing.copy(
                title_ru = editorial.title_ru,
                effect_description_ru = editorial.effect_description_ru.takeIf { it.isNotBlank() }
                    ?: existing.effect_description_ru,
                phrases = inventory?.phrases?.takeIf { it.isNotEmpty() } ?: existing.phrases,
                source_url = inventory?.source_url?.takeIf { it.isNotBlank() } ?: existing.source_url,
                updated_at = editorial.updated_at ?: now,
            )
        }

        val inv = inventory ?: return null
        return Command(
            id = editorial.command_id,
            category_id = editorial.category_id,
            title_ru = editorial.title_ru,
            phrases = inv.phrases,
            effect_description_ru = editorial.effect_description_ru,
            requires_alice_word = inv.requires_alice_word,
            requires_plus = inv.requires_plus,
            device_types = inv.device_types,
            source_url = inv.source_url,
            updated_at = editorial.updated_at ?: now,
            tags = listOf(editorial.category_id),
        )
    }

    /**
     * Admin PUT: full command body from form/JSON editor.
     * Only [Command.published_at] is preserved from existing (client must not rewrite publish stamp).
     */
    fun fromAdminPut(existing: Command, incoming: Command): Command =
        incoming.copy(published_at = existing.published_at)

    fun syncImportWithEditorial(
        incoming: Command,
        existing: Command?,
        editorial: EditorialRecordDto?,
    ): Command {
        if (editorial?.status != "approved") return incoming
        val base = existing ?: incoming
        val mergedPhrases = ((existing?.phrases ?: emptyList()) + incoming.phrases).distinct()
        return base.copy(
            phrases = mergedPhrases,
            source_url = incoming.source_url.ifBlank { base.source_url },
            device_types = incoming.device_types.ifEmpty { base.device_types },
            title_ru = editorial.title_ru,
            effect_description_ru = editorial.effect_description_ru.takeIf { it.isNotBlank() }
                ?: base.effect_description_ru,
            category_id = existing?.category_id ?: editorial.category_id,
            tags = when {
                incoming.tags.isNotEmpty() -> incoming.tags.filter { it != "needs_review" }
                existing?.tags?.isNotEmpty() == true -> existing.tags
                else -> listOf(editorial.category_id)
            },
            requires_plus = existing?.requires_plus ?: incoming.requires_plus,
            group_id = incoming.group_id ?: base.group_id,
            sort_order = incoming.sort_order ?: base.sort_order,
            variant_label_ru = incoming.variant_label_ru?.takeIf { it.isNotBlank() } ?: base.variant_label_ru,
            is_primary_in_group = if (incoming.group_id != null) {
                incoming.is_primary_in_group
            } else {
                base.is_primary_in_group
            },
            search_aliases = incoming.search_aliases.ifEmpty { base.search_aliases },
            related_command_ids = incoming.related_command_ids.ifEmpty { base.related_command_ids },
        )
    }
}
