package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.application.publish.ImportEditorialReviewUseCase
import ru.appforsale.alicecommands.api.application.publish.RebuildDraftFromPipelineUseCase
import ru.appforsale.alicecommands.api.domain.AffiliateBlock
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.ChecklistItem
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.DraftStats
import ru.appforsale.alicecommands.api.domain.EditorialEditFields
import ru.appforsale.alicecommands.api.domain.EditorialExportDocument
import ru.appforsale.alicecommands.api.domain.EditorialRecordDto
import ru.appforsale.alicecommands.api.domain.EditorialReviewRecord
import ru.appforsale.alicecommands.api.domain.InventoryItemRecord
import ru.appforsale.alicecommands.api.domain.ScenarioTemplate
import ru.appforsale.alicecommands.api.domain.ports.ContentPipelineRepository
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.PipelineStats
import java.time.Instant

class EditorialImportTest {

    @Test
    fun `import updates editorial and rebuilds draft`() {
        val pipeline = FakePipelineRepo()
        val draft = FakeDraftRepository()
        val rebuild = RebuildDraftFromPipelineUseCase(pipeline, draft)
        val import = ImportEditorialReviewUseCase(pipeline, rebuild)

        pipeline.inventory.add(
            InventoryItemRecord(
                command_id = "general_privet",
                category_id = "general",
                phrases = listOf("Алиса, привет"),
                raw_result = "Приветствие",
                source_url = "https://example.com",
            ),
        )
        pipeline.editorial["general_privet"] = EditorialRecordDto(
            command_id = "general_privet",
            category_id = "general",
            title_ru = "Привет",
            effect_description_ru = "Требует вычитки",
            status = "pending",
            updated_at = Instant.now().toString(),
        )

        val doc = EditorialExportDocument(
            exported_at = Instant.now().toString(),
            filter = "review",
            instructions = "test",
            records = listOf(
                EditorialReviewRecord(
                    command_id = "general_privet",
                    category_id = "general",
                    phrase_example = "Алиса, привет",
                    edit = EditorialEditFields(
                        command_id = "general_privet",
                        title_ru = "Приветствие",
                        effect_description_ru = "Алиса поздоровается.",
                        status = "approved",
                    ),
                ),
            ),
        )
        val result = import.execute(BundleCodec.json.encodeToString(doc))

        assertEquals(1, result.updated)
        assertEquals(1, result.draft_rebuilt)
        assertEquals("approved", pipeline.editorial["general_privet"]?.status)
        assertEquals("Алиса поздоровается.", draft.updated.last().effect_description_ru)
    }

    @Test
    fun `import rebuilds existing draft command when inventory is missing`() {
        val pipeline = FakePipelineRepo()
        val draft = FakeDraftRepository()
        val rebuild = RebuildDraftFromPipelineUseCase(pipeline, draft)
        val import = ImportEditorialReviewUseCase(pipeline, rebuild)

        draft.existing["alice_plus_animopus"] = Command(
            id = "alice_plus_animopus",
            category_id = "alice_plus",
            title_ru = "Старый заголовок",
            phrases = listOf("Алиса, анимопус"),
            effect_description_ru = "Старый мусор из справки.",
            requires_alice_word = true,
            requires_plus = true,
            device_types = listOf("station"),
            related_command_ids = listOf("alice_plus_kubokot"),
            source_url = "https://alice.yandex.ru/support/ru/assistant/alice-plus/kids",
            published_at = "2026-01-01T00:00:00Z",
            updated_at = Instant.now().toString(),
            tags = listOf("alice_plus"),
        )

        val doc = EditorialExportDocument(
            exported_at = Instant.now().toString(),
            filter = "all",
            instructions = "test",
            records = listOf(
                EditorialReviewRecord(
                    command_id = "alice_plus_animopus",
                    category_id = "alice_plus",
                    edit = EditorialEditFields(
                        command_id = "alice_plus_animopus",
                        title_ru = "Анимопус",
                        effect_description_ru = "Запустит детскую игру «Анимопус». Нужно: устройство с Алисой; подписка Яндекс Плюс.",
                        status = "approved",
                    ),
                ),
            ),
        )

        val result = import.execute(BundleCodec.json.encodeToString(doc))

        assertEquals(1, result.updated)
        assertEquals(1, result.draft_rebuilt)
        val rebuilt = draft.updated.single()
        assertEquals("alice_plus_animopus", rebuilt.id)
        assertEquals("alice_plus", rebuilt.category_id)
        assertEquals("Анимопус", rebuilt.title_ru)
        assertEquals(listOf("Алиса, анимопус"), rebuilt.phrases)
        assertEquals("Запустит детскую игру «Анимопус». Нужно: устройство с Алисой; подписка Яндекс Плюс.", rebuilt.effect_description_ru)
        assertEquals(true, rebuilt.requires_alice_word)
        assertEquals(true, rebuilt.requires_plus)
        assertEquals(listOf("station"), rebuilt.device_types)
        assertEquals(listOf("alice_plus_kubokot"), rebuilt.related_command_ids)
        assertEquals("https://alice.yandex.ru/support/ru/assistant/alice-plus/kids", rebuilt.source_url)
        assertEquals("2026-01-01T00:00:00Z", rebuilt.published_at)
        assertEquals(listOf("alice_plus"), rebuilt.tags)
    }

    private class FakeDraftRepository : DraftRepository {
        val updated: MutableList<Command> = mutableListOf()
        val existing: MutableMap<String, Command> = mutableMapOf()

        override fun loadFull(contentVersion: Int, minAppVersion: String): ContentBundle = error("not used")
        override fun stats(): DraftStats = DraftStats(0, 0, 0, 0, 0, 0)
        override fun listCategories(): List<Category> = emptyList()
        override fun getCategory(id: String): Category? = null
        override fun createCategory(category: Category) = Unit
        override fun updateCategory(category: Category) = Unit
        override fun deleteCategory(id: String) = Unit
        override fun reorderCategories(orderedIds: List<String>) = Unit
        override fun listCommandGroups(categoryId: String?) = emptyList<ru.appforsale.alicecommands.api.domain.CommandGroup>()
        override fun getCommandGroup(id: String) = null
        override fun createCommandGroup(group: ru.appforsale.alicecommands.api.domain.CommandGroup) = Unit
        override fun updateCommandGroup(group: ru.appforsale.alicecommands.api.domain.CommandGroup) = Unit
        override fun deleteCommandGroup(id: String) = Unit
        override fun reorderCommandGroups(orderedIds: List<String>) = Unit
        override fun bulkAssignCommandsToGroup(commandIds: List<String>, groupId: String?) = Unit
        override fun listCommands(categoryId: String?): List<Command> = emptyList()
        override fun getCommand(id: String): Command? = existing[id]
        override fun createCommand(command: Command) { existing[command.id] = command; updated += command }
        override fun updateCommand(command: Command) { existing[command.id] = command; updated += command }
        override fun deleteCommand(id: String) = Unit
        override fun listScenarioTemplates(): List<ScenarioTemplate> = emptyList()
        override fun getScenarioTemplate(id: String): ScenarioTemplate? = null
        override fun createScenarioTemplate(template: ScenarioTemplate) = Unit
        override fun updateScenarioTemplate(template: ScenarioTemplate) = Unit
        override fun deleteScenarioTemplate(id: String) = Unit
        override fun listChecklistItems(): List<ChecklistItem> = emptyList()
        override fun updateChecklistItems(items: List<ChecklistItem>) = Unit
        override fun listAffiliateBlocks(): List<AffiliateBlock> = emptyList()
        override fun getAffiliateBlock(id: String): AffiliateBlock? = null
        override fun createAffiliateBlock(block: AffiliateBlock) = Unit
        override fun updateAffiliateBlock(block: AffiliateBlock) = Unit
        override fun deleteAffiliateBlock(id: String) = Unit
        override fun replaceAll(bundle: ContentBundle) = Unit
        override fun merge(bundle: ContentBundle) = Unit
        override fun getCommandOfDaySettings(): ru.appforsale.alicecommands.api.domain.CommandOfDaySettings? = null
        override fun upsertCommandOfDaySettings(settings: ru.appforsale.alicecommands.api.domain.CommandOfDaySettings) = Unit
    }

    private class FakePipelineRepo : ContentPipelineRepository {
        val inventory = mutableListOf<InventoryItemRecord>()
        val editorial = mutableMapOf<String, EditorialRecordDto>()

        override fun replaceInventory(items: List<InventoryItemRecord>) { inventory.clear(); inventory.addAll(items) }
        override fun listInventory(): List<InventoryItemRecord> = inventory
        override fun replaceEditorial(records: List<EditorialRecordDto>) { editorial.clear(); records.forEach { editorial[it.command_id] = it } }
        override fun listEditorial(): List<EditorialRecordDto> = editorial.values.toList()
        override fun getEditorial(commandId: String): EditorialRecordDto? = editorial[commandId]
        override fun upsertEditorial(record: EditorialRecordDto) { editorial[record.command_id] = record }
        override fun replaceQueue(items: List<ru.appforsale.alicecommands.api.domain.ContentQueueItemDto>) {}
        override fun listQueue(status: String?) = emptyList<ru.appforsale.alicecommands.api.domain.ContentQueueItemDto>()
        override fun getQueueItem(id: String) = null
        override fun resolveQueueItem(id: String, status: String) {}
        override fun syncAll(payload: ru.appforsale.alicecommands.api.domain.PipelineSyncPayload) {}
        override fun pipelineStats() = PipelineStats(0, 0, 0, 0)
    }
}
