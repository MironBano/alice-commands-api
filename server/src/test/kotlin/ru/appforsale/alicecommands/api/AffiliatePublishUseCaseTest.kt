package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.application.publish.PublishAffiliateUseCase
import ru.appforsale.alicecommands.api.application.read.DraftPublishStatusService
import ru.appforsale.alicecommands.api.domain.AffiliateBlock
import ru.appforsale.alicecommands.api.domain.AffiliateBlocksResponse
import ru.appforsale.alicecommands.api.domain.AffiliateProduct
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.ChecklistItem
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.CurrentManifest
import ru.appforsale.alicecommands.api.domain.DraftStats
import ru.appforsale.alicecommands.api.domain.PublishHistoryEntry
import ru.appforsale.alicecommands.api.domain.ScenarioTemplate
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository

class AffiliatePublishUseCaseTest {

    @Test
    fun `publish affiliate writes public snapshot without content manifest`() {
        val draft = FakeDraftRepository(
            affiliateBlocks = listOf(
                AffiliateBlock(
                    id = "affiliate_1",
                    context_category_id = "smart_home",
                    title_ru = "Affiliate",
                    erid = "erid",
                    advertiser_name = "Advertiser",
                    products = listOf(
                        AffiliateProduct(
                            title_ru = "Device",
                            market_url = "https://example.com/device",
                            price_hint = "from 1",
                        ),
                    ),
                ),
            ),
        )
        val storage = FakeBundleStorage()

        val result = PublishAffiliateUseCase(draft, storage).execute(updatedAt = "2026-06-29T00:00:00Z")

        assertEquals("2026-06-29T00:00:00Z", result.updated_at)
        assertEquals("affiliate_1", result.blocks.single().id)
        val public = storage.readAffiliate()
        assertNotNull(public)
        assertEquals("https://example.com/device", public!!.blocks.single().products.single().market_url)
        assertEquals(0, storage.writtenBundles.size, "affiliate publish must not write content bundles")
    }

    @Test
    fun `affiliate draft alone does not require catalog publish`() {
        val draft = FakeDraftRepository(stats = DraftStats(0, 0, 0, 0, 0, 1))
        val status = DraftPublishStatusService(draft, NoCurrentManifestRepository, FakeBundleStorage())

        assertFalse(status.hasUnpublishedChanges())
    }

    private class FakeDraftRepository(
        private val stats: DraftStats = DraftStats(0, 0, 0, 0, 0, 0),
        private val affiliateBlocks: List<AffiliateBlock> = emptyList(),
    ) : DraftRepository {
        override fun loadFull(contentVersion: Int, minAppVersion: String): ContentBundle =
            ContentBundle(published_at = "", min_app_version = minAppVersion)

        override fun stats(): DraftStats = stats
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
        override fun getCommand(id: String): Command? = null
        override fun createCommand(command: Command) = Unit
        override fun updateCommand(command: Command) = Unit
        override fun deleteCommand(id: String) = Unit
        override fun listScenarioTemplates(): List<ScenarioTemplate> = emptyList()
        override fun getScenarioTemplate(id: String): ScenarioTemplate? = null
        override fun createScenarioTemplate(template: ScenarioTemplate) = Unit
        override fun updateScenarioTemplate(template: ScenarioTemplate) = Unit
        override fun deleteScenarioTemplate(id: String) = Unit
        override fun listChecklistItems(): List<ChecklistItem> = emptyList()
        override fun updateChecklistItems(items: List<ChecklistItem>) = Unit
        override fun listAffiliateBlocks(): List<AffiliateBlock> = affiliateBlocks
        override fun getAffiliateBlock(id: String): AffiliateBlock? = affiliateBlocks.find { it.id == id }
        override fun createAffiliateBlock(block: AffiliateBlock) = Unit
        override fun updateAffiliateBlock(block: AffiliateBlock) = Unit
        override fun deleteAffiliateBlock(id: String) = Unit
        override fun replaceAll(bundle: ContentBundle) = Unit
        override fun merge(bundle: ContentBundle) = Unit
        override fun getCommandOfDaySettings(): ru.appforsale.alicecommands.api.domain.CommandOfDaySettings? = null
        override fun upsertCommandOfDaySettings(settings: ru.appforsale.alicecommands.api.domain.CommandOfDaySettings) = Unit
    }

    private object NoCurrentManifestRepository : ManifestRepository {
        override fun getCurrent(): CurrentManifest? = null
        override fun nextVersion(): Int = 1
        override fun update(manifest: CurrentManifest) = Unit
        override fun listHistory(limit: Int): List<PublishHistoryEntry> = emptyList()
        override fun insertHistory(entry: PublishHistoryEntry) = Unit
        override fun getHistoryByVersion(version: Int): PublishHistoryEntry? = null
    }

    private class FakeBundleStorage : BundleStorage {
        val writtenBundles: MutableMap<String, ByteArray> = mutableMapOf()
        private var affiliateBytes: ByteArray? = null

        override fun write(filename: String, gzipBytes: ByteArray): String {
            writtenBundles[filename] = gzipBytes
            return filename
        }

        override fun read(filename: String): ByteArray? = writtenBundles[filename]
        override fun exists(filename: String): Boolean = filename in writtenBundles
        override fun isWritable(): Boolean = true
        override fun pruneOldBundles(retention: Int) = Unit
        override fun writeAffiliate(jsonBytes: ByteArray) {
            affiliateBytes = jsonBytes
        }

        override fun readAffiliate(): AffiliateBlocksResponse? =
            affiliateBytes?.decodeToString()?.let { BundleCodec.json.decodeFromString<AffiliateBlocksResponse>(it) }
    }
}
