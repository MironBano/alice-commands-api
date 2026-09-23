package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.domain.push.PushCampaignContext
import ru.appforsale.alicecommands.api.domain.push.PushCampaignEvaluator
import ru.appforsale.alicecommands.api.domain.push.PushCampaignPriority
import ru.appforsale.alicecommands.api.domain.push.PushFrequentCommandDto
import ru.appforsale.alicecommands.api.domain.push.PushNotificationTemplates
import ru.appforsale.alicecommands.api.domain.push.PushQuietHours
import ru.appforsale.alicecommands.api.domain.push.PushTokenRecord
import ru.appforsale.alicecommands.api.domain.push.PushUserSignals
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PushCampaignLogicTest {

    @Test
    fun priority_picksHighest() {
        assertEquals(
            "s2",
            PushCampaignPriority.pickHighest(setOf("s1", "s3", "s2")),
        )
        assertEquals(
            "s4",
            PushCampaignPriority.pickHighest(setOf("s6", "s4", "s1")),
        )
        assertNull(PushCampaignPriority.pickHighest(emptySet()))
    }

    @Test
    fun quietHours() {
        assertTrue(PushQuietHours.isQuiet(22))
        assertTrue(PushQuietHours.isQuiet(3))
        assertFalse(PushQuietHours.isQuiet(9))
        assertFalse(PushQuietHours.isQuiet(15))
    }

    @Test
    fun s2_s3_templates_haveNoPhrase() {
        PushNotificationTemplates.assertNoPhraseLeak("s2", PushNotificationTemplates.S2.title, PushNotificationTemplates.S2.body)
        PushNotificationTemplates.assertNoPhraseLeak("s3", PushNotificationTemplates.S3.title, PushNotificationTemplates.S3.body)
    }

    @Test
    fun spoiler_rejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PushNotificationTemplates.assertNoPhraseLeak(
                "s2",
                "Команда дня",
                "Скажи Алисе: «Включи музыку»",
            )
        }
    }

    @Test
    fun dailyActive_skipsAll() {
        val now = OffsetDateTime.parse("2026-09-10T12:00:00Z")
        val token = sampleToken(now)
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now),
            signals = PushUserSignals(
                dailyActiveToday = true,
                sessionStartCount = 0,
                hasFirstValueTts = false,
                hasSmarthomeTabSelect = false,
                hasSmartHomeTts = false,
                lastAnyEventAt = now.minusDays(20),
            ),
        )
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun s2_eligible_in_cod_window() {
        val now = OffsetDateTime.of(2026, 9, 10, 6, 5, 0, 0, ZoneOffset.UTC) // 09:05 MSK
        val token = sampleToken(now).copy(codEnabled = true, codReminderTime = "09:00")
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now).copy(codCommandId = "music_muzyka"),
            signals = idleSignals(now),
        )
        assertTrue(candidates.contains("s2"))
        val built = PushCampaignEvaluator.buildCandidate(
            "s2",
            token,
            sampleCtx(now).copy(codCommandId = "music_muzyka"),
        )!!
        assertFalse(built.body.contains("Скажи Алисе", ignoreCase = true))
        assertTrue(built.deeplink.contains("music_muzyka"))
    }

    @Test
    fun s6_uses_title_not_idle_skip() {
        val now = OffsetDateTime.parse("2026-09-10T12:00:00Z")
        val token = sampleToken(now).copy(
            frequentCommands = listOf(PushFrequentCommandDto("cmd_a", "Музыка", 3)),
        )
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now),
            signals = idleSignals(now).copy(lastAnyEventAt = now.minusDays(20)),
        )
        assertTrue(candidates.contains("s6"))
        val built = PushCampaignEvaluator.buildCandidate("s6", token, sampleCtx(now))!!
        assertTrue(built.body.contains("Музыка"))
    }

    @Test
    fun s4_newSpeaker_d1_eligible() {
        val now = OffsetDateTime.of(2026, 9, 10, 16, 0, 0, 0, ZoneOffset.UTC) // 19:00 MSK
        val token = sampleToken(now).copy(
            persona = "NEW_SPEAKER",
            appInstalledAt = now.minusDays(1),
            createdAt = now.minusDays(1),
            s4Sent = false,
            checklistCompletedCount = 1,
        )
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now),
            signals = idleSignals(now).copy(sessionStartCount = 1, hasFirstValueTts = false),
        )
        assertTrue(candidates.contains("s4"))
    }

    @Test
    fun s4_skipped_when_checklist_done() {
        val now = OffsetDateTime.of(2026, 9, 10, 16, 0, 0, 0, ZoneOffset.UTC)
        val token = sampleToken(now).copy(
            persona = "NEW_SPEAKER",
            createdAt = now.minusDays(1),
            checklistCompletedCount = 3,
        )
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now),
            signals = idleSignals(now).copy(sessionStartCount = 0),
        )
        assertFalse(candidates.contains("s4"))
    }

    @Test
    fun s5_smartHome_d2_eligible() {
        val now = OffsetDateTime.of(2026, 9, 10, 12, 0, 0, 0, ZoneOffset.UTC)
        val token = sampleToken(now).copy(
            persona = "SMART_HOME",
            appInstalledAt = now.minusDays(2),
            createdAt = now.minusDays(2),
            s5Sent = false,
        )
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now),
            signals = idleSignals(now).copy(
                hasSmarthomeTabSelect = true,
                hasSmartHomeTts = false,
            ),
        )
        assertTrue(candidates.contains("s5"))
    }

    @Test
    fun s1_skipped_when_client_already_synced() {
        val now = OffsetDateTime.parse("2026-09-10T12:00:00Z")
        val token = sampleToken(now).copy(
            contentVersion = 5,
            lastNotifiedContentVersion = 0,
        )
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now).copy(publishedContentVersion = 5),
            signals = idleSignals(now),
        )
        assertFalse(candidates.contains("s1"))
    }

    @Test
    fun s4_eligible_at_hour_20() {
        val now = OffsetDateTime.of(2026, 9, 10, 17, 30, 0, 0, ZoneOffset.UTC) // 20:30 MSK
        val token = sampleToken(now).copy(
            persona = "NEW_SPEAKER",
            appInstalledAt = now.minusDays(1),
            createdAt = now,
            s4Sent = false,
            checklistCompletedCount = 1,
        )
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now),
            signals = idleSignals(now).copy(sessionStartCount = 1, hasFirstValueTts = false),
        )
        assertTrue(candidates.contains("s4"))
    }

    @Test
    fun s4_uses_appInstalled_not_push_opt_in_date() {
        val now = OffsetDateTime.of(2026, 9, 10, 16, 0, 0, 0, ZoneOffset.UTC) // 19:00 MSK
        val token = sampleToken(now).copy(
            persona = "NEW_SPEAKER",
            appInstalledAt = now.minusDays(1),
            createdAt = now,
            s4Sent = false,
            checklistCompletedCount = 1,
        )
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now),
            signals = idleSignals(now).copy(sessionStartCount = 1, hasFirstValueTts = false),
        )
        assertTrue(candidates.contains("s4"))
    }

    @Test
    fun s4_calendar_d1_not_wall_clock_hours() {
        val installAt = OffsetDateTime.of(2026, 9, 9, 19, 0, 0, 0, ZoneOffset.UTC) // Sep 9 22:00 MSK
        val now = OffsetDateTime.of(2026, 9, 10, 15, 0, 0, 0, ZoneOffset.UTC) // Sep 10 18:00 MSK
        assertEquals(0L, java.time.Duration.between(installAt, now).toDays())
        val token = sampleToken(now).copy(
            persona = "NEW_SPEAKER",
            timezone = "Europe/Moscow",
            appInstalledAt = installAt,
            createdAt = installAt,
            s4Sent = false,
            checklistCompletedCount = 1,
        )
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now),
            signals = idleSignals(now).copy(sessionStartCount = 1, hasFirstValueTts = false),
        )
        assertTrue(candidates.contains("s4"))
    }

    @Test
    fun calendarDaysSinceInstall_matchesLocalDates() {
        val local = OffsetDateTime.of(2026, 9, 12, 10, 0, 0, 0, ZoneOffset.UTC)
            .atZoneSameInstant(java.time.ZoneId.of("Europe/Moscow"))
        val install = OffsetDateTime.of(2026, 9, 9, 12, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(3L, PushCampaignEvaluator.calendarDaysSinceInstall(install, local))
    }

    @Test
    fun s2_skipped_without_cod_id() {
        val now = OffsetDateTime.of(2026, 9, 10, 6, 5, 0, 0, ZoneOffset.UTC)
        val token = sampleToken(now).copy(codEnabled = true, codReminderTime = "09:00")
        val candidates = PushCampaignEvaluator.evaluateCandidates(
            token = token,
            ctx = sampleCtx(now).copy(codCommandId = null),
            signals = idleSignals(now),
        )
        assertFalse(candidates.contains("s2"))
    }

    private fun sampleCtx(now: OffsetDateTime) = PushCampaignContext(
        nowUtc = now,
        publishedContentVersion = 5,
        codCommandId = "music_muzyka",
        popularCommandId = "music_luchshie",
        popularHash = "abc",
        contentPublishedAt = now.minusHours(3),
    )

    private fun idleSignals(now: OffsetDateTime) = PushUserSignals(
        dailyActiveToday = false,
        sessionStartCount = 0,
        hasFirstValueTts = false,
        hasSmarthomeTabSelect = false,
        hasSmartHomeTts = false,
        lastAnyEventAt = now.minusDays(20),
    )

    private fun sampleToken(now: OffsetDateTime) = PushTokenRecord(
        installId = "inst1",
        rustoreToken = "tok",
        timezone = "Europe/Moscow",
        persona = "NEW_SPEAKER",
        contentVersion = 4,
        masterEnabled = true,
        codEnabled = false,
        codReminderTime = "09:00",
        checklistCompletedCount = 0,
        frequentCommands = emptyList(),
        frequentCommandsJson = "[]",
        appVersion = "1.0.1",
        lastS1At = null,
        lastS3At = null,
        lastS6At = null,
        s4Sent = false,
        s5Sent = false,
        lastPopularHash = null,
        lastNotifiedContentVersion = 0,
        lastPushAt = null,
        pushesThisWeek = 0,
        weekBucket = null,
        createdAt = now.minusDays(1),
        appInstalledAt = now.minusDays(1),
    )
}
