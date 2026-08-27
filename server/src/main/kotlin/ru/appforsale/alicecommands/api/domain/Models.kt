package ru.appforsale.alicecommands.api.domain

import kotlinx.serialization.Serializable

@Serializable
data class ContentBundle(
    val schema_version: Int = 1,
    val content_version: Int = 0,
    val published_at: String,
    val min_app_version: String = "1.0",
    val categories: List<Category> = emptyList(),
    val command_groups: List<CommandGroup> = emptyList(),
    val commands: List<Command> = emptyList(),
    val scenario_templates: List<ScenarioTemplate> = emptyList(),
    val checklist_items: List<ChecklistItem> = emptyList(),
    val command_of_day: CommandOfDay? = null,
)

@Serializable
data class CommandOfDay(
    val mode: String,
    val command_id: String,
    val auto_category_id: String? = null,
    val auto_seed: Int = 31,
    val resolved_date: String,
    val updated_at: String,
)

@Serializable
data class CommandOfDaySettings(
    val mode: String,
    val command_id: String,
    val auto_category_id: String? = null,
    val auto_seed: Int = 31,
    val updated_at: String,
    val updated_by: String? = null,
)

@Serializable
data class UpdateCommandOfDayRequest(
    val mode: String,
    val command_id: String? = null,
    val auto_category_id: String? = null,
    val auto_seed: Int? = null,
)

@Serializable
data class CommandOfDayPreview(
    val date: String,
    val command_id: String,
    val title_ru: String? = null,
    val phrase: String? = null,
    val category_id: String? = null,
)

@Serializable
data class CommandOfDayAdminResponse(
    val settings: CommandOfDaySettings,
    val preview_today: CommandOfDayPreview? = null,
    val pool_size: Int? = null,
    val has_unpublished_changes: Boolean = false,
    val live_content_version: Int? = null,
    val live_command_of_day: CommandOfDay? = null,
)

@Serializable
data class CommandGroup(
    val id: String,
    val category_id: String,
    val title_ru: String,
    val sort_order: Int,
    val description_ru: String? = null,
    val icon_key: String? = null,
    val icon_url: String? = null,
    val accent_color: String? = null,
    val accent_color_dark: String? = null,
    val featured: Boolean = false,
    val preview_command_ids: List<String> = emptyList(),
)

@Serializable
data class Category(
    val id: String,
    val title_ru: String,
    val title_kk: String? = null,
    val sort_order: Int,
    val featured: Boolean = false,
    val icon_key: String? = null,
    val icon_url: String? = null,
    val accent_color: String? = null,
    val accent_color_dark: String? = null,
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
    val group_id: String? = null,
    val sort_order: Int? = null,
    val variant_label_ru: String? = null,
    val is_primary_in_group: Boolean = false,
    val search_aliases: List<String> = emptyList(),
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
data class DeviceGuide(
    val id: String,
    val title_ru: String,
    val summary_ru: String,
    val capabilities_ru: String,
    val setup_ru: String,
    val setup_steps_ru: List<String> = emptyList(),
    val related_devices_ru: String? = null,
    val related_device_ids: List<String> = emptyList(),
    val command_device_filter_id: String? = null,
    val image_url: String? = null,
    val action_url: String,
    val sort_order: Int,
    /** Primary `pick_{id}` + related picks for device guide detail; computed at publish. */
    val detail_referral_pick_ids: List<String> = emptyList(),
)

@Serializable
data class DevicePick(
    val id: String,
    val title_ru: String,
    val description_ru: String? = null,
    val price_hint_ru: String? = null,
    val image_url: String? = null,
    val action_url: String,
    val sort_order: Int,
    val erid: String? = null,
    val advertiser_name: String? = null,
    val disclosure_ru: String? = null,
    val cta_ru: String? = null,
    val tags: List<String> = emptyList(),
    val device_types: List<String> = emptyList(),
    val category_ids: List<String> = emptyList(),
    val command_group_ids: List<String> = emptyList(),
    val command_ids: List<String> = emptyList(),
    val scenario_template_ids: List<String> = emptyList(),
    val guide_ids: List<String> = emptyList(),
    val placements: List<String> = emptyList(),
    val priority: Int = 0,
    val starts_at: String? = null,
    val ends_at: String? = null,
    val max_impressions_per_session: Int? = null,
)

@Serializable
data class SmartHomeDevicesResponse(
    val schema_version: Int = 1,
    val updated_at: String,
    val guides: List<DeviceGuide>,
    val picks: List<DevicePick>,
)

@Serializable
data class UploadDeviceImageRequest(
    val slug: String,
    val image_base64: String,
    val content_type: String? = null,
)

@Serializable
data class UploadDeviceImageResponse(
    val slug: String,
    val image_url: String,
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
    val commandGroupsCount: Int,
    val commandsCount: Int,
    val scenarioTemplatesCount: Int,
    val checklistItemsCount: Int,
    val affiliateBlocksCount: Int,
    val deviceGuidesCount: Int,
    val devicePicksCount: Int,
)

@Serializable
data class EntityDeltaSection<T>(
    val added: List<T> = emptyList(),
    val updated: List<T> = emptyList(),
    val removed: List<String> = emptyList(),
)

@Serializable
data class ContentDeltaResponse(
    val from_version: Int,
    val to_version: Int,
    val schema_version: Int,
    val published_at: String,
    val full_bundle_required: Boolean = false,
    val categories: EntityDeltaSection<Category> = EntityDeltaSection(),
    val command_groups: EntityDeltaSection<CommandGroup> = EntityDeltaSection(),
    val commands: EntityDeltaSection<Command> = EntityDeltaSection(),
    val scenario_templates: EntityDeltaSection<ScenarioTemplate> = EntityDeltaSection(),
    val checklist_items: EntityDeltaSection<ChecklistItem> = EntityDeltaSection(),
    val command_of_day: CommandOfDay? = null,
)

@Serializable
data class ContentValidationWarnings(
    val orphan_commands: List<String> = emptyList(),
    val empty_groups: List<String> = emptyList(),
    val duplicate_alias_commands: List<String> = emptyList(),
    val missing_sort_order_commands: List<String> = emptyList(),
    val icon_url_without_icon_key: List<String> = emptyList(),
    val low_contrast_visuals: List<String> = emptyList(),
    val command_of_day_auto_pool_small: Boolean = false,
    val command_of_day_manual_pinned_long: Boolean = false,
)

@Serializable
data class IconCatalogEntry(
    val slug: String,
    val label_ru: String,
    val url: String = "",
)

@Serializable
data class AccentColorPreset(
    val name: String,
    val light: String,
    val dark: String,
)

@Serializable
data class IconCatalog(
    val icons: List<IconCatalogEntry> = emptyList(),
    val accent_presets: List<AccentColorPreset> = emptyList(),
    val public_base_url: String = "",
)

@Serializable
data class UploadIconRequest(
    val slug: String? = null,
    val svg: String,
)

@Serializable
data class UploadIconResponse(
    val slug: String,
    val icon_url: String,
    val icon_key: String,
)

@Serializable
data class BulkAssignGroupRequest(
    val command_ids: List<String>,
    val group_id: String?,
)

@Serializable
data class PublishResult(
    val contentVersion: Int,
    val bundleSha256: String,
    val publishedAt: String,
)

@Serializable
data class InventoryItemRecord(
    val command_id: String,
    val category_id: String,
    val phrases: List<String>,
    val raw_result: String? = null,
    val source_url: String,
    val section: String? = null,
    val requires_alice_word: Boolean = true,
    val requires_plus: Boolean = false,
    val device_types: List<String> = emptyList(),
    val source_id: String? = null,
    val last_seen_at: String? = null,
    val deprecated: Boolean = false,
)

@Serializable
data class EditorialRecordDto(
    val command_id: String,
    val category_id: String,
    val title_ru: String,
    val effect_description_ru: String,
    val status: String = "pending",
    val approved_at: String? = null,
    val notes: String? = null,
    val updated_at: String? = null,
)

@Serializable
data class ContentQueueItemDto(
    val id: String,
    val event_type: String,
    val command_id: String,
    val phrase: String? = null,
    val category_id: String? = null,
    val title_ru: String? = null,
    val suggested_effect: String? = null,
    val raw_result: String? = null,
    val source_url: String? = null,
    val status: String = "open",
    val created_at: String? = null,
)

@Serializable
data class PipelineSyncPayload(
    val inventory: List<InventoryItemRecord> = emptyList(),
    val editorial: List<EditorialRecordDto> = emptyList(),
    val queue: List<ContentQueueItemDto> = emptyList(),
)

@Serializable
data class PipelineStatusResponse(
    val inventory_count: Int,
    val editorial_approved: Int,
    val editorial_pending: Int,
    val open_queue: Int,
    val catalog_commands: Int,
)

@Serializable
data class QueueActionRequest(
    val title_ru: String? = null,
    val effect_description_ru: String? = null,
)

@Serializable
data class EditorialTextSnapshot(
    val title_ru: String,
    val effect_description_ru: String,
)

@Serializable
data class EditorialEditFields(
    val command_id: String = "",
    val title_ru: String,
    val effect_description_ru: String,
    val status: String = "approved",
)

@Serializable
data class EditorialReviewRecord(
    val command_id: String,
    val category_id: String,
    val phrase_example: String? = null,
    val phrases: List<String> = emptyList(),
    val raw_result: String? = null,
    val source_url: String? = null,
    val published: EditorialTextSnapshot? = null,
    val draft: EditorialTextSnapshot? = null,
    val edit: EditorialEditFields,
    val reasons: List<String> = emptyList(),
    val queue_events: List<String> = emptyList(),
)

@Serializable
data class EditorialReviewResponse(
    val filter: String,
    val total: Int,
    val records: List<EditorialReviewRecord>,
)

@Serializable
data class EditorialExportDocument(
    val schema_version: Int = 1,
    val exported_at: String,
    val filter: String,
    val instructions: String,
    val records: List<EditorialReviewRecord>,
)

@Serializable
data class EditorialImportResult(
    val updated: Int,
    val draft_rebuilt: Int,
)

@Serializable
data class EditorialBatchSaveRequest(
    val records: List<EditorialEditFields>,
)

class ValidationException(val errors: List<String>) : Exception(errors.joinToString("; "))

object FeedbackStatus {
    const val OPEN = "open"
    const val RESOLVED = "resolved"
    const val DISMISSED = "dismissed"
}

object CommandReportIssueType {
    const val WRONG_EFFECT = "wrong_effect"
    const val OUTDATED = "outdated"
    const val PHRASE_NOT_WORKING = "phrase_not_working"
    const val REQUIRES_PLUS_WRONG = "requires_plus_wrong"
    const val WRONG_DEVICE = "wrong_device"
    const val OTHER = "other"

    val ALL = setOf(
        WRONG_EFFECT,
        OUTDATED,
        PHRASE_NOT_WORKING,
        REQUIRES_PLUS_WRONG,
        WRONG_DEVICE,
        OTHER,
    )
}

@Serializable
data class SubmitFeedbackRequest(
    val message: String,
    val rating: Int? = null,
    val app_version: String? = null,
    val platform: String? = null,
    val locale: String? = null,
    val content_version: Int? = null,
    val device_model: String? = null,
)

@Serializable
data class SubmitFeedbackResponse(
    val id: String,
    val status: String = FeedbackStatus.OPEN,
)

@Serializable
data class ReportCommandIssueRequest(
    val issue_type: String,
    val message: String? = null,
    val content_version: Int? = null,
    val category_id: String? = null,
    val command_title: String? = null,
    val phrase_used: String? = null,
    val app_version: String? = null,
    val platform: String? = null,
    val locale: String? = null,
)

@Serializable
data class ReportCommandIssueResponse(
    val id: String,
    val status: String = FeedbackStatus.OPEN,
    val command_exists_current: Boolean = false,
)

@Serializable
data class UserFeedbackDto(
    val id: String,
    val message: String,
    val rating: Int? = null,
    val app_version: String? = null,
    val platform: String? = null,
    val locale: String? = null,
    val content_version: Int? = null,
    val device_model: String? = null,
    val status: String = FeedbackStatus.OPEN,
    val created_at: String,
    val resolved_at: String? = null,
)

@Serializable
data class CommandReportDto(
    val id: String,
    val command_id: String,
    val issue_type: String,
    val message: String? = null,
    val content_version: Int? = null,
    val category_id: String? = null,
    val command_title: String? = null,
    val phrase_used: String? = null,
    val app_version: String? = null,
    val platform: String? = null,
    val locale: String? = null,
    val command_exists_current: Boolean = false,
    val status: String = FeedbackStatus.OPEN,
    val created_at: String,
    val resolved_at: String? = null,
)

@Serializable
data class FeedbackInboxCounts(
    val open_feedback: Int = 0,
    val open_command_reports: Int = 0,
)

@Serializable
data class AnalyticsBatchRequest(
    val events: List<AnalyticsEventDto>,
)

@Serializable
data class AnalyticsEventDto(
    val installId: String,
    val sessionId: String,
    val eventId: String,
    val eventName: String,
    val occurredAt: Long,
    val appVersion: String? = null,
    val androidVersion: String? = null,
    val locale: String? = null,
    val userProperties: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap(),
)

@Serializable
data class AnalyticsBatchResponse(
    val accepted: Int,
    val duplicates: Int,
    val rejected: Int,
    /** Event IDs rejected by validation (PII / schema). Backward-compatible optional field. */
    val rejectedEventIds: List<String> = emptyList(),
)

@Serializable
data class AnalyticsEventAdminDto(
    val event_id: String,
    val install_id: String,
    val session_id: String,
    val event_name: String,
    val occurred_at: String,
    val app_version: String? = null,
    val android_version: String? = null,
    val locale: String? = null,
    val user_properties: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap(),
)

@Serializable
data class AnalyticsTopEventDto(
    val event_name: String,
    val count: Int,
)

@Serializable
data class AnalyticsDailyPointDto(
    val date: String,
    val events: Int,
    val dau: Int,
    /** Raw COUNT(DISTINCT install_id) with any event that day (not new installs). */
    val unique_installs: Int,
    /**
     * Installs whose first ever event (Europe/Moscow calendar day) falls on this date.
     * Use this for the “new installs” trend — not [unique_installs].
     */
    val new_installs: Int = 0,
)

@Serializable
data class AnalyticsSummaryResponse(
    val from: String,
    val to: String,
    val daily_active_installs: Int,
    val total_events: Int,
    /** Distinct canonical install ids (dominant install per session_id). Dedups client race ghosts. */
    val unique_installs: Int,
    val top_events: List<AnalyticsTopEventDto>,
    /** Raw COUNT(DISTINCT install_id) before session-canonical dedup. */
    val raw_unique_installs: Int = unique_installs,
    /** Sum of [AnalyticsDailyPointDto.new_installs] over the period (convenience aggregate). */
    val new_installs: Int = 0,
    /** Per-day series for trend chart (gaps filled with zeros). Calendar days in Europe/Moscow. */
    val daily: List<AnalyticsDailyPointDto> = emptyList(),
    /** Average of daily[].dau over days_in_range (0 if empty). */
    val avg_dau: Double = 0.0,
    /** Number of inclusive calendar days (= daily.size). */
    val days_in_range: Int = daily.size,
)

@Serializable
data class AnalyticsFunnelStepDto(
    val event_name: String,
    val installs: Int,
    /** Percent vs previous step; null for the first step. */
    val conversion_from_previous: Double? = null,
    /** Percent vs first step; null for the first step. */
    val conversion_from_first: Double? = null,
)

@Serializable
data class AnalyticsFunnelResponse(
    val from: String,
    val to: String,
    val steps: List<AnalyticsFunnelStepDto>,
)

@Serializable
data class AnalyticsBreakdownItemDto(
    val value: String,
    val count: Int,
)

@Serializable
data class AnalyticsBreakdownResponse(
    val from: String,
    val to: String,
    val event_name: String,
    val param: String,
    /** `params` (default) or `user_properties`. */
    val field_source: String = "params",
    val items: List<AnalyticsBreakdownItemDto>,
)

@Serializable
data class AnalyticsEventsListResponse(
    val items: List<AnalyticsEventAdminDto>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)
