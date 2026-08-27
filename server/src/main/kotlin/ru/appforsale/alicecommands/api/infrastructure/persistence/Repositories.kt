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
import ru.appforsale.alicecommands.api.domain.DeviceGuide
import ru.appforsale.alicecommands.api.domain.DevicePick
import ru.appforsale.alicecommands.api.domain.AffiliateProduct
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.ChecklistItem
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.application.publish.CommandOfDayBundleBuilder
import ru.appforsale.alicecommands.api.application.publish.CommandOfDayResolver
import ru.appforsale.alicecommands.api.application.publish.CommandOfDayValidationUseCase
import ru.appforsale.alicecommands.api.domain.CommandOfDaySettings
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
            val commands = listCommandsInternal()
            ensureCommandOfDaySettingsInternal()
            val settings = getCommandOfDaySettingsInternal()
            val commandOfDay = settings?.let { CommandOfDayBundleBuilder.build(it, commands) }
            ContentBundle(
                schema_version = 2,
                content_version = contentVersion,
                published_at = Instant.now().toIsoString(),
                min_app_version = minAppVersion,
                categories = listCategoriesInternal(),
                command_groups = listCommandGroupsInternal(),
                commands = commands,
                scenario_templates = listScenarioTemplatesInternal(),
                checklist_items = listChecklistItemsInternal(),
                command_of_day = commandOfDay,
            )
        }

    override fun stats(): DraftStats = transaction(database) {
        DraftStats(
            categoriesCount = CategoriesTable.selectAll().count().toInt(),
            commandGroupsCount = CommandGroupsTable.selectAll().count().toInt(),
            commandsCount = CommandsTable.selectAll().count().toInt(),
            scenarioTemplatesCount = ScenarioTemplatesTable.selectAll().count().toInt(),
            checklistItemsCount = ChecklistItemsTable.selectAll().count().toInt(),
            affiliateBlocksCount = AffiliateBlocksTable.selectAll().count().toInt(),
            deviceGuidesCount = DeviceGuidesTable.selectAll().count().toInt(),
            devicePicksCount = DevicePicksTable.selectAll().count().toInt(),
        )
    }

    override fun listCategories(): List<Category> = transaction(database) { listCategoriesInternal() }

    private fun listCategoriesInternal(): List<Category> =
        CategoriesTable.selectAll()
            .orderBy(CategoriesTable.sortOrder to SortOrder.ASC)
            .map { it.toCategory() }

    override fun getCategory(id: String): Category? = transaction(database) {
        CategoriesTable.selectAll().where { CategoriesTable.id eq id }
            .map { it.toCategory() }.singleOrNull()
    }

    override fun createCategory(category: Category) {
        unitTx {
        CategoriesTable.insert { it.fromCategory(category) }
        }
    }

    override fun updateCategory(category: Category) {
        unitTx {
        CategoriesTable.update({ CategoriesTable.id eq category.id }) {
            it.fromCategory(category)
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

    override fun listCommandGroups(categoryId: String?): List<CommandGroup> =
        transaction(database) { listCommandGroupsInternal(categoryId) }

    private fun listCommandGroupsInternal(categoryId: String? = null): List<CommandGroup> {
        val query = if (categoryId != null) {
            CommandGroupsTable.selectAll().where { CommandGroupsTable.categoryId eq categoryId }
        } else {
            CommandGroupsTable.selectAll()
        }
        return query.orderBy(CommandGroupsTable.sortOrder to SortOrder.ASC).map { it.toCommandGroup() }
    }

    override fun getCommandGroup(id: String): CommandGroup? = transaction(database) {
        CommandGroupsTable.selectAll().where { CommandGroupsTable.id eq id }
            .map { it.toCommandGroup() }.singleOrNull()
    }

    override fun createCommandGroup(group: CommandGroup) {
        unitTx {
            CommandGroupsTable.insert { it.fromCommandGroup(group) }
        }
    }

    override fun updateCommandGroup(group: CommandGroup) {
        unitTx {
            CommandGroupsTable.update({ CommandGroupsTable.id eq group.id }) {
                it.fromCommandGroup(group)
            }
        }
    }

    override fun deleteCommandGroup(id: String) {
        unitTx { CommandGroupsTable.deleteWhere { CommandGroupsTable.id eq id } }
    }

    override fun reorderCommandGroups(orderedIds: List<String>) {
        unitTx {
            orderedIds.forEachIndexed { index, groupId ->
                CommandGroupsTable.update({ CommandGroupsTable.id eq groupId }) {
                    it[sortOrder] = index + 1
                }
            }
        }
    }

    override fun bulkAssignCommandsToGroup(commandIds: List<String>, groupId: String?) {
        unitTx {
            val baseSort = if (groupId == null) {
                0
            } else {
                CommandsTable.selectAll()
                    .where { CommandsTable.groupId eq groupId }
                    .maxOfOrNull { it[CommandsTable.sortOrder] ?: 0 } ?: 0
            }
            commandIds.forEachIndexed { index, commandId ->
                CommandsTable.update({ CommandsTable.id eq commandId }) {
                    it[CommandsTable.groupId] = groupId
                    if (groupId != null) {
                        it[CommandsTable.sortOrder] = baseSort + (index + 1) * 10
                        it[CommandsTable.isPrimaryInGroup] = false
                    }
                    it[updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
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
        return query.orderBy(
            CommandsTable.groupId to SortOrder.ASC_NULLS_LAST,
            CommandsTable.sortOrder to SortOrder.ASC_NULLS_LAST,
            CommandsTable.titleRu to SortOrder.ASC,
        ).map { row -> row.toCommand() }
    }

    private fun getCommandOfDaySettingsInternal(): CommandOfDaySettings? =
        CommandOfDaySettingsTable.selectAll()
            .where { CommandOfDaySettingsTable.id eq 1 }
            .map { it.toCommandOfDaySettings() }
            .singleOrNull()

    private fun ensureCommandOfDaySettingsInternal() {
        if (getCommandOfDaySettingsInternal() != null) return
        val categories = listCategoriesInternal()
        val commands = listCommandsInternal()
        val categoryId = categories.firstOrNull { it.id == "music" }?.id
            ?: categories.firstOrNull { it.featured }?.id
            ?: categories.firstOrNull()?.id
            ?: return
        val pool = CommandOfDayResolver.buildPool(commands, categoryId)
        if (pool.isEmpty()) return
        val commandId = CommandOfDayResolver.resolveCommandId(pool, CommandOfDayResolver.todayMoscow())
        val settings = CommandOfDaySettings(
            mode = CommandOfDayValidationUseCase.MODE_AUTO,
            command_id = commandId,
            auto_category_id = categoryId,
            auto_seed = CommandOfDayResolver.DEFAULT_SEED,
            updated_at = Instant.now().toIsoString(),
        )
        CommandOfDaySettingsTable.insert {
            it[id] = 1
            it.fromCommandOfDaySettings(settings)
        }
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

    override fun listDeviceGuides(): List<DeviceGuide> = transaction(database) {
        DeviceGuidesTable.selectAll()
            .orderBy(DeviceGuidesTable.sortOrder to SortOrder.ASC)
            .map { it.toDeviceGuide() }
    }

    override fun getDeviceGuide(id: String): DeviceGuide? = transaction(database) {
        DeviceGuidesTable.selectAll().where { DeviceGuidesTable.id eq id }
            .map { it.toDeviceGuide() }.singleOrNull()
    }

    override fun createDeviceGuide(guide: DeviceGuide) {
        unitTx { DeviceGuidesTable.insert { it.fromDeviceGuide(guide) } }
    }

    override fun updateDeviceGuide(guide: DeviceGuide) {
        unitTx {
            DeviceGuidesTable.update({ DeviceGuidesTable.id eq guide.id }) {
                it.fromDeviceGuide(guide)
            }
        }
    }

    override fun deleteDeviceGuide(id: String) {
        unitTx { DeviceGuidesTable.deleteWhere { DeviceGuidesTable.id eq id } }
    }

    override fun listDevicePicks(): List<DevicePick> = transaction(database) {
        DevicePicksTable.selectAll()
            .orderBy(DevicePicksTable.sortOrder to SortOrder.ASC)
            .map { it.toDevicePick() }
    }

    override fun getDevicePick(id: String): DevicePick? = transaction(database) {
        DevicePicksTable.selectAll().where { DevicePicksTable.id eq id }
            .map { it.toDevicePick() }.singleOrNull()
    }

    override fun createDevicePick(pick: DevicePick) {
        unitTx { DevicePicksTable.insert { it.fromDevicePick(pick) } }
    }

    override fun updateDevicePick(pick: DevicePick) {
        unitTx {
            DevicePicksTable.update({ DevicePicksTable.id eq pick.id }) {
                it.fromDevicePick(pick)
            }
        }
    }

    override fun deleteDevicePick(id: String) {
        unitTx { DevicePicksTable.deleteWhere { DevicePicksTable.id eq id } }
    }

    override fun getCommandOfDaySettings(): CommandOfDaySettings? =
        transaction(database) {
            ensureCommandOfDaySettingsInternal()
            getCommandOfDaySettingsInternal()
        }

    override fun upsertCommandOfDaySettings(settings: CommandOfDaySettings) {
        unitTx {
            val existing = getCommandOfDaySettingsInternal()
            if (existing == null) {
                CommandOfDaySettingsTable.insert {
                    it[id] = 1
                    it.fromCommandOfDaySettings(settings)
                }
            } else {
                CommandOfDaySettingsTable.update({ CommandOfDaySettingsTable.id eq 1 }) {
                    it.fromCommandOfDaySettings(settings)
                }
            }
        }
    }

    override fun replaceAll(bundle: ContentBundle) {
        unitTx {
        CommandOfDaySettingsTable.deleteWhere { CommandOfDaySettingsTable.id eq 1 }
        ChecklistItemsTable.deleteAll()
        CommandsTable.deleteAll()
        CommandGroupsTable.deleteAll()
        ScenarioTemplatesTable.deleteAll()
        CategoriesTable.deleteAll()
        bundle.categories.forEach { createCategoryInternal(it) }
        bundle.command_groups.forEach { createCommandGroupInternal(it) }
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
        ensureCommandOfDaySettingsInternal()
        }
    }

    override fun merge(bundle: ContentBundle) {
        unitTx {
        bundle.categories.forEach { cat ->
            if (getCategoryInternal(cat.id) == null) createCategoryInternal(cat)
            else updateCategoryInternal(cat)
        }
        bundle.command_groups.forEach { group ->
            if (getCommandGroupInternal(group.id) == null) createCommandGroupInternal(group)
            else updateCommandGroupInternal(group)
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
        CategoriesTable.insert { it.fromCategory(category) }
    }

    private fun updateCategoryInternal(category: Category) {
        CategoriesTable.update({ CategoriesTable.id eq category.id }) {
            it.fromCategory(category)
        }
    }

    private fun getCategoryInternal(id: String): Category? =
        CategoriesTable.selectAll().where { CategoriesTable.id eq id }
            .map { it.toCategory() }.singleOrNull()

    private fun getCommandGroupInternal(id: String): CommandGroup? =
        CommandGroupsTable.selectAll().where { CommandGroupsTable.id eq id }
            .map { it.toCommandGroup() }.singleOrNull()

    private fun createCommandGroupInternal(group: CommandGroup) {
        CommandGroupsTable.insert { it.fromCommandGroup(group) }
    }

    private fun updateCommandGroupInternal(group: CommandGroup) {
        CommandGroupsTable.update({ CommandGroupsTable.id eq group.id }) {
            it.fromCommandGroup(group)
        }
    }

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

private fun org.jetbrains.exposed.sql.ResultRow.toCategory(): Category = Category(
    id = this[CategoriesTable.id],
    title_ru = this[CategoriesTable.titleRu],
    title_kk = this[CategoriesTable.titleKk],
    sort_order = this[CategoriesTable.sortOrder],
    featured = this[CategoriesTable.featured],
    icon_key = this[CategoriesTable.iconKey],
    icon_url = this[CategoriesTable.iconUrl],
    accent_color = this[CategoriesTable.accentColor],
    accent_color_dark = this[CategoriesTable.accentColorDark],
    description_ru = this[CategoriesTable.descriptionRu],
    source_url = this[CategoriesTable.sourceUrl],
    device_types = this[CategoriesTable.deviceTypes].toList(),
)

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromCategory(category: Category) {
    this[CategoriesTable.id] = category.id
    this[CategoriesTable.titleRu] = category.title_ru
    this[CategoriesTable.titleKk] = category.title_kk
    this[CategoriesTable.sortOrder] = category.sort_order
    this[CategoriesTable.featured] = category.featured
    this[CategoriesTable.iconKey] = category.icon_key
    this[CategoriesTable.iconUrl] = category.icon_url
    this[CategoriesTable.accentColor] = category.accent_color
    this[CategoriesTable.accentColorDark] = category.accent_color_dark
    this[CategoriesTable.descriptionRu] = category.description_ru
    this[CategoriesTable.sourceUrl] = category.source_url
    this[CategoriesTable.deviceTypes] = category.device_types
    this[CategoriesTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
}

private fun org.jetbrains.exposed.sql.ResultRow.toCommandGroup(): CommandGroup = CommandGroup(
    id = this[CommandGroupsTable.id],
    category_id = this[CommandGroupsTable.categoryId],
    title_ru = this[CommandGroupsTable.titleRu],
    sort_order = this[CommandGroupsTable.sortOrder],
    description_ru = this[CommandGroupsTable.descriptionRu],
    icon_key = this[CommandGroupsTable.iconKey],
    icon_url = this[CommandGroupsTable.iconUrl],
    accent_color = this[CommandGroupsTable.accentColor],
    accent_color_dark = this[CommandGroupsTable.accentColorDark],
    featured = this[CommandGroupsTable.featured],
    preview_command_ids = this[CommandGroupsTable.previewCommandIds].toList(),
)

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromCommandGroup(group: CommandGroup) {
    this[CommandGroupsTable.id] = group.id
    this[CommandGroupsTable.categoryId] = group.category_id
    this[CommandGroupsTable.titleRu] = group.title_ru
    this[CommandGroupsTable.descriptionRu] = group.description_ru
    this[CommandGroupsTable.sortOrder] = group.sort_order
    this[CommandGroupsTable.iconKey] = group.icon_key
    this[CommandGroupsTable.iconUrl] = group.icon_url
    this[CommandGroupsTable.accentColor] = group.accent_color
    this[CommandGroupsTable.accentColorDark] = group.accent_color_dark
    this[CommandGroupsTable.featured] = group.featured
    this[CommandGroupsTable.previewCommandIds] = group.preview_command_ids
    this[CommandGroupsTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
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
    group_id = this[CommandsTable.groupId],
    sort_order = this[CommandsTable.sortOrder],
    variant_label_ru = this[CommandsTable.variantLabelRu],
    is_primary_in_group = this[CommandsTable.isPrimaryInGroup],
    search_aliases = this[CommandsTable.searchAliases].toList(),
)

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromCommand(command: Command) {
    this[CommandsTable.id] = command.id
    this[CommandsTable.categoryId] = command.category_id
    this[CommandsTable.groupId] = command.group_id
    this[CommandsTable.sortOrder] = command.sort_order
    this[CommandsTable.variantLabelRu] = command.variant_label_ru
    this[CommandsTable.isPrimaryInGroup] = command.is_primary_in_group
    this[CommandsTable.searchAliases] = command.search_aliases
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

private fun org.jetbrains.exposed.sql.ResultRow.toDeviceGuide(): DeviceGuide = DeviceGuide(
    id = this[DeviceGuidesTable.id],
    title_ru = this[DeviceGuidesTable.titleRu],
    summary_ru = this[DeviceGuidesTable.summaryRu],
    capabilities_ru = this[DeviceGuidesTable.capabilitiesRu],
    setup_ru = this[DeviceGuidesTable.setupRu],
    setup_steps_ru = this[DeviceGuidesTable.setupStepsRu].toList(),
    related_devices_ru = this[DeviceGuidesTable.relatedDevicesRu],
    related_device_ids = this[DeviceGuidesTable.relatedDeviceIds].toList(),
    command_device_filter_id = this[DeviceGuidesTable.commandDeviceFilterId],
    image_url = this[DeviceGuidesTable.imageUrl],
    action_url = this[DeviceGuidesTable.actionUrl],
    sort_order = this[DeviceGuidesTable.sortOrder],
)

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromDeviceGuide(guide: DeviceGuide) {
    this[DeviceGuidesTable.id] = guide.id
    this[DeviceGuidesTable.titleRu] = guide.title_ru
    this[DeviceGuidesTable.summaryRu] = guide.summary_ru
    this[DeviceGuidesTable.capabilitiesRu] = guide.capabilities_ru
    this[DeviceGuidesTable.setupRu] = guide.setup_ru
    this[DeviceGuidesTable.setupStepsRu] = guide.setup_steps_ru
    this[DeviceGuidesTable.relatedDevicesRu] = guide.related_devices_ru
    this[DeviceGuidesTable.relatedDeviceIds] = guide.related_device_ids
    this[DeviceGuidesTable.commandDeviceFilterId] = guide.command_device_filter_id
    this[DeviceGuidesTable.imageUrl] = guide.image_url
    this[DeviceGuidesTable.actionUrl] = guide.action_url
    this[DeviceGuidesTable.sortOrder] = guide.sort_order
    this[DeviceGuidesTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
}

private fun org.jetbrains.exposed.sql.ResultRow.toDevicePick(): DevicePick = DevicePick(
    id = this[DevicePicksTable.id],
    title_ru = this[DevicePicksTable.titleRu],
    description_ru = this[DevicePicksTable.descriptionRu],
    price_hint_ru = this[DevicePicksTable.priceHintRu],
    image_url = this[DevicePicksTable.imageUrl],
    action_url = this[DevicePicksTable.actionUrl],
    sort_order = this[DevicePicksTable.sortOrder],
    erid = this[DevicePicksTable.erid],
    advertiser_name = this[DevicePicksTable.advertiserName],
    disclosure_ru = this[DevicePicksTable.disclosureRu],
    cta_ru = this[DevicePicksTable.ctaRu],
    tags = this[DevicePicksTable.tags].toList(),
    device_types = this[DevicePicksTable.deviceTypes].toList(),
    category_ids = this[DevicePicksTable.categoryIds].toList(),
    command_group_ids = this[DevicePicksTable.commandGroupIds].toList(),
    command_ids = this[DevicePicksTable.commandIds].toList(),
    scenario_template_ids = this[DevicePicksTable.scenarioTemplateIds].toList(),
    guide_ids = this[DevicePicksTable.guideIds].toList(),
    placements = this[DevicePicksTable.placements].toList(),
    priority = this[DevicePicksTable.priority],
    starts_at = this[DevicePicksTable.startsAt]?.toIsoString(),
    ends_at = this[DevicePicksTable.endsAt]?.toIsoString(),
    max_impressions_per_session = this[DevicePicksTable.maxImpressionsPerSession],
)

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromDevicePick(pick: DevicePick) {
    this[DevicePicksTable.id] = pick.id
    this[DevicePicksTable.titleRu] = pick.title_ru
    this[DevicePicksTable.descriptionRu] = pick.description_ru
    this[DevicePicksTable.priceHintRu] = pick.price_hint_ru
    this[DevicePicksTable.imageUrl] = pick.image_url
    this[DevicePicksTable.actionUrl] = pick.action_url
    this[DevicePicksTable.sortOrder] = pick.sort_order
    this[DevicePicksTable.erid] = pick.erid
    this[DevicePicksTable.advertiserName] = pick.advertiser_name
    this[DevicePicksTable.disclosureRu] = pick.disclosure_ru
    this[DevicePicksTable.ctaRu] = pick.cta_ru
    this[DevicePicksTable.tags] = pick.tags
    this[DevicePicksTable.deviceTypes] = pick.device_types
    this[DevicePicksTable.categoryIds] = pick.category_ids
    this[DevicePicksTable.commandGroupIds] = pick.command_group_ids
    this[DevicePicksTable.commandIds] = pick.command_ids
    this[DevicePicksTable.scenarioTemplateIds] = pick.scenario_template_ids
    this[DevicePicksTable.guideIds] = pick.guide_ids
    this[DevicePicksTable.placements] = pick.placements
    this[DevicePicksTable.priority] = pick.priority
    this[DevicePicksTable.startsAt] = pick.starts_at?.let { OffsetDateTime.parse(it) }
    this[DevicePicksTable.endsAt] = pick.ends_at?.let { OffsetDateTime.parse(it) }
    this[DevicePicksTable.maxImpressionsPerSession] = pick.max_impressions_per_session
    this[DevicePicksTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
}

private fun org.jetbrains.exposed.sql.ResultRow.toCommandOfDaySettings(): CommandOfDaySettings =
    CommandOfDaySettings(
        mode = this[CommandOfDaySettingsTable.mode],
        command_id = this[CommandOfDaySettingsTable.commandId],
        auto_category_id = this[CommandOfDaySettingsTable.autoCategoryId],
        auto_seed = this[CommandOfDaySettingsTable.autoSeed],
        updated_at = this[CommandOfDaySettingsTable.updatedAt].toIsoString(),
        updated_by = this[CommandOfDaySettingsTable.updatedBy],
    )

private fun org.jetbrains.exposed.sql.statements.UpdateBuilder<*>.fromCommandOfDaySettings(settings: CommandOfDaySettings) {
    this[CommandOfDaySettingsTable.mode] = settings.mode
    this[CommandOfDaySettingsTable.commandId] = settings.command_id
    this[CommandOfDaySettingsTable.autoCategoryId] = settings.auto_category_id
    this[CommandOfDaySettingsTable.autoSeed] = settings.auto_seed
    this[CommandOfDaySettingsTable.updatedAt] = Instant.parse(settings.updated_at).atOffset(ZoneOffset.UTC)
    this[CommandOfDaySettingsTable.updatedBy] = settings.updated_by
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
