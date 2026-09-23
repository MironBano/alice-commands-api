package ru.appforsale.alicecommands.api.domain.push

/**
 * Notification teaser templates — must never include command phrase for S2/S3.
 * Mirror of app `PushNotificationCopy`.
 */
object PushNotificationTemplates {
    data class Template(val title: String, val body: String)

    val S1 = Template(
        title = "Обновился справочник команд",
        body = "Загляните в каталог — список команд обновлён",
    )
    val S2 = Template(
        title = "Команда дня готова",
        body = "Загляните в приложение — узнайте, что сказать Алисе сегодня",
    )
    val S3 = Template(
        title = "Популярно на этой неделе",
        body = "Откройте приложение — посмотрите, какую команду чаще выбирают с Алисой",
    )
    val S4 = Template(
        title = "С чего начать с колонкой",
        body = "3 простые команды для первого раза — откройте подборку",
    )
    val S5 = Template(
        title = "Команды для умного дома",
        body = "Управляйте светом и устройствами голосом",
    )

    fun s6(commandTitle: String) = Template(
        title = "Ваша команда под рукой",
        body = "Вы часто смотрели «$commandTitle» — сохраните в избранное",
    )

    fun assertNoPhraseLeak(scenario: String, title: String, body: String) {
        if (scenario == "s2" || scenario == "s3") {
            require(!body.contains("Скажи Алисе", ignoreCase = true)) {
                "S2/S3 notification must not contain phrase spoiler"
            }
            require(!title.contains("Скажи Алисе", ignoreCase = true)) {
                "S2/S3 notification must not contain phrase spoiler"
            }
        }
    }
}

object PushCampaignPriority {
    /** Lower index = higher priority. */
    val ORDER = listOf("s2", "s4", "s5", "s3", "s6", "s1")

    fun pickHighest(candidates: Set<String>): String? =
        ORDER.firstOrNull { it in candidates }
}

object PushQuietHours {
    fun isQuiet(localHour: Int): Boolean {
        val h = localHour.coerceIn(0, 23)
        return h >= 22 || h < 9
    }
}

object PushCampaignRules {
    const val MAX_PUSH_PER_DAY = 1
    const val MAX_PUSH_PER_WEEK = 3
    const val S1_MIN_GAP_DAYS = 7L
    const val S1_DEBOUNCE_HOURS = 24L
    const val S3_MIN_GAP_DAYS = 7L
    const val S6_MIN_GAP_DAYS = 30L
    const val S6_IDLE_DAYS = 14L
    const val COD_WINDOW_MINUTES = 15
    const val S3_HOUR = 11
    val S3_WEEKDAY: java.time.DayOfWeek = java.time.DayOfWeek.WEDNESDAY
    const val S4_HOUR_START = 18
    const val S4_HOUR_END = 20
}

data class PushCampaignContext(
    val nowUtc: java.time.OffsetDateTime,
    val publishedContentVersion: Int,
    val codCommandId: String?,
    val popularCommandId: String?,
    val popularHash: String?,
    val contentPublishedAt: java.time.OffsetDateTime?,
)

data class PushUserSignals(
    val dailyActiveToday: Boolean,
    val sessionStartCount: Int,
    val hasFirstValueTts: Boolean,
    val hasSmarthomeTabSelect: Boolean,
    val hasSmartHomeTts: Boolean,
    val lastAnyEventAt: java.time.OffsetDateTime?,
)

data class PushCandidate(
    val scenario: String,
    val title: String,
    val body: String,
    val deeplink: String,
    val commandId: String? = null,
    val channelId: String,
)

/**
 * Pure eligibility + payload builder for S1–S6 (unit-tested).
 */
object PushCampaignEvaluator {

    /** Calendar days from install local date to [localNow] local date (D0 = install day). */
    fun calendarDaysSinceInstall(
        installAnchor: java.time.OffsetDateTime,
        localNow: java.time.ZonedDateTime,
    ): Long {
        val installDate = installAnchor.atZoneSameInstant(localNow.zone).toLocalDate()
        val today = localNow.toLocalDate()
        return java.time.temporal.ChronoUnit.DAYS.between(installDate, today)
    }

    fun evaluateCandidates(
        token: PushTokenRecord,
        ctx: PushCampaignContext,
        signals: PushUserSignals,
    ): Set<String> {
        if (!token.masterEnabled) return emptySet()
        val zone = runCatching { java.time.ZoneId.of(token.timezone) }
            .getOrDefault(java.time.ZoneId.of("Europe/Moscow"))
        val local = ctx.nowUtc.atZoneSameInstant(zone)
        if (PushQuietHours.isQuiet(local.hour)) return emptySet()
        if (signals.dailyActiveToday) return emptySet()
        if (!withinGlobalCaps(token, ctx.nowUtc, zone)) return emptySet()

        val out = linkedSetOf<String>()
        if (eligibleS2(token, ctx, local)) out += "s2"
        if (eligibleS4(token, signals, local, ctx.nowUtc)) out += "s4"
        if (eligibleS5(token, signals, local)) out += "s5"
        if (eligibleS3(token, ctx, local)) out += "s3"
        if (eligibleS6(token, signals, ctx.nowUtc)) out += "s6"
        if (eligibleS1(token, ctx)) out += "s1"
        return out
    }

    fun buildCandidate(
        scenario: String,
        token: PushTokenRecord,
        ctx: PushCampaignContext,
    ): PushCandidate? {
        return when (scenario) {
            "s1" -> PushCandidate(
                scenario = "s1",
                title = PushNotificationTemplates.S1.title,
                body = PushNotificationTemplates.S1.body,
                deeplink = "alicecommands://route/home/catalog?source=push",
                channelId = "push_content",
            )
            "s2" -> {
                val id = ctx.codCommandId ?: return null
                PushCandidate(
                    scenario = "s2",
                    title = PushNotificationTemplates.S2.title,
                    body = PushNotificationTemplates.S2.body,
                    deeplink = "alicecommands://command/$id?source=push",
                    commandId = id,
                    channelId = "push_reminder",
                ).also {
                    PushNotificationTemplates.assertNoPhraseLeak("s2", it.title, it.body)
                }
            }
            "s3" -> {
                val id = ctx.popularCommandId ?: return null
                PushCandidate(
                    scenario = "s3",
                    title = PushNotificationTemplates.S3.title,
                    body = PushNotificationTemplates.S3.body,
                    deeplink = "alicecommands://command/$id?source=push",
                    commandId = id,
                    channelId = "push_content",
                ).also {
                    PushNotificationTemplates.assertNoPhraseLeak("s3", it.title, it.body)
                }
            }
            "s4" -> PushCandidate(
                scenario = "s4",
                title = PushNotificationTemplates.S4.title,
                body = PushNotificationTemplates.S4.body,
                deeplink = "alicecommands://route/home/catalog?scroll=try_now&source=push",
                channelId = "push_reminder",
            )
            "s5" -> PushCandidate(
                scenario = "s5",
                title = PushNotificationTemplates.S5.title,
                body = PushNotificationTemplates.S5.body,
                deeplink = "alicecommands://route/home/smarthome?source=push",
                channelId = "push_reminder",
            )
            "s6" -> {
                val top = token.frequentCommands.firstOrNull() ?: return null
                val template = PushNotificationTemplates.s6(top.title)
                PushCandidate(
                    scenario = "s6",
                    title = template.title,
                    body = template.body,
                    deeplink = "alicecommands://command/${top.id}?source=push",
                    commandId = top.id,
                    channelId = "push_reminder",
                )
            }
            else -> null
        }
    }

    private fun withinGlobalCaps(
        token: PushTokenRecord,
        nowUtc: java.time.OffsetDateTime,
        zone: java.time.ZoneId,
    ): Boolean {
        val last = token.lastPushAt
        if (last != null) {
            val lastLocal = last.atZoneSameInstant(zone).toLocalDate()
            val todayLocal = nowUtc.atZoneSameInstant(zone).toLocalDate()
            if (lastLocal == todayLocal) return false
        }
        val weekBucket = isoWeekBucket(nowUtc, zone)
        val weekCount = if (token.weekBucket == weekBucket) token.pushesThisWeek else 0
        return weekCount < PushCampaignRules.MAX_PUSH_PER_WEEK
    }

    fun isoWeekBucket(nowUtc: java.time.OffsetDateTime, zone: java.time.ZoneId): String {
        val localDate = nowUtc.atZoneSameInstant(zone).toLocalDate()
        val week = localDate.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
        val year = localDate.get(java.time.temporal.WeekFields.ISO.weekBasedYear())
        return "%04d-W%02d".format(year, week)
    }

    private fun eligibleS1(token: PushTokenRecord, ctx: PushCampaignContext): Boolean {
        if (ctx.publishedContentVersion <= 0) return false
        if (ctx.publishedContentVersion <= token.lastNotifiedContentVersion) return false
        // Client already on published version — no need to nudge catalog update.
        if (token.contentVersion >= ctx.publishedContentVersion) return false
        val last = token.lastS1At
        if (last != null) {
            val gapDays = java.time.Duration.between(last, ctx.nowUtc).toDays()
            if (gapDays < PushCampaignRules.S1_MIN_GAP_DAYS) return false
        }
        val publishedAt = ctx.contentPublishedAt
        if (publishedAt != null) {
            val hoursSincePublish = java.time.Duration.between(publishedAt, ctx.nowUtc).toHours()
            if (hoursSincePublish < 1) return false
            if (token.lastS1At != null &&
                java.time.Duration.between(token.lastS1At, ctx.nowUtc).toHours() < PushCampaignRules.S1_DEBOUNCE_HOURS
            ) {
                return false
            }
        }
        return true
    }

    private fun eligibleS2(
        token: PushTokenRecord,
        ctx: PushCampaignContext,
        local: java.time.ZonedDateTime,
    ): Boolean {
        if (!token.codEnabled) return false
        if (ctx.codCommandId.isNullOrBlank()) return false
        val parts = token.codReminderTime.split(":")
        if (parts.size < 2) return false
        val hour = parts[0].toIntOrNull() ?: return false
        val minute = parts[1].toIntOrNull() ?: return false
        val targetMinutes = hour * 60 + minute
        val nowMinutes = local.hour * 60 + local.minute
        val delta = kotlin.math.abs(nowMinutes - targetMinutes)
        return delta <= PushCampaignRules.COD_WINDOW_MINUTES
    }

    private fun eligibleS3(
        token: PushTokenRecord,
        ctx: PushCampaignContext,
        local: java.time.ZonedDateTime,
    ): Boolean {
        if (ctx.popularCommandId.isNullOrBlank() || ctx.popularHash.isNullOrBlank()) return false
        if (token.lastPopularHash == ctx.popularHash) return false
        if (local.dayOfWeek != PushCampaignRules.S3_WEEKDAY) return false
        if (local.hour != PushCampaignRules.S3_HOUR) return false
        val last = token.lastS3At
        if (last != null) {
            val gap = java.time.Duration.between(last, ctx.nowUtc).toDays()
            if (gap < PushCampaignRules.S3_MIN_GAP_DAYS) return false
        }
        return true
    }

    private fun eligibleS4(
        token: PushTokenRecord,
        signals: PushUserSignals,
        local: java.time.ZonedDateTime,
        nowUtc: java.time.OffsetDateTime,
    ): Boolean {
        if (token.s4Sent) return false
        if (!token.persona.equals("NEW_SPEAKER", ignoreCase = true)) return false
        if (token.checklistCompletedCount >= 3) return false
        if (signals.hasFirstValueTts && signals.sessionStartCount >= 2) return false
        if (signals.sessionStartCount > 1) return false
        val installAnchor = token.appInstalledAt ?: token.createdAt
        if (calendarDaysSinceInstall(installAnchor, local) != 1L) return false
        return local.hour in PushCampaignRules.S4_HOUR_START..PushCampaignRules.S4_HOUR_END
    }

    private fun eligibleS5(
        token: PushTokenRecord,
        signals: PushUserSignals,
        local: java.time.ZonedDateTime,
    ): Boolean {
        if (token.s5Sent) return false
        if (!token.persona.equals("SMART_HOME", ignoreCase = true)) return false
        if (!signals.hasSmarthomeTabSelect) return false
        if (signals.hasSmartHomeTts) return false
        val installAnchor = token.appInstalledAt ?: token.createdAt
        val installAgeDays = calendarDaysSinceInstall(installAnchor, local)
        return installAgeDays in 2L..3L
    }

    private fun eligibleS6(
        token: PushTokenRecord,
        signals: PushUserSignals,
        nowUtc: java.time.OffsetDateTime,
    ): Boolean {
        if (token.frequentCommands.isEmpty()) return false
        val lastEvent = signals.lastAnyEventAt ?: return false
        val idleDays = java.time.Duration.between(lastEvent, nowUtc).toDays()
        if (idleDays < PushCampaignRules.S6_IDLE_DAYS) return false
        val last = token.lastS6At
        if (last != null) {
            val gap = java.time.Duration.between(last, nowUtc).toDays()
            if (gap < PushCampaignRules.S6_MIN_GAP_DAYS) return false
        }
        return true
    }
}
