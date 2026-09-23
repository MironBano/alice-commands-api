package ru.appforsale.alicecommands.api.infrastructure.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object PushTokensTable : Table("push_tokens") {
    val installId = text("install_id")
    val rustoreToken = text("rustore_token")
    val timezone = text("timezone")
    val persona = text("persona").nullable()
    val contentVersion = integer("content_version")
    val masterEnabled = bool("master_enabled")
    val codEnabled = bool("cod_enabled")
    val codReminderTime = text("cod_reminder_time")
    val checklistCompletedCount = integer("checklist_completed_count")
    val frequentCommandsJson = text("frequent_commands_json")
    val appVersion = text("app_version").nullable()
    val lastS1At = timestampWithTimeZone("last_s1_at").nullable()
    val lastS3At = timestampWithTimeZone("last_s3_at").nullable()
    val lastS6At = timestampWithTimeZone("last_s6_at").nullable()
    val s4Sent = bool("s4_sent")
    val s5Sent = bool("s5_sent")
    val lastPopularHash = text("last_popular_hash").nullable()
    val lastNotifiedContentVersion = integer("last_notified_content_version")
    val lastPushAt = timestampWithTimeZone("last_push_at").nullable()
    val pushesThisWeek = integer("pushes_this_week")
    val weekBucket = text("week_bucket").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val appInstalledAt = timestampWithTimeZone("app_installed_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(installId)
}
