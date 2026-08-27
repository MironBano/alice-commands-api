package ru.appforsale.alicecommands.api.application.publish

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.BundleCodec
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
import ru.appforsale.alicecommands.api.infrastructure.validation.JsonSchemaValidator
import kotlin.io.path.Path

class ImportJsonSyncModeTest {

    private class FakeDraftRepository : DraftRepository {
        val updated: MutableList<Command> = mutableListOf()
        var existing: Command? = null
        val categories: MutableMap<String, Category> = mutableMapOf()

        override fun loadFull(contentVersion: Int, minAppVersion: String): ContentBundle =
            error("not used")

        override fun stats(): DraftStats = DraftStats(0, 0, 0, 0, 0, 0, 0, 0)

        override fun listCategories(): List<Category> = categories.values.toList()

        override fun getCategory(id: String): Category? = categories[id]

        override fun createCategory(category: Category) {
            categories[category.id] = category
        }

        override fun updateCategory(category: Category) {
            categories[category.id] = category
        }

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

        override fun getCommand(id: String): Command? = existing?.takeIf { it.id == id }

        override fun createCommand(command: Command) {
            updated += command
        }

        override fun updateCommand(command: Command) {
            updated += command
        }

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

    private class FakePipelineRepository : ContentPipelineRepository {
        override fun replaceInventory(items: List<InventoryItemRecord>) = Unit
        override fun listInventory(): List<InventoryItemRecord> = emptyList()
        override fun replaceEditorial(records: List<EditorialRecordDto>) = Unit
        override fun listEditorial(): List<EditorialRecordDto> = listOf(
            EditorialRecordDto(
                command_id = "music_test",
                category_id = "music",
                title_ru = "Джаз",
                effect_description_ru = "Включит джаз.",
                status = "approved",
            ),
        )
        override fun getEditorial(commandId: String): EditorialRecordDto? =
            listEditorial().firstOrNull { it.command_id == commandId }

        override fun upsertEditorial(record: EditorialRecordDto) = Unit
        override fun replaceQueue(items: List<ContentQueueItemDto>) = Unit
        override fun listQueue(status: String?): List<ContentQueueItemDto> = emptyList()
        override fun getQueueItem(id: String): ContentQueueItemDto? = null
        override fun resolveQueueItem(id: String, status: String) = Unit
        override fun syncAll(payload: PipelineSyncPayload) = Unit
        override fun pipelineStats(): PipelineStats = PipelineStats(0, 1, 0, 0)
    }

    @Test
    fun `sync mode keeps approved editorial title and effect`() {
        val draft = FakeDraftRepository()
        draft.existing = Command(
            id = "music_test",
            category_id = "music",
            title_ru = "Джаз",
            phrases = listOf("Алиса, включи джаз"),
            effect_description_ru = "Включит джаз.",
            requires_alice_word = true,
            source_url = "https://example.com/old",
            updated_at = "2026-01-01T00:00:00Z",
        )
        val schemaPath = listOf(
            Path("schema/content-bundle.schema.json"),
            Path("../schema/content-bundle.schema.json"),
        ).first { it.toFile().exists() }
        val schema = JsonSchemaValidator(schemaPath, BundleCodec.json)
        val visuals = CategoryVisualValidationUseCase(setOf("cdn.alicecommands.ru", "localhost"))
        val useCase = ImportJsonUseCase(draft, FakePipelineRepository(), schema, visuals)
        val incoming = ContentBundle(
            published_at = "1970-01-01T00:00:00Z",
            commands = listOf(
                Command(
                    id = "music_test",
                    category_id = "music",
                    title_ru = "Плохой заголовок",
                    phrases = listOf("Алиса, включи джаз", "Алиса, поставь джаз"),
                    effect_description_ru = "Алиса выполнит команду…",
                    requires_alice_word = true,
                    source_url = "https://example.com/new",
                    updated_at = "2026-06-28T00:00:00Z",
                ),
            ),
        )
        useCase.execute(BundleCodec.json.encodeToString(incoming), ImportJsonUseCase.Mode.SYNC)

        assertEquals(1, draft.updated.size)
        val merged = draft.updated.single()
        assertEquals("Джаз", merged.title_ru)
        assertEquals("Включит джаз.", merged.effect_description_ru)
        assertTrue(merged.phrases.contains("Алиса, поставь джаз"))
        assertEquals("https://example.com/new", merged.source_url)
    }

    @Test
    fun `sync mode keeps approved editorial but applies group fields from incoming`() {
        val draft = FakeDraftRepository()
        draft.existing = Command(
            id = "music_test",
            category_id = "music",
            title_ru = "Джаз",
            phrases = listOf("Алиса, включи джаз"),
            effect_description_ru = "Включит джаз.",
            requires_alice_word = true,
            source_url = "https://example.com/old",
            updated_at = "2026-01-01T00:00:00Z",
        )
        val schemaPath = listOf(
            Path("schema/content-bundle.schema.json"),
            Path("../schema/content-bundle.schema.json"),
        ).first { it.toFile().exists() }
        val schema = JsonSchemaValidator(schemaPath, BundleCodec.json)
        val visuals = CategoryVisualValidationUseCase(setOf("cdn.alicecommands.ru", "localhost"))
        val useCase = ImportJsonUseCase(draft, FakePipelineRepository(), schema, visuals)
        val incoming = ContentBundle(
            published_at = "1970-01-01T00:00:00Z",
            command_groups = listOf(
                ru.appforsale.alicecommands.api.domain.CommandGroup(
                    id = "music_group",
                    category_id = "music",
                    title_ru = "Группа",
                    sort_order = 10,
                ),
            ),
            commands = listOf(
                Command(
                    id = "music_test",
                    category_id = "music",
                    title_ru = "Плохой заголовок",
                    phrases = listOf("Алиса, включи джаз"),
                    effect_description_ru = "Алиса выполнит команду…",
                    requires_alice_word = true,
                    source_url = "https://example.com/new",
                    updated_at = "2026-06-28T00:00:00Z",
                    group_id = "music_group",
                    sort_order = 10,
                    variant_label_ru = "Джаз",
                    is_primary_in_group = true,
                    search_aliases = listOf("музыка"),
                ),
            ),
        )
        useCase.execute(BundleCodec.json.encodeToString(incoming), ImportJsonUseCase.Mode.SYNC)

        val merged = draft.updated.single()
        assertEquals("music_group", merged.group_id)
        assertEquals(10, merged.sort_order)
        assertEquals("Джаз", merged.variant_label_ru)
        assertTrue(merged.is_primary_in_group)
        assertEquals(listOf("музыка"), merged.search_aliases)
        assertEquals("Джаз", merged.title_ru)
    }
}
