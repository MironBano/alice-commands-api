package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.push.PushCampaignTickerSchedule

class PushCampaignTickerScheduleTest {

    @Test
    fun alignedTick_withinToleranceReturnsZero() {
        val boundary = 1_000L * 60L * 15L // exact quarter
        assertEquals(0L, PushCampaignTickerSchedule.delayMsUntilAlignedTick(boundary))
        assertEquals(0L, PushCampaignTickerSchedule.delayMsUntilAlignedTick(boundary + 500))
    }

    @Test
    fun alignedTick_waitsUntilNextQuarter() {
        val boundary = 1_000L * 60L * 15L
        assertEquals(
            60_000L,
            PushCampaignTickerSchedule.delayMsUntilAlignedTick(boundary + 14 * 60_000L),
        )
    }

    @Test
    fun nextQuarterStrict_neverZeroOnBoundary() {
        val boundary = 1_000L * 60L * 30L
        assertEquals(
            PushCampaignTickerSchedule.QUARTER_MS,
            PushCampaignTickerSchedule.delayMsUntilNextQuarterStrict(boundary),
        )
        assertEquals(
            1_000L,
            PushCampaignTickerSchedule.delayMsUntilNextQuarterStrict(boundary + PushCampaignTickerSchedule.QUARTER_MS - 1_000L),
        )
    }
}
