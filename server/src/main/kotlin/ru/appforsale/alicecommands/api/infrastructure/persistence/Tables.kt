package ru.appforsale.alicecommands.api.infrastructure.persistence

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import org.jetbrains.exposed.sql.json.jsonb
import ru.appforsale.alicecommands.api.domain.AffiliateProduct

private val persistenceJson = Json { ignoreUnknownKeys = true }

object CategoriesTable : Table("categories") {
    val id = text("id")
    val titleRu = text("title_ru")
    val titleKk = text("title_kk").nullable()
    val sortOrder = integer("sort_order")
    val featured = bool("featured")
    val iconKey = text("icon_key").nullable()
    val iconUrl = text("icon_url").nullable()
    val accentColor = varchar("accent_color", 7).nullable()
    val accentColorDark = varchar("accent_color_dark", 7).nullable()
    val descriptionRu = text("description_ru").nullable()
    val sourceUrl = text("source_url")
    val deviceTypes = array<String>("device_types")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object CommandGroupsTable : Table("command_groups") {
    val id = text("id")
    val categoryId = text("category_id").references(CategoriesTable.id)
    val titleRu = text("title_ru")
    val descriptionRu = text("description_ru").nullable()
    val sortOrder = integer("sort_order")
    val iconKey = text("icon_key").nullable()
    val iconUrl = text("icon_url").nullable()
    val accentColor = varchar("accent_color", 7).nullable()
    val accentColorDark = varchar("accent_color_dark", 7).nullable()
    val featured = bool("featured")
    val previewCommandIds = array<String>("preview_command_ids")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object CommandsTable : Table("commands") {
    val id = text("id")
    val categoryId = text("category_id").references(CategoriesTable.id)
    val groupId = text("group_id").references(CommandGroupsTable.id).nullable()
    val sortOrder = integer("sort_order").nullable()
    val variantLabelRu = text("variant_label_ru").nullable()
    val isPrimaryInGroup = bool("is_primary_in_group")
    val searchAliases = array<String>("search_aliases")
    val titleRu = text("title_ru")
    val phrases = jsonb("phrases", persistenceJson, ListSerializer(String.serializer()))
    val effectDescriptionRu = text("effect_description_ru")
    val requiresAliceWord = bool("requires_alice_word")
    val requiresPlus = bool("requires_plus")
    val deviceTypes = array<String>("device_types")
    val relatedCommandIds = array<String>("related_command_ids")
    val sourceUrl = text("source_url")
    val publishedAt = timestampWithTimeZone("published_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
    val tags = array<String>("tags")
    override val primaryKey = PrimaryKey(id)
}

object ScenarioTemplatesTable : Table("scenario_templates") {
    val id = text("id")
    val titleRu = text("title_ru")
    val triggerRu = text("trigger_ru").nullable()
    val actionsRu = jsonb("actions_ru", persistenceJson, ListSerializer(String.serializer()))
    val examplePhrases = jsonb("example_phrases", persistenceJson, ListSerializer(String.serializer()))
    val audience = text("audience").nullable()
    val deepLinkHint = text("deep_link_hint").nullable()
    val sourceUrl = text("source_url")
    override val primaryKey = PrimaryKey(id)
}

object ChecklistItemsTable : Table("checklist_items") {
    val id = text("id")
    val itemOrder = integer("item_order")
    val commandId = text("command_id").references(CommandsTable.id)
    val hintRu = text("hint_ru").nullable()
    override val primaryKey = PrimaryKey(id)
}

object AffiliateBlocksTable : Table("affiliate_blocks") {
    val id = text("id")
    val contextCategoryId = text("context_category_id").nullable()
    val titleRu = text("title_ru")
    val erid = text("erid").nullable()
    val advertiserName = text("advertiser_name").nullable()
    val products = jsonb("products", persistenceJson, ListSerializer(AffiliateProduct.serializer()))
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object DeviceGuidesTable : Table("device_guides") {
    val id = text("id")
    val titleRu = text("title_ru")
    val summaryRu = text("summary_ru")
    val capabilitiesRu = text("capabilities_ru")
    val setupRu = text("setup_ru")
    val setupStepsRu = array<String>("setup_steps_ru")
    val relatedDevicesRu = text("related_devices_ru").nullable()
    val relatedDeviceIds = array<String>("related_device_ids")
    val commandDeviceFilterId = text("command_device_filter_id").nullable()
    val imageUrl = text("image_url").nullable()
    val actionUrl = text("action_url")
    val sortOrder = integer("sort_order")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object DevicePicksTable : Table("device_picks") {
    val id = text("id")
    val titleRu = text("title_ru")
    val descriptionRu = text("description_ru").nullable()
    val priceHintRu = text("price_hint_ru").nullable()
    val imageUrl = text("image_url").nullable()
    val actionUrl = text("action_url")
    val sortOrder = integer("sort_order")
    val erid = text("erid").nullable()
    val advertiserName = text("advertiser_name").nullable()
    val disclosureRu = text("disclosure_ru").nullable()
    val ctaRu = text("cta_ru").nullable()
    val tags = array<String>("tags")
    val deviceTypes = array<String>("device_types")
    val categoryIds = array<String>("category_ids")
    val commandGroupIds = array<String>("command_group_ids")
    val commandIds = array<String>("command_ids")
    val scenarioTemplateIds = array<String>("scenario_template_ids")
    val guideIds = array<String>("guide_ids")
    val placements = array<String>("placements")
    val priority = integer("priority")
    val startsAt = timestampWithTimeZone("starts_at").nullable()
    val endsAt = timestampWithTimeZone("ends_at").nullable()
    val maxImpressionsPerSession = integer("max_impressions_per_session").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object CurrentManifestTable : Table("current_manifest") {
    val contentVersion = integer("content_version")
    val bundlePath = text("bundle_path")
    val bundleSha256 = text("bundle_sha256")
    val publishedAt = timestampWithTimeZone("published_at")
    val minAppVersion = text("min_app_version")
    val schemaVersion = integer("schema_version")
    val bundleSizeBytes = long("bundle_size_bytes")
    override val primaryKey = PrimaryKey(contentVersion)
}

object PublishHistoryTable : Table("publish_history") {
    val id = long("id").autoIncrement()
    val contentVersion = integer("content_version")
    val bundleSha256 = text("bundle_sha256")
    val adminUsername = text("admin_username")
    val publishedAt = timestampWithTimeZone("published_at")
    val notes = text("notes").nullable()
    override val primaryKey = PrimaryKey(id)
}

object AdminSessionsTable : Table("admin_sessions") {
    val id = text("id")
    val expiresAt = timestampWithTimeZone("expires_at")
    override val primaryKey = PrimaryKey(id)
}

object LoginAttemptsTable : Table("login_attempts") {
    val ipAddress = text("ip_address")
    val attemptedAt = timestampWithTimeZone("attempted_at")
}

object InventoryItemsTable : Table("inventory_items") {
    val commandId = text("command_id")
    val categoryId = text("category_id")
    val phrases = jsonb("phrases", persistenceJson, ListSerializer(String.serializer()))
    val rawResult = text("raw_result").nullable()
    val sourceUrl = text("source_url")
    val section = text("section").nullable()
    val requiresAliceWord = bool("requires_alice_word")
    val requiresPlus = bool("requires_plus")
    val deviceTypes = array<String>("device_types")
    val sourceId = text("source_id").nullable()
    val lastSeenAt = timestampWithTimeZone("last_seen_at")
    val deprecated = bool("deprecated")
    override val primaryKey = PrimaryKey(commandId)
}

object EditorialRecordsTable : Table("editorial_records") {
    val commandId = text("command_id")
    val categoryId = text("category_id")
    val titleRu = text("title_ru")
    val effectDescriptionRu = text("effect_description_ru")
    val status = text("status")
    val approvedAt = timestampWithTimeZone("approved_at").nullable()
    val notes = text("notes").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(commandId)
}

object ContentQueueTable : Table("content_queue") {
    val id = text("id")
    val eventType = text("event_type")
    val commandId = text("command_id")
    val phrase = text("phrase").nullable()
    val categoryId = text("category_id").nullable()
    val titleRu = text("title_ru").nullable()
    val suggestedEffect = text("suggested_effect").nullable()
    val rawResult = text("raw_result").nullable()
    val sourceUrl = text("source_url").nullable()
    val status = text("status")
    val createdAt = timestampWithTimeZone("created_at")
    val resolvedAt = timestampWithTimeZone("resolved_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object UserFeedbackTable : Table("user_feedback") {
    val id = text("id")
    val message = text("message")
    val rating = integer("rating").nullable()
    val appVersion = text("app_version").nullable()
    val platform = text("platform").nullable()
    val locale = text("locale").nullable()
    val contentVersion = integer("content_version").nullable()
    val deviceModel = text("device_model").nullable()
    val clientIp = text("client_ip")
    val status = text("status")
    val createdAt = timestampWithTimeZone("created_at")
    val resolvedAt = timestampWithTimeZone("resolved_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object CommandReportsTable : Table("command_reports") {
    val id = text("id")
    val commandId = text("command_id")
    val issueType = text("issue_type")
    val message = text("message").nullable()
    val contentVersion = integer("content_version").nullable()
    val categoryId = text("category_id").nullable()
    val commandTitle = text("command_title").nullable()
    val phraseUsed = text("phrase_used").nullable()
    val appVersion = text("app_version").nullable()
    val platform = text("platform").nullable()
    val locale = text("locale").nullable()
    val commandExistsCurrent = bool("command_exists_current")
    val clientIp = text("client_ip")
    val status = text("status")
    val createdAt = timestampWithTimeZone("created_at")
    val resolvedAt = timestampWithTimeZone("resolved_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

object PublicSubmissionAttemptsTable : Table("public_submission_attempts") {
    val ipAddress = text("ip_address")
    val attemptedAt = timestampWithTimeZone("attempted_at")
}

object AnalyticsEventsTable : Table("analytics_events") {
    val eventId = text("event_id")
    val installId = text("install_id")
    val sessionId = text("session_id")
    val eventName = text("event_name")
    val occurredAt = timestampWithTimeZone("occurred_at")
    val receivedAt = timestampWithTimeZone("received_at")
    val appVersion = text("app_version").nullable()
    val androidVersion = text("android_version").nullable()
    val locale = text("locale").nullable()
    val userProperties = jsonb(
        "user_properties",
        persistenceJson,
        kotlinx.serialization.builtins.MapSerializer(String.serializer(), String.serializer()),
    )
    val params = jsonb(
        "params",
        persistenceJson,
        kotlinx.serialization.builtins.MapSerializer(String.serializer(), String.serializer()),
    )
    val clientIp = text("client_ip").nullable()
    override val primaryKey = PrimaryKey(eventId)
}

object AnalyticsRequestAttemptsTable : Table("analytics_request_attempts") {
    val ipAddress = text("ip_address")
    val attemptedAt = timestampWithTimeZone("attempted_at")
}

object CommandOfDaySettingsTable : Table("command_of_day_settings") {
    val id = integer("id")
    val mode = varchar("mode", 16)
    val commandId = text("command_id").references(CommandsTable.id)
    val autoCategoryId = text("auto_category_id").references(CategoriesTable.id).nullable()
    val autoSeed = integer("auto_seed")
    val updatedAt = timestampWithTimeZone("updated_at")
    val updatedBy = text("updated_by").nullable()
    override val primaryKey = PrimaryKey(id)
}
