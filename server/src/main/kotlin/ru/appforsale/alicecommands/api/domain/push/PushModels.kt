package ru.appforsale.alicecommands.api.domain.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class PushRegisterRequest(
    @SerialName("installId") val installId: String,
    @SerialName("rustoreToken") val rustoreToken: String,
    @SerialName("timezone") val timezone: String,
    @SerialName("persona") val persona: String? = null,
    @SerialName("contentVersion") val contentVersion: Int = 0,
    @SerialName("codEnabled") val codEnabled: Boolean = false,
    @SerialName("codReminderTime") val codReminderTime: String = "09:00",
    @SerialName("masterEnabled") val masterEnabled: Boolean = true,
    @SerialName("checklistCompletedCount") val checklistCompletedCount: Int = 0,
    @SerialName("frequentCommands") val frequentCommands: List<PushFrequentCommandDto> = emptyList(),
    @SerialName("appVersion") val appVersion: String? = null,
    @SerialName("appInstalledAt") val appInstalledAt: String? = null,
    /** Delivery channel: `rustore` (default) or `fcm` (Google Play). Token still travels as rustoreToken. */
    @SerialName("provider") val provider: String = PushProvider.RUSTORE,
)

@Serializable
data class PushFrequentCommandDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("viewCount") val viewCount: Int = 0,
)

@Serializable
data class PushPreferencesRequest(
    @SerialName("installId") val installId: String,
    @SerialName("masterEnabled") val masterEnabled: Boolean,
    @SerialName("codEnabled") val codEnabled: Boolean,
    @SerialName("codReminderTime") val codReminderTime: String,
)

@Serializable
data class PushUnregisterRequest(
    @SerialName("installId") val installId: String,
    @SerialName("rustoreToken") val rustoreToken: String,
    @SerialName("provider") val provider: String = PushProvider.RUSTORE,
)

object PushProvider {
    const val RUSTORE = "rustore"
    const val FCM = "fcm"

    fun normalize(raw: String?): String {
        val value = raw?.trim()?.lowercase().orEmpty()
        return when (value) {
            "", RUSTORE -> RUSTORE
            FCM -> FCM
            else -> error("unsupported_push_provider")
        }
    }
}

data class PushTokenRecord(
    val installId: String,
    val rustoreToken: String,
    /** `rustore` or `fcm` — selects campaign sender. */
    val provider: String = PushProvider.RUSTORE,
    val timezone: String,
    val persona: String?,
    val contentVersion: Int,
    val masterEnabled: Boolean,
    val codEnabled: Boolean,
    val codReminderTime: String,
    val checklistCompletedCount: Int,
    val frequentCommands: List<PushFrequentCommandDto>,
    val frequentCommandsJson: String,
    val appVersion: String?,
    val lastS1At: OffsetDateTime?,
    val lastS3At: OffsetDateTime?,
    val lastS6At: OffsetDateTime?,
    val s4Sent: Boolean,
    val s5Sent: Boolean,
    val lastPopularHash: String?,
    val lastNotifiedContentVersion: Int,
    val lastPushAt: OffsetDateTime?,
    val pushesThisWeek: Int,
    val weekBucket: String?,
    val createdAt: OffsetDateTime,
    val appInstalledAt: OffsetDateTime?,
    /** Non-null when RuStore rejected the token (e.g. rustore_push_http_404); skipped by campaign. */
    val deliveryBlockedReason: String? = null,
)

data class PushRegisterResult(
    /** True when client re-registered the same token that is already delivery-blocked. */
    val tokenStale: Boolean = false,
)
