package ru.appforsale.alicecommands.api.domain.ports

import ru.appforsale.alicecommands.api.domain.ContentQueueItemDto
import ru.appforsale.alicecommands.api.domain.EditorialRecordDto
import ru.appforsale.alicecommands.api.domain.InventoryItemRecord
import ru.appforsale.alicecommands.api.domain.PipelineSyncPayload

interface ContentPipelineRepository {
    fun replaceInventory(items: List<InventoryItemRecord>)
    fun listInventory(): List<InventoryItemRecord>
    fun replaceEditorial(records: List<EditorialRecordDto>)
    fun listEditorial(): List<EditorialRecordDto>
    fun getEditorial(commandId: String): EditorialRecordDto?
    fun upsertEditorial(record: EditorialRecordDto)
    fun replaceQueue(items: List<ContentQueueItemDto>)
    fun listQueue(status: String? = "open"): List<ContentQueueItemDto>
    fun getQueueItem(id: String): ContentQueueItemDto?
    fun resolveQueueItem(id: String, status: String)
    fun syncAll(payload: PipelineSyncPayload)
    fun pipelineStats(): PipelineStats
}

data class PipelineStats(
    val inventoryCount: Int,
    val editorialApproved: Int,
    val editorialPending: Int,
    val openQueue: Int,
)
