package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.publish.DraftCommandMerge
import ru.appforsale.alicecommands.api.application.publish.RebuildDraftFromPipelineUseCase
import ru.appforsale.alicecommands.api.domain.AffiliateBlock
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.ChecklistItem
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ContentQueueItemDto
import ru.appforsale.alicecommands.api.domain.DraftStats
import ru.appforsale.alicecommands.api.domain.EditorialRecordDto
import ru.appforsale.alicecommands.api.domain.InventoryItemRecord
import ru.appforsale.alicecommands.api.domain.PipelineSyncPayload
import ru.appforsale.alicecommands.api.domain.ScenarioTemplate
import ru.appforsale.alicecommands.api.domain.ports.ContentPipelineRepository
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.PipelineStats
import java.time.Instant

class DraftCommandMergeTest {

    @Test
    fun `fromEditorial preserves schema v2 fields on existing command`() {
        val existing = Command(
            id = "alice_plus_aktivirui_promokod_plius",
            category_id = "alice_plus",
            title_ru = "Старый",
            phrases = listOf("Алиса, активируй промокод Плюс"),
            effect_description_ru = "Старый эффект.",
            requires_alice_word = true,
            requires_plus = true,
            device_types = listOf("station", "phone"),
            related_command_ids = listOf("other"),
            source_url = "https://alice.yandex.ru/support/ru/assistant/alice-plus/",
            updated_at = "2026-01-01T00:00:00Z",
            tags = listOf("alice_plus"),
            group_id = "alice_plus_subscription",
            sort_order = 30,
            variant_label_ru = "Промокод",
            is_primary_in_group = false,
            search_aliases = listOf("промокод"),
        )
        val editorial = EditorialRecordDto(
            command_id = existing.id,
            category_id = "general",
            title_ru = "Активируй промокод Плюс",
            effect_description_ru = "Активирует промокод подписки.",
            status = "approved",
        )
        val inventory = InventoryItemRecord(
            command_id = existing.id,
            category_id = "general",
            phrases = listOf("Алиса, промокод"),
            raw_result = "x",
            source_url = "https://example.com/new",
            requires_plus = false,
        )

        val merged = DraftCommandMerge.fromEditorial(editorial, inventory, existing, Instant.now().toString())!!

        assertEquals("Активируй промокод Плюс", merged.title_ru)
        assertEquals("Активирует промокод подписки.", merged.effect_description_ru)
        assertEquals("alice_plus", merged.category_id)
        assertEquals("alice_plus_subscription", merged.group_id)
        assertEquals(30, merged.sort_order)
        assertEquals("Промокод", merged.variant_label_ru)
        assertEquals(listOf("промокод"), merged.search_aliases)
        assertEquals(true, merged.requires_plus)
        assertEquals(listOf("alice_plus"), merged.tags)
        assertEquals(listOf("other"), merged.related_command_ids)
    }

    @Test
    fun `fromEditorial keeps existing effect when editorial effect is blank`() {
        val existing = command("c1", effect = "Хороший эффект.")
        val editorial = editorial("c1", effect = "")
        val merged = DraftCommandMerge.fromEditorial(editorial, null, existing, "now")!!
        assertEquals("Хороший эффект.", merged.effect_description_ru)
    }

    @Test
    fun `rebuild use case does not strip group metadata`() {
        val pipeline = MergeTestPipelineRepo()
        val draft = MergeTestDraftRepository()
        draft.existing["music_test"] = Command(
            id = "music_test",
            category_id = "general",
            title_ru = "Старый",
            phrases = listOf("Алиса, включи музыку"),
            effect_description_ru = "Старый.",
            requires_alice_word = true,
            requires_plus = true,
            source_url = "https://example.com",
            updated_at = "2026-01-01T00:00:00Z",
            tags = listOf("music"),
            group_id = "general_music_migrated",
            sort_order = 5,
            variant_label_ru = "Музыка",
            search_aliases = listOf("музыка"),
        )
        pipeline.inventory.add(
            InventoryItemRecord(
                command_id = "music_test",
                category_id = "music",
                phrases = listOf("Алиса, включи музыку"),
                raw_result = "x",
                source_url = "https://example.com",
                requires_plus = false,
            ),
        )
        pipeline.editorial["music_test"] = EditorialRecordDto(
            command_id = "music_test",
            category_id = "music",
            title_ru = "Включи музыку",
            effect_description_ru = "Запустит музыку.",
            status = "approved",
        )

        val count = RebuildDraftFromPipelineUseCase(pipeline, draft).execute()

        assertEquals(1, count)
        val rebuilt = draft.updated.single()
        assertEquals("general_music_migrated", rebuilt.group_id)
        assertEquals("general", rebuilt.category_id)
        assertEquals(listOf("music"), rebuilt.tags)
        assertEquals(true, rebuilt.requires_plus)
        assertEquals("Запустит музыку.", rebuilt.effect_description_ru)
    }

    @Test
    fun `fromAdminPut preserves group metadata when omitted in body`() {
        val existing = Command(
            id = "music_test",
            category_id = "music",
            title_ru = "Старый",
            phrases = listOf("Алиса, включи музыку"),
            effect_description_ru = "Старый.",
            requires_alice_word = true,
            source_url = "https://example.com",
            updated_at = "2026-01-01T00:00:00Z",
            group_id = "music_playback",
            sort_order = 5,
            variant_label_ru = "Музыка",
            search_aliases = listOf("музыка"),
        )
        val incoming = existing.copy(
            title_ru = "Включи музыку",
            effect_description_ru = "Новый эффект.",
            group_id = null,
            sort_order = null,
            variant_label_ru = null,
            search_aliases = emptyList(),
            updated_at = "now",
        )
        val merged = DraftCommandMerge.fromAdminPut(existing, incoming)
        assertEquals("music_playback", merged.group_id)
        assertEquals(5, merged.sort_order)
        assertEquals("Музыка", merged.variant_label_ru)
        assertEquals(listOf("музыка"), merged.search_aliases)
        assertEquals("Новый эффект.", merged.effect_description_ru)
    }

    private fun command(id: String, effect: String) = Command(
        id = id,
        category_id = "general",
        title_ru = "T",
        phrases = listOf("Алиса, t"),
        effect_description_ru = effect,
        requires_alice_word = true,
        source_url = "https://example.com",
        updated_at = "now",
    )

    private fun editorial(id: String, effect: String) = EditorialRecordDto(
        command_id = id,
        category_id = "general",
        title_ru = "T",
        effect_description_ru = effect,
        status = "approved",
    )

    private class MergeTestDraftRepository : DraftRepository {
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

    private class MergeTestPipelineRepo : ContentPipelineRepository {
        val inventory = mutableListOf<InventoryItemRecord>()
        val editorial = mutableMapOf<String, EditorialRecordDto>()

        override fun replaceInventory(items: List<InventoryItemRecord>) { inventory.clear(); inventory.addAll(items) }
        override fun listInventory(): List<InventoryItemRecord> = inventory
        override fun replaceEditorial(records: List<EditorialRecordDto>) { editorial.clear(); records.forEach { editorial[it.command_id] = it } }
        override fun listEditorial(): List<EditorialRecordDto> = editorial.values.toList()
        override fun getEditorial(commandId: String): EditorialRecordDto? = editorial[commandId]
        override fun upsertEditorial(record: EditorialRecordDto) { editorial[record.command_id] = record }
        override fun replaceQueue(items: List<ContentQueueItemDto>) {}
        override fun listQueue(status: String?) = emptyList<ContentQueueItemDto>()
        override fun getQueueItem(id: String) = null
        override fun resolveQueueItem(id: String, status: String) {}
        override fun syncAll(payload: PipelineSyncPayload) {}
        override fun pipelineStats() = PipelineStats(0, 0, 0, 0)
    }
}
