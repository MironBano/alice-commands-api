package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ContentQueueItemDto
import ru.appforsale.alicecommands.api.domain.EditorialRecordDto
import ru.appforsale.alicecommands.api.domain.PipelineStatusResponse
import ru.appforsale.alicecommands.api.domain.PipelineSyncPayload
import ru.appforsale.alicecommands.api.domain.QueueActionRequest
import ru.appforsale.alicecommands.api.domain.ports.ContentPipelineRepository
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.SchemaValidator
import ru.appforsale.alicecommands.api.application.BundleCodec
import java.time.Instant

class SyncPipelineUseCase(
    private val pipelineRepository: ContentPipelineRepository,
) {
    fun execute(payload: PipelineSyncPayload) {
        pipelineRepository.syncAll(payload)
    }

    fun status(): PipelineStatusResponse {
        val stats = pipelineRepository.pipelineStats()
        val inventoryIds = pipelineRepository.listInventory().map { it.command_id }.toSet()
        val catalogCount = pipelineRepository.listEditorial()
            .count { it.status == "approved" && it.command_id in inventoryIds }
        return PipelineStatusResponse(
            inventory_count = stats.inventoryCount,
            editorial_approved = stats.editorialApproved,
            editorial_pending = stats.editorialPending,
            open_queue = stats.openQueue,
            catalog_commands = catalogCount,
        )
    }
}

class RebuildDraftFromPipelineUseCase(
    private val pipelineRepository: ContentPipelineRepository,
    private val draftRepository: DraftRepository,
) {
    fun execute(): Int {
        val inventoryById = pipelineRepository.listInventory().associateBy { it.command_id }
        val now = Instant.now().toString()
        var count = 0
        pipelineRepository.listEditorial()
            .filter { it.status == "approved" }
            .forEach { editorial ->
                val inventory = inventoryById[editorial.command_id]
                val existing = draftRepository.getCommand(editorial.command_id)
                val command = DraftCommandMerge.fromEditorial(editorial, inventory, existing, now)
                    ?: return@forEach
                if (existing == null) {
                    draftRepository.createCommand(command)
                } else {
                    draftRepository.updateCommand(command)
                }
                count++
            }
        return count
    }
}

class ApproveQueueItemUseCase(
    private val pipelineRepository: ContentPipelineRepository,
    private val draftRepository: DraftRepository,
) {
    fun execute(itemId: String, body: QueueActionRequest) {
        val item = pipelineRepository.getQueueItem(itemId)
            ?: throw IllegalArgumentException("Queue item not found: $itemId")
        val title = body.title_ru ?: item.title_ru ?: item.phrase ?: item.command_id
        val effect = body.effect_description_ru ?: item.suggested_effect
            ?: throw IllegalArgumentException("effect_description_ru required to approve")
        val now = Instant.now().toString()
        pipelineRepository.upsertEditorial(
            EditorialRecordDto(
                command_id = item.command_id,
                category_id = item.category_id ?: "general",
                title_ru = title,
                effect_description_ru = effect,
                status = "approved",
                approved_at = now,
                updated_at = now,
            ),
        )
        pipelineRepository.resolveQueueItem(itemId, "resolved")
        applyApprovedToDraft(item.command_id, now)
    }

    private fun applyApprovedToDraft(commandId: String, updatedAt: String) {
        val editorial = pipelineRepository.getEditorial(commandId) ?: return
        if (editorial.status != "approved") return
        val inventory = pipelineRepository.listInventory().firstOrNull { it.command_id == commandId }
        val existing = draftRepository.getCommand(commandId)
        val command = DraftCommandMerge.fromEditorial(editorial, inventory, existing, updatedAt) ?: return
        if (existing == null) {
            draftRepository.createCommand(command)
        } else {
            draftRepository.updateCommand(command)
        }
    }
}

class DismissQueueItemUseCase(
    private val pipelineRepository: ContentPipelineRepository,
) {
    fun execute(itemId: String) {
        pipelineRepository.getQueueItem(itemId)
            ?: throw IllegalArgumentException("Queue item not found: $itemId")
        pipelineRepository.resolveQueueItem(itemId, "dismissed")
    }
}

class ImportJsonUseCase(
    private val draftRepository: DraftRepository,
    private val pipelineRepository: ContentPipelineRepository,
    private val schemaValidator: SchemaValidator,
    private val categoryVisualValidationUseCase: CategoryVisualValidationUseCase,
) {
    enum class Mode { REPLACE, MERGE, SYNC }

    fun execute(jsonText: String, mode: Mode) {
        categoryVisualValidationUseCase.validateJsonForForbiddenCommandFields(jsonText)
        schemaValidator.validateJson(jsonText)
        val bundle = BundleCodec.json.decodeFromString<ContentBundle>(jsonText)
        when (mode) {
            Mode.REPLACE -> draftRepository.replaceAll(bundle)
            Mode.MERGE -> draftRepository.merge(bundle)
            Mode.SYNC -> syncToDraft(bundle)
        }
    }

    private fun syncToDraft(bundle: ContentBundle) {
        bundle.categories.forEach { cat ->
            if (draftRepository.getCategory(cat.id) == null) {
                draftRepository.createCategory(cat)
            } else {
                draftRepository.updateCategory(cat)
            }
        }

        bundle.command_groups.forEach { group ->
            if (draftRepository.getCommandGroup(group.id) == null) {
                draftRepository.createCommandGroup(group)
            } else {
                draftRepository.updateCommandGroup(group)
            }
        }

        val editorialById = pipelineRepository.listEditorial()
            .associateBy { it.command_id }

        bundle.commands.forEach { incoming ->
            val existing = draftRepository.getCommand(incoming.id)
            val editorial = editorialById[incoming.id]
            val merged = mergeCommand(incoming, existing, editorial)
            if (existing == null) {
                draftRepository.createCommand(merged)
            } else {
                draftRepository.updateCommand(merged)
            }
        }
    }

    private fun mergeCommand(
        incoming: Command,
        existing: Command?,
        editorial: EditorialRecordDto?,
    ): Command = DraftCommandMerge.syncImportWithEditorial(incoming, existing, editorial)
}
