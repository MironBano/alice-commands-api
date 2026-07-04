package ru.appforsale.alicecommands.api.infrastructure.persistence

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.appforsale.alicecommands.api.domain.ContentQueueItemDto
import ru.appforsale.alicecommands.api.domain.EditorialRecordDto
import ru.appforsale.alicecommands.api.domain.InventoryItemRecord
import ru.appforsale.alicecommands.api.domain.PipelineSyncPayload
import ru.appforsale.alicecommands.api.domain.ports.ContentPipelineRepository
import ru.appforsale.alicecommands.api.domain.ports.PipelineStats
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ExposedContentPipelineRepository(
    private val database: Database,
) : ContentPipelineRepository {

    private inline fun unitTx(crossinline block: org.jetbrains.exposed.sql.Transaction.() -> Unit) {
        transaction(database) { block() }
    }

    override fun replaceInventory(items: List<InventoryItemRecord>) {
        unitTx {
            InventoryItemsTable.deleteAll()
            items.forEach { item -> insertInventory(item) }
        }
    }

    override fun listInventory(): List<InventoryItemRecord> = transaction(database) {
        InventoryItemsTable.selectAll()
            .orderBy(InventoryItemsTable.categoryId to SortOrder.ASC)
            .map { it.toInventoryItem() }
    }

    override fun replaceEditorial(records: List<EditorialRecordDto>) {
        unitTx {
            EditorialRecordsTable.deleteAll()
            records.forEach { record -> insertEditorial(record) }
        }
    }

    override fun listEditorial(): List<EditorialRecordDto> = transaction(database) {
        EditorialRecordsTable.selectAll()
            .orderBy(EditorialRecordsTable.categoryId to SortOrder.ASC)
            .map { it.toEditorialRecord() }
    }

    override fun getEditorial(commandId: String): EditorialRecordDto? = transaction(database) {
        EditorialRecordsTable.selectAll()
            .where { EditorialRecordsTable.commandId eq commandId }
            .map { it.toEditorialRecord() }
            .singleOrNull()
    }

    override fun upsertEditorial(record: EditorialRecordDto) {
        unitTx {
            val existing = EditorialRecordsTable.selectAll()
                .where { EditorialRecordsTable.commandId eq record.command_id }
                .singleOrNull()
            if (existing == null) {
                insertEditorial(record)
            } else {
                updateEditorial(record)
            }
        }
    }

    override fun replaceQueue(items: List<ContentQueueItemDto>) {
        unitTx {
            ContentQueueTable.deleteAll()
            items.forEach { item -> insertQueueItem(item) }
        }
    }

    override fun listQueue(status: String?): List<ContentQueueItemDto> = transaction(database) {
        val query = if (status != null) {
            ContentQueueTable.selectAll().where { ContentQueueTable.status eq status }
        } else {
            ContentQueueTable.selectAll()
        }
        query.orderBy(ContentQueueTable.createdAt to SortOrder.DESC)
            .map { it.toQueueItem() }
    }

    override fun getQueueItem(id: String): ContentQueueItemDto? = transaction(database) {
        ContentQueueTable.selectAll()
            .where { ContentQueueTable.id eq id }
            .map { it.toQueueItem() }
            .singleOrNull()
    }

    override fun resolveQueueItem(id: String, status: String) {
        unitTx {
            ContentQueueTable.update({ ContentQueueTable.id eq id }) {
                it[ContentQueueTable.status] = status
                it[resolvedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }
    }

    override fun syncAll(payload: PipelineSyncPayload) {
        unitTx {
            InventoryItemsTable.deleteAll()
            payload.inventory.forEach { insertInventory(it) }

            val existingEditorial = EditorialRecordsTable.selectAll()
                .map { it.toEditorialRecord() }
                .associateBy { it.command_id }

            payload.editorial.forEach { incoming ->
                val existing = existingEditorial[incoming.command_id]
                when {
                    existing == null -> insertEditorial(incoming)
                    existing.status == "approved" -> updateEditorial(existing)
                    else -> updateEditorial(incoming)
                }
            }

            val existingQueue = ContentQueueTable.selectAll().map { it.toQueueItem() }
            val closedKeys = existingQueue
                .filter { it.status != "open" }
                .associateBy { queueKey(it) }

            ContentQueueTable.deleteAll()
            payload.queue.forEach { incoming ->
                val key = queueKey(incoming)
                val closed = closedKeys[key]
                if (closed != null) {
                    insertQueueItem(closed)
                } else if (incoming.status == "open") {
                    insertQueueItem(incoming)
                }
            }
            closedKeys.values
                .filter { closed -> payload.queue.none { queueKey(it) == queueKey(closed) } }
                .forEach { insertQueueItem(it) }
        }
    }

    private fun queueKey(item: ContentQueueItemDto): String =
        "${item.event_type}|${item.command_id}|${item.phrase.orEmpty()}"

    override fun pipelineStats(): PipelineStats = transaction(database) {
        val inventoryCount = InventoryItemsTable.selectAll().count().toInt()
        val editorial = EditorialRecordsTable.selectAll().map { it.toEditorialRecord() }
        val openQueue = ContentQueueTable.selectAll()
            .where { ContentQueueTable.status eq "open" }
            .count()
            .toInt()
        PipelineStats(
            inventoryCount = inventoryCount,
            editorialApproved = editorial.count { it.status == "approved" },
            editorialPending = editorial.count { it.status == "pending" || it.status == "ai_draft" },
            openQueue = openQueue,
        )
    }

    private fun insertInventory(item: InventoryItemRecord) {
        InventoryItemsTable.insert {
            it[commandId] = item.command_id
            it[categoryId] = item.category_id
            it[phrases] = item.phrases
            it[rawResult] = item.raw_result
            it[sourceUrl] = item.source_url
            it[section] = item.section
            it[requiresAliceWord] = item.requires_alice_word
            it[requiresPlus] = item.requires_plus
            it[deviceTypes] = item.device_types
            it[sourceId] = item.source_id
            it[lastSeenAt] = parseTime(item.last_seen_at)
            it[deprecated] = item.deprecated
        }
    }

    private fun insertEditorial(record: EditorialRecordDto) {
        EditorialRecordsTable.insert {
            it[commandId] = record.command_id
            it[categoryId] = record.category_id
            it[titleRu] = record.title_ru
            it[effectDescriptionRu] = record.effect_description_ru
            it[status] = record.status
            it[approvedAt] = record.approved_at?.let(::parseTime)
            it[notes] = record.notes
            it[updatedAt] = parseTime(record.updated_at)
        }
    }

    private fun updateEditorial(record: EditorialRecordDto) {
        EditorialRecordsTable.update({ EditorialRecordsTable.commandId eq record.command_id }) {
            it[categoryId] = record.category_id
            it[titleRu] = record.title_ru
            it[effectDescriptionRu] = record.effect_description_ru
            it[status] = record.status
            it[approvedAt] = record.approved_at?.let(::parseTime)
            it[notes] = record.notes
            it[updatedAt] = parseTime(record.updated_at)
        }
    }

    private fun insertQueueItem(item: ContentQueueItemDto) {
        ContentQueueTable.insert {
            it[id] = item.id
            it[eventType] = item.event_type
            it[commandId] = item.command_id
            it[phrase] = item.phrase
            it[categoryId] = item.category_id
            it[titleRu] = item.title_ru
            it[suggestedEffect] = item.suggested_effect
            it[rawResult] = item.raw_result
            it[sourceUrl] = item.source_url
            it[status] = item.status
            it[createdAt] = parseTime(item.created_at)
            it[resolvedAt] = null
        }
    }

    private fun parseTime(value: String?): OffsetDateTime =
        if (value.isNullOrBlank()) {
            OffsetDateTime.now(ZoneOffset.UTC)
        } else {
            OffsetDateTime.parse(value)
        }
}

private fun org.jetbrains.exposed.sql.ResultRow.toInventoryItem() = InventoryItemRecord(
    command_id = this[InventoryItemsTable.commandId],
    category_id = this[InventoryItemsTable.categoryId],
    phrases = this[InventoryItemsTable.phrases],
    raw_result = this[InventoryItemsTable.rawResult],
    source_url = this[InventoryItemsTable.sourceUrl],
    section = this[InventoryItemsTable.section],
    requires_alice_word = this[InventoryItemsTable.requiresAliceWord],
    requires_plus = this[InventoryItemsTable.requiresPlus],
    device_types = this[InventoryItemsTable.deviceTypes].toList(),
    source_id = this[InventoryItemsTable.sourceId],
    last_seen_at = this[InventoryItemsTable.lastSeenAt].toInstant().toString(),
    deprecated = this[InventoryItemsTable.deprecated],
)

private fun org.jetbrains.exposed.sql.ResultRow.toEditorialRecord() = EditorialRecordDto(
    command_id = this[EditorialRecordsTable.commandId],
    category_id = this[EditorialRecordsTable.categoryId],
    title_ru = this[EditorialRecordsTable.titleRu],
    effect_description_ru = this[EditorialRecordsTable.effectDescriptionRu],
    status = this[EditorialRecordsTable.status],
    approved_at = this[EditorialRecordsTable.approvedAt]?.toInstant()?.toString(),
    notes = this[EditorialRecordsTable.notes],
    updated_at = this[EditorialRecordsTable.updatedAt].toInstant().toString(),
)

private fun org.jetbrains.exposed.sql.ResultRow.toQueueItem() = ContentQueueItemDto(
    id = this[ContentQueueTable.id],
    event_type = this[ContentQueueTable.eventType],
    command_id = this[ContentQueueTable.commandId],
    phrase = this[ContentQueueTable.phrase],
    category_id = this[ContentQueueTable.categoryId],
    title_ru = this[ContentQueueTable.titleRu],
    suggested_effect = this[ContentQueueTable.suggestedEffect],
    raw_result = this[ContentQueueTable.rawResult],
    source_url = this[ContentQueueTable.sourceUrl],
    status = this[ContentQueueTable.status],
    created_at = this[ContentQueueTable.createdAt].toInstant().toString(),
)
