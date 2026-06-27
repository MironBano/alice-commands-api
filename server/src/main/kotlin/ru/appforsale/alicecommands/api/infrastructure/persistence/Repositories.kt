package ru.appforsale.alicecommands.api.infrastructure.persistence

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.flywaydb.core.Flyway
import ru.appforsale.alicecommands.api.config.AppConfig
import ru.appforsale.alicecommands.api.domain.AffiliateBlock
import ru.appforsale.alicecommands.api.domain.AffiliateProduct
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.ChecklistItem
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.CurrentManifest
import ru.appforsale.alicecommands.api.domain.DraftStats
import ru.appforsale.alicecommands.api.domain.PublishHistoryEntry
import ru.appforsale.alicecommands.api.domain.ScenarioTemplate
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun initDatabase(config: AppConfig): Database {
    Flyway.configure()
        .dataSource(config.databaseUrl, config.databaseUser, config.databasePassword)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    return Database.connect(
        url = config.databaseUrl,
        driver = "org.postgresql.Driver",
        user = config.databaseUser,
        password = config.databasePassword,
    )
}

private fun OffsetDateTime.toIsoString(): String =
    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this)

private fun Instant.toIsoString(): String =
    DateTimeFormatter.ISO_INSTANT.format(this)

class ExposedDraftRepository(private val database: Database) : DraftRepository {

    private inline fun unitTx(crossinline block: org.jetbrains.exposed.sql.Transaction.() -> Unit) {
        transaction(database) { block() }
    }
    override fun loadFull(contentVersion: Int, minAppVersion: String): ContentBundle =
        transaction(database) {
            ContentBundle(
                schema_version = 1,
                content_version = contentVersion,
                published_at = Instant.now().toIsoString(),
                min_app_version = minAppVersion,
                categories = listCategoriesInternal(),
                commands = listCommandsInternal(),
                scenario_templates = listScenarioTemplatesInternal(),
                checklist_items = listChecklistItemsInternal(),
            )
        }

    override fun stats(): DraftStats = transaction(database) {
        DraftStats(
            categoriesCount = CategoriesTable.selectAll().count().toInt(),
            commandsCount = CommandsTable.selectAll().count().toInt(),
            scenarioTemplatesCount = ScenarioTemplatesTable.selectAll().count().toInt(),
            checklistItemsCount = ChecklistItemsTable.selectAll().count().toInt(),
            affiliateBlocksCount = AffiliateBlocksTable.selectAll().count().toInt(),
        )
    }

    override fun listCategories(): List<Category> = transaction(database) { listCategoriesInternal() }

    private fun listCategoriesInternal(): List<Category> =
        CategoriesTable.selectAll()
            .orderBy(CategoriesTable.sortOrder to SortOrder.ASC)
            .map { row ->
                Category(
                    id = row[CategoriesTable.id],
                    title_ru = row[CategoriesTable.titleRu],
                    title_kk = row[CategoriesTable.titleKk],
                    sort_order = row[CategoriesTable.sortOrder],
                    featured = row[CategoriesTable.featured],
                    icon_key = row[CategoriesTable.iconKey],
                    description_ru = row[CategoriesTable.descriptionRu],
                    source_url = row[CategoriesTable.sourceUrl],
                    device_types = row[CategoriesTable.deviceTypes].toList(),
                )
            }

    override fun getCategory(id: String): Category? = transaction(database) {
        CategoriesTable.selectAll().where { CategoriesTable.id eq id }
            .map { row ->
                Category(
                    id = row[CategoriesTable.id],
                    title_ru = row[CategoriesTable.titleRu],
                    title_kk = row[CategoriesTable.titleKk],
                    sort_order = row[CategoriesTable.sortOrder],
                    featured = row[CategoriesTable.featured],
                    icon_key = row[CategoriesTable.iconKey],
                    description_ru = row[CategoriesTable.descriptionRu],
                    source_url = row[CategoriesTable.sourceUrl],
                    device_types = row[CategoriesTable.deviceTypes].toList(),
                )
            }.singleOrNull()
    }

    override fun createCategory(category: Category) {
        unitTx {
        CategoriesTable.insert {
            it[id] = category.id
            it[titleRu] = category.title_ru
            it[titleKk] = category.title_kk
            it[sortOrder] = category.sort_order
            it[featured] = category.featured
            it[iconKey] = category.icon_key
            it[descriptionRu] = category.description_ru
            it[sourceUrl] = category.source_url
            it[deviceTypes] = category.device_types
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        }
    }

    override fun updateCategory(category: Category) {
        unitTx {
        CategoriesTable.update({ CategoriesTable.id eq category.id }) {
            it[titleRu] = category.title_ru
            it[titleKk] = category.title_kk
            it[sortOrder] = category.sort_order
            it[featured] = category.featured
            it[iconKey] = category.icon_key
            it[descriptionRu] = category.description_ru
            it[sourceUrl] = category.source_url
            it[deviceTypes] = category.device_types
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
        }
    }

    override fun deleteCategory(id: String) {
        unitTx { CategoriesTable.deleteWhere { CategoriesTable.id eq id } }
    }

    override fun reorderCategories(orderedIds: List<String>) {
        unitTx {
        orderedIds.forEachIndexed { index, categoryId ->
            CategoriesTable.update({ CategoriesTable.id eq categoryId }) {
                it[sortOrder] = index + 1
            }
        }
        }
    }

    override fun listCommands(categoryId: String?): List<Command> =
        transaction(database) { listCommandsInternal(categoryId) }

    private fun listCommandsInternal(categoryId: String? = null): List<Command> {
        val query = if (categoryId != null) {
            CommandsTable.selectAll().where { CommandsTable.categoryId eq categoryId }
        } else {
            CommandsTable.selectAll()
        }
        return query.orderBy(CommandsTable.titleRu to SortOrder.ASC).map { row -> row.toCommand() }
    }

    override fun getCommand(id: String): Command? = transaction(database) {
        CommandsTable.selectAll().where { CommandsTable.id eq id }
            .map { it.toCommand() }.singleOrNull()
    }

    override fun createCommand(command: Command) {
        unitTx { CommandsTable.insert { it.fromCommand(command) } }
    }

    override fun updateCommand(command: Command) {
        unitTx {
        CommandsTable.update({ CommandsTable.id eq command.id }) {
            it.fromCommand(command)
        }
        }
    }

    override fun deleteCommand(id: String) {
        unitTx { CommandsTable.deleteWhere { CommandsTable.id eq id } }
    }

    override fun listScenarioTemplates(): List<ScenarioTemplate> =
        transaction(database) { listScenarioTemplatesInternal() }

    private fun listScenarioTemplatesInternal(): List<ScenarioTemplate> =
        ScenarioTemplatesTable.selectAll()
            .orderBy(ScenarioTemplatesTable.id to SortOrder.ASC)
            .map { it.toScenarioTemplate() }

    override fun getScenarioTemplate(id: String): ScenarioTemplate? = transaction(database) {
        ScenarioTemplatesTable.selectAll().where { ScenarioTemplatesTable.id eq id }
            .map { it.toScenarioTemplate() }.singleOrNull()
    }

    override fun createScenarioTemplate(template: ScenarioTemplate) {
        unitTx { ScenarioTemplatesTable.insert { it.fromScenarioTemplate(template) } }
    }

    override fun updateScenarioTemplate(template: ScenarioTemplate) {
        unitTx {
        ScenarioTemplatesTable.update({ ScenarioTemplatesTable.id eq template.id }) {
            it.fromScenarioTemplate(template)
        }
        }
    }

    override fun deleteScenarioTemplate(id: String) {
        unitTx { ScenarioTemplatesTable.deleteWhere { ScenarioTemplatesTable.id eq id } }
    }

    override fun listChecklistItems(): List<ChecklistItem> =
        transaction(database) { listChecklistItemsInternal() }

    private fun listChecklistItemsInternal(): List<ChecklistItem> =
        ChecklistItemsTable.selectAll()
            .orderBy(ChecklistItemsTable.itemOrder to SortOrder.ASC)
            .map { row ->
                ChecklistItem(
                    id = row[ChecklistItemsTable.id],
                    order = row[ChecklistItemsTable.itemOrder],
                    command_id = row[ChecklistItemsTable.commandId],
                    hint_ru = row[ChecklistItemsTable.hintRu],
                )
            }

    override fun updateChecklistItems(items: List<ChecklistItem>) {
        unitTx {
        ChecklistItemsTable.deleteAll()
        items.forEach { item ->
            ChecklistItemsTable.insert {
                it[id] = item.id
                it[itemOrder] = item.order
                it[commandId] = item.command_id
                it[hintRu] = item.hint_ru
            }
        }
        }
    }

    override fun listAffiliateBlocks(): List<AffiliateBlock> = transaction(database) {
        AffiliateBlocksTable.selectAll().map { it.toAffiliateBlock() }
    }

    override fun getAffiliateBlock(id: String): AffiliateBlock? = transaction(database) {
        AffiliateBlocksTable.selectAll().where { AffiliateBlocksTable.id eq id }
            .map { it.toAffiliateBlock() }.singleOrNull()
    }

    override fun createAffiliateBlock(block: AffiliateBlock) {
        unitTx { AffiliateBlocksTable.insert { it.fromAffiliateBlock(block) } }
    }

    override fun updateAffiliateBlock(block: AffiliateBlock) {
        unitTx {
        AffiliateBlocksTable.update({ AffiliateBlocksTable.id eq block.id }) {
            it.fromAffiliateBlock(block)
        }
        }
    }

    override fun deleteAffiliateBlock(id: String) {
        unitTx { AffiliateBlocksTable.deleteWhere { AffiliateBlocksTable.id eq id } }
    }

    override fun replaceAll(bundle: ContentBundle) {
        unitTx {
        ChecklistItemsTable.deleteAll()
        CommandsTable.deleteAll()
        ScenarioTemplatesTable.deleteAll()
        CategoriesTable.deleteAll()
        bundle.categories.forEach { createCategoryInternal(it) }
        bundle.commands.forEach { createCommandInternal(it) }
        bundle.scenario_templates.forEach { createScenarioTemplateInternal(it) }
        bundle.checklist_items.forEach { item ->
            ChecklistItemsTable.insert {
                it[id] = item.id
                it[itemOrder] = item.order
                it[commandId] = item.command_id
                it[hintRu] = item.hint_ru
            }
        }
        }
    }

    override fun merge(bundle: ContentBundle) {
        unitTx {
        bundle.categories.forEach { cat ->
            if (getCategoryInternal(cat.id) == null) createCategoryInternal(cat)
            else updateCategoryInternal(cat)
        }
        bundle.commands.forEach { cmd ->
            if (getCommandInternal(cmd.id) == null) createCommandInternal(cmd)
            else updateCommandInternal(cmd)
        }
        bundle.scenario_templates.forEach { tpl ->
            if (getScenarioTemplateInternal(tpl.id) == null) createScenarioTemplateInternal(tpl)
            else updateScenarioTemplateInternal(tpl)
        }
        bundle.checklist_items.forEach { item ->
            ChecklistItemsTable.deleteWhere { ChecklistItemsTable.id eq item.id }
            ChecklistItemsTable.insert {
                it[id] = item.id
                it[itemOrder] = item.order
                it[commandId] = item.command_id
                it[hintRu] = item.hint_ru
            }
        }
        }
    }

    private fun createCategoryInternal(category: Category) {
        CategoriesTable.insert {
            it[id] = category.id
            it[titleRu] = category.title_ru
            it[titleKk] = category.title_kk
            it[sortOrder] = category.sort_order
            it[featured] = category.featured
            it[iconKey] = category.icon_key
            it[descriptionRu] = category.description_ru
            it[sourceUrl] = category.source_url
            it[deviceTypes] = category.device_types
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }

    private fun updateCategoryInternal(category: Category) {
        CategoriesTable.update({ CategoriesTable.id eq category.id }) {
            it[titleRu] = category.title_ru
            it[titleKk] = category.title_kk
            it[sortOrder] = category.sort_order
            it[featured] = category.featured
            it[iconKey] = category.icon_key
            it[descriptionRu] = category.description_ru
            it[sourceUrl] = category.source_url
            it[deviceTypes] = category.device_types
            it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }

    private fun getCategoryInternal(id: String): Category? =
        CategoriesTable.selectAll().where { CategoriesTable.id eq id }
            .map { row ->
                Category(
                    id = row[CategoriesTable.id],
                    title_ru = row[CategoriesTable.titleRu],
                    title_kk = row[CategoriesTable.titleKk],
                    sort_order = row[CategoriesTable.sortOrder],
                    featured = row[CategoriesTable.featured],
                    icon_key = row[CategoriesTable.iconKey],
                    description_ru = row[CategoriesTable.descriptionRu],
                    source_url = row[CategoriesTable.sourceUrl],
                    device_types = row[CategoriesTable.deviceTypes].toList(),
                )
            }.singleOrNull()

    private fun createCommandInternal(command: Command) {
        CommandsTable.insert { it.fromCommand(command) }
    }

    private fun updateCommandInternal(command: Command) {
        CommandsTable.update({ CommandsTable.id eq command.id }) {
            it.fromCommand(command)
        }
    }

    private fun getCommandInternal(id: String): Command? =
        CommandsTable.selectAll().where { CommandsTable.id eq id }
            .map { it.toCommand() }.singleOrNull()

    private fun createScenarioTemplateInternal(template: ScenarioTemplate) {
        ScenarioTemplatesTable.insert { it.fromScenarioTemplate(template) }
    }

    private fun updateScenarioTemplateInternal(template: ScenarioTemplate) {
        ScenarioTemplatesTable.update({ ScenarioTemplatesTable.id eq template.id }) {
            it.fromScenarioTemplate(template)
        }
    }

    private fun getScenarioTemplateInternal(id: String): ScenarioTemplate? =
        ScenarioTemplatesTable.selectAll().where { ScenarioTemplatesTable.id eq id }
            .map { it.toScenarioTemplate() }.singleOrNull()
}

class ExposedManifestRepository(private val database: Database) : ManifestRepository {

    private inline fun unitTx(crossinline block: org.jetbrains.exposed.sql.Transaction.() -> Unit) {
        transaction(database) { block() }
    }
    override fun getCurrent(): CurrentManifest? = transaction(database) {
        CurrentManifestTable.selectAll()
            .orderBy(CurrentManifestTable.contentVersion to SortOrder.DESC)
            .limit(1)
            .map { row ->
                CurrentManifest(
                    contentVersion = row[CurrentManifestTable.contentVersion],
                    bundlePath = row[CurrentManifestTable.bundlePath],
                    bundleSha256 = row[CurrentManifestTable.bundleSha256],
                    publishedAt = row[CurrentManifestTable.publishedAt].toIsoString(),
                    minAppVersion = row[CurrentManifestTable.minAppVersion],
                    schemaVersion = row[CurrentManifestTable.schemaVersion],
                    bundleSizeBytes = row[CurrentManifestTable.bundleSizeBytes],
                )
            }.singleOrNull()
    }

    override fun nextVersion(): Int = transaction(database) {
        val current = CurrentManifestTable.selectAll()
            .orderBy(CurrentManifestTable.contentVersion to SortOrder.DESC)
            .limit(1)
            .singleOrNull()
        (current?.get(CurrentManifestTable.contentVersion) ?: 0) + 1
    }

    override fun update(manifest: CurrentManifest) {
        unitTx {
            val updated = CurrentManifestTable.update({ CurrentManifestTable.contentVersion greaterEq 0 }) {
                it[contentVersion] = manifest.contentVersion
                it[bundlePath] = manifest.bundlePath
                it[bundleSha256] = manifest.bundleSha256
                it[publishedAt] = OffsetDateTime.parse(manifest.publishedAt)
                it[minAppVersion] = manifest.minAppVersion
                it[schemaVersion] = manifest.schemaVersion
                it[bundleSizeBytes] = manifest.bundleSizeBytes
            }
            if (updated == 0) {
                CurrentManifestTable.insert {
                    it[contentVersion] = manifest.contentVersion
                    it[bundlePath] = manifest.bundlePath
                    it[bundleSha256] = manifest.bundleSha256
                    it[publishedAt] = OffsetDateTime.parse(manifest.publishedAt)
                    it[minAppVersion] = manifest.minAppVersion
                    it[schemaVersion] = manifest.schemaVersion
                    it[bundleSizeBytes] = manifest.bundleSizeBytes
                }
            }
        }
    }

    override fun listHistory(limit: Int): List<PublishHistoryEntry> = transaction(database) {
        PublishHistoryTable.selectAll()
            .orderBy(PublishHistoryTable.publishedAt to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                PublishHistoryEntry(
                    id = row[PublishHistoryTable.id],
                    contentVersion = row[PublishHistoryTable.contentVersion],
                    bundleSha256 = row[PublishHistoryTable.bundleSha256],
                    adminUsername = row[PublishHistoryTable.adminUsername],
                    publishedAt = row[PublishHistoryTable.publishedAt].toIsoString(),
                    notes = row[PublishHistoryTable.notes],
                )
            }
    }

    override fun insertHistory(entry: PublishHistoryEntry) {
        unitTx {
        PublishHistoryTable.insert {
            it[contentVersion] = entry.contentVersion
            it[bundleSha256] = entry.bundleSha256
            it[adminUsername] = entry.adminUsername
            it[publishedAt] = OffsetDateTime.parse(entry.publishedAt)
            it[notes] = entry.notes
        }
        }
    }

    override fun getHistoryByVersion(version: Int): PublishHistoryEntry? = transaction(database) {
        PublishHistoryTable.selectAll()
            .where { PublishHistoryTable.contentVersion eq version }
            .orderBy(PublishHistoryTable.publishedAt to SortOrder.DESC)
            .limit(1)
            .map { row ->
                PublishHistoryEntry(
                    id = row[PublishHistoryTable.id],
                    contentVersion = row[PublishHistoryTable.contentVersion],
                    bundleSha256 = row[PublishHistoryTable.bundleSha256],
                    adminUsername = row[PublishHistoryTable.adminUsername],
                    publishedAt = row[PublishHistoryTable.publishedAt].toIsoString(),
                    notes = row[PublishHistoryTable.notes],
                )
            }.singleOrNull()
    }
}

private fun org.jetbrains.exposed.sql.ResultRow.toCommand(): Command = Command(
    id = this[CommandsTable.id],
    category_id = this[CommandsTable.categoryId],
    title_ru = this[CommandsTable.titleRu],
    phrases = this[CommandsTable.phrases],
    effect_description_ru = this[CommandsTable.effectDescriptionRu],
    requires_alice_word = this[CommandsTable.requiresAliceWord],
    requires_plus = this[CommandsTable.requiresPlus],
    device_types = this[CommandsTable.deviceTypes].toList(),
    related_command_ids = this[CommandsTable.relatedCommandIds].toList(),
    source_url = this[CommandsTable.sourceUrl],
    published_at = this[CommandsTable.publishedAt]?.toIsoString(),
    updated_at = this[CommandsTable.updatedAt].toIsoString(),
    tags = this[CommandsTable.tags].toList(),
)

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromCommand(command: Command) {
    this[CommandsTable.id] = command.id
    this[CommandsTable.categoryId] = command.category_id
    this[CommandsTable.titleRu] = command.title_ru
    this[CommandsTable.phrases] = command.phrases
    this[CommandsTable.effectDescriptionRu] = command.effect_description_ru
    this[CommandsTable.requiresAliceWord] = command.requires_alice_word
    this[CommandsTable.requiresPlus] = command.requires_plus
    this[CommandsTable.deviceTypes] = command.device_types
    this[CommandsTable.relatedCommandIds] = command.related_command_ids
    this[CommandsTable.sourceUrl] = command.source_url
    this[CommandsTable.publishedAt] = command.published_at?.let { OffsetDateTime.parse(it) }
    this[CommandsTable.updatedAt] = OffsetDateTime.parse(command.updated_at)
    this[CommandsTable.tags] = command.tags
}

private fun org.jetbrains.exposed.sql.ResultRow.toScenarioTemplate(): ScenarioTemplate = ScenarioTemplate(
    id = this[ScenarioTemplatesTable.id],
    title_ru = this[ScenarioTemplatesTable.titleRu],
    trigger_ru = this[ScenarioTemplatesTable.triggerRu],
    actions_ru = this[ScenarioTemplatesTable.actionsRu],
    example_phrases = this[ScenarioTemplatesTable.examplePhrases],
    audience = this[ScenarioTemplatesTable.audience],
    deep_link_hint = this[ScenarioTemplatesTable.deepLinkHint],
    source_url = this[ScenarioTemplatesTable.sourceUrl],
)

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromScenarioTemplate(template: ScenarioTemplate) {
    this[ScenarioTemplatesTable.id] = template.id
    this[ScenarioTemplatesTable.titleRu] = template.title_ru
    this[ScenarioTemplatesTable.triggerRu] = template.trigger_ru
    this[ScenarioTemplatesTable.actionsRu] = template.actions_ru
    this[ScenarioTemplatesTable.examplePhrases] = template.example_phrases
    this[ScenarioTemplatesTable.audience] = template.audience
    this[ScenarioTemplatesTable.deepLinkHint] = template.deep_link_hint
    this[ScenarioTemplatesTable.sourceUrl] = template.source_url
}

private fun org.jetbrains.exposed.sql.ResultRow.toAffiliateBlock(): AffiliateBlock = AffiliateBlock(
    id = this[AffiliateBlocksTable.id],
    context_category_id = this[AffiliateBlocksTable.contextCategoryId],
    title_ru = this[AffiliateBlocksTable.titleRu],
    erid = this[AffiliateBlocksTable.erid],
    advertiser_name = this[AffiliateBlocksTable.advertiserName],
    products = this[AffiliateBlocksTable.products],
)

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromAffiliateBlock(block: AffiliateBlock) {
    this[AffiliateBlocksTable.id] = block.id
    this[AffiliateBlocksTable.contextCategoryId] = block.context_category_id
    this[AffiliateBlocksTable.titleRu] = block.title_ru
    this[AffiliateBlocksTable.erid] = block.erid
    this[AffiliateBlocksTable.advertiserName] = block.advertiser_name
    this[AffiliateBlocksTable.products] = block.products
    this[AffiliateBlocksTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
}

class ExposedSessionRepository(private val database: Database) :
    ru.appforsale.alicecommands.api.domain.ports.SessionRepository {

  private val sessionTtlHours = 24L

  private inline fun unitTx(crossinline block: org.jetbrains.exposed.sql.Transaction.() -> Unit) {
    transaction(database) { block() }
  }

  override fun createSession(): String = transaction(database) {
    val id = java.util.UUID.randomUUID().toString()
    AdminSessionsTable.insert {
      it[AdminSessionsTable.id] = id
      it[expiresAt] = OffsetDateTime.now(ZoneOffset.UTC).plusHours(sessionTtlHours)
    }
    id
  }

  override fun isValid(sessionId: String): Boolean = transaction(database) {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    AdminSessionsTable.selectAll()
      .where { (AdminSessionsTable.id eq sessionId) and (AdminSessionsTable.expiresAt greaterEq now) }
      .any()
  }

  override fun invalidate(sessionId: String) {
    unitTx { AdminSessionsTable.deleteWhere { AdminSessionsTable.id eq sessionId } }
  }

  override fun touch(sessionId: String) {
    unitTx {
    AdminSessionsTable.update({ AdminSessionsTable.id eq sessionId }) {
      it[expiresAt] = OffsetDateTime.now(ZoneOffset.UTC).plusHours(sessionTtlHours)
    }
    }
  }

  override fun cleanupExpired() {
    unitTx {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    AdminSessionsTable.deleteWhere { AdminSessionsTable.expiresAt less now }
    }
  }
}
