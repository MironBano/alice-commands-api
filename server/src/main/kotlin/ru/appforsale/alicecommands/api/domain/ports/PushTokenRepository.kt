package ru.appforsale.alicecommands.api.domain.ports

import ru.appforsale.alicecommands.api.domain.push.PushPreferencesRequest
import ru.appforsale.alicecommands.api.domain.push.PushRegisterRequest
import ru.appforsale.alicecommands.api.domain.push.PushRegisterResult
import ru.appforsale.alicecommands.api.domain.push.PushTokenRecord
import java.time.OffsetDateTime

interface PushTokenRepository {
    fun upsertRegister(request: PushRegisterRequest): PushRegisterResult
    fun updatePreferences(request: PushPreferencesRequest): Boolean
    fun delete(installId: String)
    /** Deletes only when stored rustoreToken matches [rustoreToken]. Returns true if deleted. */
    fun deleteIfTokenMatches(installId: String, rustoreToken: String): Boolean
    fun listActive(): List<PushTokenRecord>
    fun markSent(
        installId: String,
        scenario: String,
        now: OffsetDateTime,
        weekBucket: String,
        popularHash: String? = null,
        notifiedContentVersion: Int? = null,
    )
    fun markDeliveryBlocked(installId: String, reason: String)

    /** Single-instance campaign tick across API replicas (PostgreSQL advisory lock). */
    fun <T> withExclusiveCampaignLock(block: () -> T): T
}
