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
)

data class PushTokenRecord(
    val installId: String,
    val rustoreToken: String,
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
)
