package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.domain.EditorialEditFields
import ru.appforsale.alicecommands.api.domain.EditorialExportDocument
import ru.appforsale.alicecommands.api.domain.EditorialImportResult
import ru.appforsale.alicecommands.api.domain.EditorialRecordDto
import ru.appforsale.alicecommands.api.domain.EditorialReviewRecord
import ru.appforsale.alicecommands.api.domain.ports.ContentPipelineRepository
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import java.time.Instant

class ImportEditorialReviewUseCase(
    private val pipelineRepository: ContentPipelineRepository,
    private val rebuildDraftFromPipelineUseCase: RebuildDraftFromPipelineUseCase,
) {
    fun execute(jsonText: String): EditorialImportResult {
        val doc = BundleCodec.json.decodeFromString<EditorialExportDocument>(jsonText)
        return applyRecords(doc.records.map { it.toImportFields() })
    }

    fun applyRecords(records: List<EditorialEditFields>): EditorialImportResult {
        val now = Instant.now().toString()
        val inventoryById = pipelineRepository.listInventory().associateBy { it.command_id }
        var updated = 0
        records.forEach { fields ->
            if (fields.command_id.isBlank()) return@forEach
            val existing = pipelineRepository.getEditorial(fields.command_id)
            val inventory = inventoryById[fields.command_id]
            val status = fields.status.ifBlank { "pending" }
            pipelineRepository.upsertEditorial(
                EditorialRecordDto(
                    command_id = fields.command_id,
                    category_id = existing?.category_id ?: inventory?.category_id ?: "general",
                    title_ru = fields.title_ru.trim(),
                    effect_description_ru = fields.effect_description_ru.trim(),
                    status = status,
                    approved_at = if (status == "approved") now else existing?.approved_at,
                    notes = existing?.notes,
                    updated_at = now,
                ),
            )
            if (status == "approved") {
                pipelineRepository.listQueue("open")
                    .filter { it.command_id == fields.command_id }
                    .forEach { pipelineRepository.resolveQueueItem(it.id, "resolved") }
            }
            updated++
        }
        val rebuilt = rebuildDraftFromPipelineUseCase.execute()
        return EditorialImportResult(updated = updated, draft_rebuilt = rebuilt)
    }
}

class SaveEditorialBatchUseCase(
    private val importEditorialReviewUseCase: ImportEditorialReviewUseCase,
) {
    fun execute(records: List<EditorialEditFields>): EditorialImportResult =
        importEditorialReviewUseCase.applyRecords(records)
}

private fun EditorialReviewRecord.toImportFields(): EditorialEditFields =
    EditorialEditFields(
        command_id = command_id,
        title_ru = edit.title_ru,
        effect_description_ru = edit.effect_description_ru,
        status = edit.status,
    )
