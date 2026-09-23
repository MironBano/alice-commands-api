package ru.appforsale.alicecommands.api.infrastructure.persistence

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.appforsale.alicecommands.api.domain.push.PushFrequentCommandDto
import ru.appforsale.alicecommands.api.domain.push.PushPreferencesRequest
import ru.appforsale.alicecommands.api.domain.push.PushRegisterRequest
import ru.appforsale.alicecommands.api.domain.push.PushTokenRecord
import ru.appforsale.alicecommands.api.domain.ports.PushTokenRepository
import java.time.Instant
import java.time.OffsetDateTime

class ExposedPushTokenRepository(
    private val database: Database,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : PushTokenRepository {

    override fun upsertRegister(request: PushRegisterRequest) {
        transaction(database) {
            val exists = PushTokensTable.selectAll()
                .where { PushTokensTable.installId eq request.installId }
                .count() > 0
            val now = OffsetDateTime.now()
            val parsedInstallAt = parseAppInstalledAt(request.appInstalledAt)
            if (exists) {
                val existingInstallAt = PushTokensTable.selectAll()
                    .where { PushTokensTable.installId eq request.installId }
                    .firstOrNull()
                    ?.get(PushTokensTable.appInstalledAt)
                PushTokensTable.update({ PushTokensTable.installId eq request.installId }) {
                    it[rustoreToken] = request.rustoreToken
                    it[timezone] = request.timezone.ifBlank { "Europe/Moscow" }
                    it[persona] = request.persona
                    it[contentVersion] = request.contentVersion
                    it[masterEnabled] = request.masterEnabled
                    it[codEnabled] = request.codEnabled
                    it[codReminderTime] = request.codReminderTime.ifBlank { "09:00" }
                    it[checklistCompletedCount] = request.checklistCompletedCount
                    it[frequentCommandsJson] = json.encodeToString(request.frequentCommands)
                    it[appVersion] = request.appVersion
                    if (existingInstallAt == null && parsedInstallAt != null) {
                        it[appInstalledAt] = parsedInstallAt
                    }
                    it[updatedAt] = now
                }
            } else {
                PushTokensTable.insert {
                    it[installId] = request.installId
                    it[rustoreToken] = request.rustoreToken
                    it[timezone] = request.timezone.ifBlank { "Europe/Moscow" }
                    it[persona] = request.persona
                    it[contentVersion] = request.contentVersion
                    it[masterEnabled] = request.masterEnabled
                    it[codEnabled] = request.codEnabled
                    it[codReminderTime] = request.codReminderTime.ifBlank { "09:00" }
                    it[checklistCompletedCount] = request.checklistCompletedCount
                    it[frequentCommandsJson] = json.encodeToString(request.frequentCommands)
                    it[appVersion] = request.appVersion
                    it[appInstalledAt] = parsedInstallAt ?: now
                    it[s4Sent] = false
                    it[s5Sent] = false
                    it[lastNotifiedContentVersion] = 0
                    it[pushesThisWeek] = 0
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }
    }

    override fun updatePreferences(request: PushPreferencesRequest): Boolean =
        transaction(database) {
            val updated = PushTokensTable.update({ PushTokensTable.installId eq request.installId }) {
                it[masterEnabled] = request.masterEnabled
                it[codEnabled] = request.codEnabled
                it[codReminderTime] = request.codReminderTime
                it[updatedAt] = OffsetDateTime.now()
            }
            updated > 0
        }

    override fun delete(installId: String) {
        transaction(database) {
            PushTokensTable.deleteWhere { PushTokensTable.installId eq installId }
        }
    }

    override fun deleteIfTokenMatches(installId: String, rustoreToken: String): Boolean =
        transaction(database) {
            val row = PushTokensTable.selectAll()
                .where { PushTokensTable.installId eq installId }
                .firstOrNull() ?: return@transaction false
            if (row[PushTokensTable.rustoreToken] != rustoreToken) return@transaction false
            PushTokensTable.deleteWhere { PushTokensTable.installId eq installId }
            true
        }

    override fun listActive(): List<PushTokenRecord> = transaction(database) {
        PushTokensTable.selectAll()
            .where { PushTokensTable.masterEnabled eq true }
            .map { row ->
                val freqJson = row[PushTokensTable.frequentCommandsJson]
                val frequent = runCatching {
                    json.decodeFromString<List<PushFrequentCommandDto>>(freqJson)
                }.getOrDefault(emptyList())
                PushTokenRecord(
                    installId = row[PushTokensTable.installId],
                    rustoreToken = row[PushTokensTable.rustoreToken],
                    timezone = row[PushTokensTable.timezone],
                    persona = row[PushTokensTable.persona],
                    contentVersion = row[PushTokensTable.contentVersion],
                    masterEnabled = row[PushTokensTable.masterEnabled],
                    codEnabled = row[PushTokensTable.codEnabled],
                    codReminderTime = row[PushTokensTable.codReminderTime],
                    checklistCompletedCount = row[PushTokensTable.checklistCompletedCount],
                    frequentCommands = frequent,
                    frequentCommandsJson = freqJson,
                    appVersion = row[PushTokensTable.appVersion],
                    lastS1At = row[PushTokensTable.lastS1At],
                    lastS3At = row[PushTokensTable.lastS3At],
                    lastS6At = row[PushTokensTable.lastS6At],
                    s4Sent = row[PushTokensTable.s4Sent],
                    s5Sent = row[PushTokensTable.s5Sent],
                    lastPopularHash = row[PushTokensTable.lastPopularHash],
                    lastNotifiedContentVersion = row[PushTokensTable.lastNotifiedContentVersion],
                    lastPushAt = row[PushTokensTable.lastPushAt],
                    pushesThisWeek = row[PushTokensTable.pushesThisWeek],
                    weekBucket = row[PushTokensTable.weekBucket],
                    createdAt = row[PushTokensTable.createdAt],
                    appInstalledAt = row[PushTokensTable.appInstalledAt],
                )
            }
    }

    override fun markSent(
        installId: String,
        scenario: String,
        now: OffsetDateTime,
        weekBucket: String,
        popularHash: String?,
        notifiedContentVersion: Int?,
    ) {
        transaction(database) {
            val current = PushTokensTable.selectAll()
                .where { PushTokensTable.installId eq installId }
                .firstOrNull() ?: return@transaction
            val sameWeek = current[PushTokensTable.weekBucket] == weekBucket
            val nextWeekCount = if (sameWeek) current[PushTokensTable.pushesThisWeek] + 1 else 1
            val weekBucketValue = weekBucket
            PushTokensTable.update({ PushTokensTable.installId eq installId }) {
                it[lastPushAt] = now
                it[PushTokensTable.weekBucket] = weekBucketValue
                it[pushesThisWeek] = nextWeekCount
                it[updatedAt] = now
                when (scenario) {
                    "s1" -> {
                        it[lastS1At] = now
                        if (notifiedContentVersion != null) {
                            it[lastNotifiedContentVersion] = notifiedContentVersion
                        }
                    }
                    "s3" -> {
                        it[lastS3At] = now
                        if (popularHash != null) it[lastPopularHash] = popularHash
                    }
                    "s4" -> it[s4Sent] = true
                    "s5" -> it[s5Sent] = true
                    "s6" -> it[lastS6At] = now
                }
            }
        }
    }

    private fun parseAppInstalledAt(raw: String?): OffsetDateTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(raw) }
            .recoverCatching { Instant.parse(raw).atOffset(java.time.ZoneOffset.UTC) }
            .getOrNull()
    }

    override fun <T> withExclusiveCampaignLock(block: () -> T): T = transaction(database) {
        val conn = connection.connection as java.sql.Connection
        conn.prepareStatement("SELECT pg_advisory_xact_lock(?)").use { ps ->
            ps.setLong(1, PUSH_CAMPAIGN_ADVISORY_LOCK_KEY)
            ps.execute()
        }
        block()
    }

    companion object {
        /** Stable advisory lock id for push campaign worker (multi-instance deploy). */
        private const val PUSH_CAMPAIGN_ADVISORY_LOCK_KEY = 8_427_635_902L
    }
}
