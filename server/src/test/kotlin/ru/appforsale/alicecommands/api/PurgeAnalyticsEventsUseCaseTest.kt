package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.analytics.PurgeAnalyticsEventsUseCase
import ru.appforsale.alicecommands.api.domain.AnalyticsBreakdownResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsEventAdminDto
import ru.appforsale.alicecommands.api.domain.AnalyticsEventDto
import ru.appforsale.alicecommands.api.domain.AnalyticsFunnelResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsSummaryResponse
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsEventRepository
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsInsertResult
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PurgeAnalyticsEventsUseCaseTest {

    @Test
    fun `execute deletes events older than retention window`() {
        val repository = RecordingAnalyticsEventRepository()
        val useCase = PurgeAnalyticsEventsUseCase(repository, retentionDays = 90)

        val deleted = useCase.execute()

        assertEquals(3, deleted)
        assertEquals(1, repository.deleteCalls.size)
        val cutoff = repository.deleteCalls.single()
        val expectedCutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(90)
        assertTrue(cutoff.isBefore(expectedCutoff.plusMinutes(1)))
        assertTrue(cutoff.isAfter(expectedCutoff.minusMinutes(1)))
    }

    private class RecordingAnalyticsEventRepository : AnalyticsEventRepository {
        val deleteCalls = mutableListOf<OffsetDateTime>()

        override fun insertBatchIgnoreDuplicates(
            clientIp: String,
            events: List<AnalyticsEventDto>,
        ) = AnalyticsInsertResult(0, 0)

        override fun countEventsForIpSince(clientIp: String, since: OffsetDateTime) = 0L

        override fun querySummary(from: OffsetDateTime, to: OffsetDateTime) =
            AnalyticsSummaryResponse(
                from = from.toString(),
                to = to.toString(),
                daily_active_installs = 0,
                total_events = 0,
                unique_installs = 0,
                top_events = emptyList(),
                raw_unique_installs = 0,
            )

        override fun listEvents(
            from: OffsetDateTime,
            to: OffsetDateTime,
            eventName: String?,
            installId: String?,
            limit: Int,
            offset: Int,
        ) = emptyList<AnalyticsEventAdminDto>() to 0

        override fun queryFunnel(
            from: OffsetDateTime,
            to: OffsetDateTime,
            steps: List<String>,
        ) = AnalyticsFunnelResponse(
            from = from.toLocalDate().toString(),
            to = to.toLocalDate().toString(),
            steps = emptyList(),
        )

        override fun queryBreakdown(
            from: OffsetDateTime,
            to: OffsetDateTime,
            eventName: String,
            param: String,
            limit: Int,
            fieldSource: String,
        ) = AnalyticsBreakdownResponse(
            from = from.toLocalDate().toString(),
            to = to.toLocalDate().toString(),
            event_name = eventName,
            param = param,
            field_source = fieldSource,
            items = emptyList(),
        )

        override fun deleteEventsOlderThan(cutoff: OffsetDateTime): Int {
            deleteCalls += cutoff
            return 3
        }

        override fun loadPushUserSignals(
            installId: String,
            dayStartUtc: OffsetDateTime,
            appInstalledAt: OffsetDateTime?,
        ) = ru.appforsale.alicecommands.api.domain.push.PushUserSignals(
            dailyActiveToday = false,
            sessionStartCount = 0,
            hasFirstValueTts = false,
            hasSmarthomeTabSelect = false,
            hasSmartHomeTts = false,
            lastAnyEventAt = null,
        )
    }
}
