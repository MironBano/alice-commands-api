package ru.appforsale.alicecommands.api.domain.ports

import ru.appforsale.alicecommands.api.domain.AffiliateBlock
import ru.appforsale.alicecommands.api.domain.AffiliateBlocksResponse
import ru.appforsale.alicecommands.api.domain.DeviceGuide
import ru.appforsale.alicecommands.api.domain.DevicePick
import ru.appforsale.alicecommands.api.domain.SmartHomeDevicesResponse
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.ChecklistItem
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.CommandOfDaySettings
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.CurrentManifest
import ru.appforsale.alicecommands.api.domain.DraftStats
import ru.appforsale.alicecommands.api.domain.PublishHistoryEntry
import ru.appforsale.alicecommands.api.domain.ScenarioTemplate

interface DraftRepository {
    fun loadFull(contentVersion: Int = 0, minAppVersion: String = "1.0"): ContentBundle
    fun stats(): DraftStats
    fun listCategories(): List<Category>
    fun getCategory(id: String): Category?
    fun createCategory(category: Category)
    fun updateCategory(category: Category)
    fun deleteCategory(id: String)
    fun reorderCategories(orderedIds: List<String>)
    fun listCommandGroups(categoryId: String? = null): List<CommandGroup>
    fun getCommandGroup(id: String): CommandGroup?
    fun createCommandGroup(group: CommandGroup)
    fun updateCommandGroup(group: CommandGroup)
    fun deleteCommandGroup(id: String)
    fun reorderCommandGroups(orderedIds: List<String>)
    fun bulkAssignCommandsToGroup(commandIds: List<String>, groupId: String?)
    fun listCommands(categoryId: String? = null): List<Command>
    fun getCommand(id: String): Command?
    fun createCommand(command: Command)
    fun updateCommand(command: Command)
    fun deleteCommand(id: String)
    fun listScenarioTemplates(): List<ScenarioTemplate>
    fun getScenarioTemplate(id: String): ScenarioTemplate?
    fun createScenarioTemplate(template: ScenarioTemplate)
    fun updateScenarioTemplate(template: ScenarioTemplate)
    fun deleteScenarioTemplate(id: String)
    fun listChecklistItems(): List<ChecklistItem>
    fun updateChecklistItems(items: List<ChecklistItem>)
    fun listAffiliateBlocks(): List<AffiliateBlock>
    fun getAffiliateBlock(id: String): AffiliateBlock?
    fun createAffiliateBlock(block: AffiliateBlock)
    fun updateAffiliateBlock(block: AffiliateBlock)
    fun deleteAffiliateBlock(id: String)
    fun listDeviceGuides(): List<DeviceGuide> = emptyList()
    fun getDeviceGuide(id: String): DeviceGuide? = null
    fun createDeviceGuide(guide: DeviceGuide) = Unit
    fun updateDeviceGuide(guide: DeviceGuide) = Unit
    fun deleteDeviceGuide(id: String) = Unit
    fun listDevicePicks(): List<DevicePick> = emptyList()
    fun getDevicePick(id: String): DevicePick? = null
    fun createDevicePick(pick: DevicePick) = Unit
    fun updateDevicePick(pick: DevicePick) = Unit
    fun deleteDevicePick(id: String) = Unit
    fun replaceAll(bundle: ContentBundle)
    fun merge(bundle: ContentBundle)
    fun getCommandOfDaySettings(): CommandOfDaySettings?
    fun upsertCommandOfDaySettings(settings: CommandOfDaySettings)
}

interface ManifestRepository {
    fun getCurrent(): CurrentManifest?
    fun nextVersion(): Int
    fun update(manifest: CurrentManifest)
    fun listHistory(limit: Int = 5): List<PublishHistoryEntry>
    fun insertHistory(entry: PublishHistoryEntry)
    fun getHistoryByVersion(version: Int): PublishHistoryEntry?
}

interface BundleStorage {
    fun write(filename: String, gzipBytes: ByteArray): String
    fun read(filename: String): ByteArray?
    fun exists(filename: String): Boolean
    fun isWritable(): Boolean
    fun pruneOldBundles(retention: Int)
    fun writeAffiliate(jsonBytes: ByteArray)
    fun readAffiliate(): AffiliateBlocksResponse?
    fun writeSmartHomeDevices(jsonBytes: ByteArray) = Unit
    fun readSmartHomeDevices(): SmartHomeDevicesResponse? = null
}

interface SmartHomeDevicesSchemaValidator {
    fun validate(response: SmartHomeDevicesResponse)
    fun validateJson(json: String)
}

interface SchemaValidator {
    fun validate(bundle: ContentBundle)
    fun validateJson(json: String)
}

interface SessionRepository {
    fun createSession(): String
    fun isValid(sessionId: String): Boolean
    fun invalidate(sessionId: String)
    fun touch(sessionId: String)
    fun cleanupExpired()
}

interface LoginRateLimiter {
    fun isBlocked(ip: String): Boolean
    fun recordFailure(ip: String)
    fun clearFailures(ip: String)
}
