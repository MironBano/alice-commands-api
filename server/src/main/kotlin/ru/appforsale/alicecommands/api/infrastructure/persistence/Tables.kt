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
    val descriptionRu = text("description_ru").nullable()
    val sourceUrl = text("source_url")
    val deviceTypes = array<String>("device_types")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object CommandsTable : Table("commands") {
    val id = text("id")
    val categoryId = text("category_id").references(CategoriesTable.id)
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
