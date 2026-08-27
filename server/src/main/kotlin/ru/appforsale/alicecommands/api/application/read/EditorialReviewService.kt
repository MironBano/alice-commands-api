package ru.appforsale.alicecommands.api.application.read

import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.EditorialExportDocument
import ru.appforsale.alicecommands.api.domain.EditorialReviewRecord
import ru.appforsale.alicecommands.api.domain.EditorialReviewResponse
import ru.appforsale.alicecommands.api.domain.EditorialTextSnapshot
import ru.appforsale.alicecommands.api.domain.EditorialEditFields
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import ru.appforsale.alicecommands.api.domain.ports.ContentPipelineRepository
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository
import java.time.Instant

class EditorialReviewService(
    private val draftRepository: DraftRepository,
    private val manifestRepository: ManifestRepository,
    private val bundleStorage: BundleStorage,
    private val pipelineRepository: ContentPipelineRepository,
) {
    fun review(filter: String, search: String? = null): EditorialReviewResponse {
        val published = loadPublishedCommands()
        val draft = draftRepository.loadFull().commands.associateBy { it.id }
        val editorial = pipelineRepository.listEditorial().associateBy { it.command_id }
        val inventory = pipelineRepository.listInventory().associateBy { it.command_id }
        val queueOpen = pipelineRepository.listQueue("open").groupBy { it.command_id }

        val candidateIds = linkedSetOf<String>()
        candidateIds.addAll(published.keys)
        candidateIds.addAll(draft.keys)
        candidateIds.addAll(editorial.keys)
        candidateIds.addAll(queueOpen.keys)

        val records = candidateIds
            .sorted()
            .mapNotNull { id ->
                buildRecord(
                    id = id,
                    published = published[id],
                    draft = draft[id],
                    editorial = editorial[id],
                    inventory = inventory[id],
                    queueEvents = queueOpen[id].orEmpty().map { it.event_type }.distinct(),
                )
            }
            .filter { matchesFilter(it, filter) }
            .filter { matchesSearch(it, search) }

        return EditorialReviewResponse(
            filter = filter,
            total = records.size,
            records = records,
        )
    }

    fun exportDocument(filter: String, search: String? = null): EditorialExportDocument {
        val review = review(filter, search)
        return EditorialExportDocument(
            exported_at = Instant.now().toString(),
            filter = filter,
            instructions = EDITORIAL_EXPORT_INSTRUCTIONS,
            records = review.records,
        )
    }

    private fun buildRecord(
        id: String,
        published: Command?,
        draft: Command?,
        editorial: ru.appforsale.alicecommands.api.domain.EditorialRecordDto?,
        inventory: ru.appforsale.alicecommands.api.domain.InventoryItemRecord?,
        queueEvents: List<String>,
    ): EditorialReviewRecord? {
        if (draft == null && editorial == null && queueEvents.isEmpty() && published == null) return null

        val publishedSnap = published?.let {
            EditorialTextSnapshot(it.title_ru, it.effect_description_ru)
        }
        val draftSnap = draft?.let {
            EditorialTextSnapshot(it.title_ru, it.effect_description_ru)
        }

        val reasons = mutableListOf<String>()
        if (published == null && draft != null) reasons.add("added")
        if (published != null && draft == null) reasons.add("removed")
        if (publishedSnap != null && draftSnap != null &&
            (publishedSnap.title_ru != draftSnap.title_ru || publishedSnap.effect_description_ru != draftSnap.effect_description_ru)
        ) {
            reasons.add("changed")
        }
        if (editorial?.status == "pending" || editorial?.status == "ai_draft") reasons.add("pending")
        if (queueEvents.isNotEmpty()) reasons.add("queue")

        val editTitle = editorial?.title_ru ?: draftSnap?.title_ru ?: inventory?.phrases?.firstOrNull() ?: id
        val editEffect = editorial?.effect_description_ru ?: draftSnap?.effect_description_ru ?: "Требует вычитки"
        val editStatus = editorial?.status ?: if (reasons.contains("pending") || reasons.contains("queue")) "pending" else "approved"

        return EditorialReviewRecord(
            command_id = id,
            category_id = editorial?.category_id ?: draft?.category_id ?: inventory?.category_id ?: "general",
            phrase_example = inventory?.phrases?.firstOrNull(),
            phrases = inventory?.phrases ?: draft?.phrases ?: emptyList(),
            raw_result = inventory?.raw_result,
            source_url = inventory?.source_url ?: draft?.source_url,
            published = publishedSnap,
            draft = draftSnap,
            edit = EditorialEditFields(
                command_id = id,
                title_ru = editTitle,
                effect_description_ru = editEffect,
                status = editStatus,
            ),
            reasons = reasons,
            queue_events = queueEvents,
        )
    }

    private fun matchesFilter(record: EditorialReviewRecord, filter: String): Boolean = when (filter.lowercase()) {
        "all" -> true
        "changed" -> "changed" in record.reasons
        "added" -> "added" in record.reasons
        "pending" -> "pending" in record.reasons
        "queue" -> "queue" in record.reasons
        "removed" -> "removed" in record.reasons
        else -> record.reasons.isNotEmpty() // review (default)
    }

    private fun matchesSearch(record: EditorialReviewRecord, search: String?): Boolean {
        if (search.isNullOrBlank()) return true
        val q = search.trim().lowercase()
        return record.command_id.lowercase().contains(q) ||
            record.edit.title_ru.lowercase().contains(q) ||
            (record.phrase_example?.lowercase()?.contains(q) == true) ||
            record.category_id.lowercase().contains(q)
    }

    private fun loadPublishedCommands(): Map<String, Command> {
        val current = manifestRepository.getCurrent() ?: return emptyMap()
        val bytes = bundleStorage.read(current.bundlePath) ?: return emptyMap()
        val bundle = BundleCodec.json.decodeFromString<ContentBundle>(BundleCodec.gunzip(bytes))
        return bundle.commands.associateBy { it.id }
    }

    companion object {
        const val EDITORIAL_EXPORT_INSTRUCTIONS =
            "Отредактируйте edit.title_ru и edit.effect_description_ru. " +
                "Поставьте edit.status=approved для готовых команд. " +
                "Не меняйте command_id. Верните JSON целиком."
    }
}
