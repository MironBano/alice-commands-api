package ru.appforsale.alicecommands.api.domain

import kotlinx.serialization.Serializable

@Serializable
data class ContentBundle(
    val schema_version: Int = 1,
    val content_version: Int = 0,
    val published_at: String,
    val min_app_version: String = "1.0",
    val categories: List<Category> = emptyList(),
    val commands: List<Command> = emptyList(),
    val scenario_templates: List<ScenarioTemplate> = emptyList(),
    val checklist_items: List<ChecklistItem> = emptyList(),
)

@Serializable
data class Category(
    val id: String,
    val title_ru: String,
    val title_kk: String? = null,
    val sort_order: Int,
    val featured: Boolean = false,
    val icon_key: String? = null,
    val description_ru: String? = null,
    val source_url: String,
    val device_types: List<String> = emptyList(),
)

@Serializable
data class Command(
    val id: String,
    val category_id: String,
    val title_ru: String,
    val phrases: List<String>,
    val effect_description_ru: String,
    val requires_alice_word: Boolean,
    val requires_plus: Boolean = false,
    val device_types: List<String> = emptyList(),
    val related_command_ids: List<String> = emptyList(),
    val source_url: String,
    val published_at: String? = null,
    val updated_at: String,
    val tags: List<String> = emptyList(),
)

@Serializable
data class ScenarioTemplate(
    val id: String,
    val title_ru: String,
    val trigger_ru: String? = null,
    val actions_ru: List<String> = emptyList(),
    val example_phrases: List<String> = emptyList(),
    val audience: String? = null,
    val deep_link_hint: String? = null,
    val source_url: String,
)

@Serializable
data class ChecklistItem(
    val id: String,
    val order: Int,
    val command_id: String,
    val hint_ru: String? = null,
)

@Serializable
data class AffiliateProduct(
    val title_ru: String,
    val market_url: String,
    val price_hint: String? = null,
)

@Serializable
data class AffiliateBlock(
    val id: String,
    val context_category_id: String? = null,
    val title_ru: String,
    val erid: String? = null,
    val advertiser_name: String? = null,
    val products: List<AffiliateProduct> = emptyList(),
)

@Serializable
data class AffiliateBlocksResponse(
    val schema_version: Int = 1,
    val updated_at: String,
    val blocks: List<AffiliateBlock>,
)

@Serializable
data class ManifestResponse(
    val schema_version: Int,
    val content_version: Int,
    val published_at: String,
    val min_app_version: String,
    val bundle_url: String,
    val bundle_sha256: String,
    val backup_url: String,
    val bundle_size_bytes: Long,
)

@Serializable
data class CurrentManifest(
    val contentVersion: Int,
    val bundlePath: String,
    val bundleSha256: String,
    val publishedAt: String,
    val minAppVersion: String,
    val schemaVersion: Int,
    val bundleSizeBytes: Long,
)

@Serializable
data class PublishHistoryEntry(
    val id: Long,
    val contentVersion: Int,
    val bundleSha256: String,
    val adminUsername: String,
    val publishedAt: String,
    val notes: String? = null,
)

@Serializable
data class ApiError(
    val error: String,
    val message: String,
    val details: List<String> = emptyList(),
)

@Serializable
data class DraftStats(
    val categoriesCount: Int,
    val commandsCount: Int,
    val scenarioTemplatesCount: Int,
    val checklistItemsCount: Int,
    val affiliateBlocksCount: Int,
)

@Serializable
data class PublishResult(
    val contentVersion: Int,
    val bundleSha256: String,
    val publishedAt: String,
)

class ValidationException(val errors: List<String>) : Exception(errors.joinToString("; "))
