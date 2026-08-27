package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.analytics.SubmitAnalyticsBatchUseCase
import ru.appforsale.alicecommands.api.domain.AnalyticsBatchRequest
import ru.appforsale.alicecommands.api.domain.AnalyticsBreakdownResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsEventDto
import ru.appforsale.alicecommands.api.domain.AnalyticsEventAdminDto
import ru.appforsale.alicecommands.api.domain.AnalyticsFunnelResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsSummaryResponse
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsEventRepository
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsInsertResult
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsRateLimiter
import java.time.OffsetDateTime

class SubmitAnalyticsBatchUseCaseTest {

    private val rateLimiter = object : AnalyticsRateLimiter {
        override fun isRequestBlocked(ip: String) = false
        override fun recordRequest(ip: String) = Unit
        override fun isDailyEventCapExceeded(ip: String, incomingCount: Int) = false
    }

    @Test
    fun `valid event accepted invalid rejected in same batch`() {
        val repo = FakeAnalyticsEventRepository()
        val useCase = SubmitAnalyticsBatchUseCase(repo, rateLimiter)
        val valid = sampleEvent(eventName = "screen_view")
        val invalid = sampleEvent(eventName = "INVALID-NAME")

        val result = useCase.execute(
            clientIp = "127.0.0.1",
            request = AnalyticsBatchRequest(events = listOf(valid, invalid)),
        )

        assertEquals(1, result.accepted)
        assertEquals(0, result.duplicates)
        assertEquals(1, result.rejected)
        assertEquals(1, repo.inserted.size)
    }

    @Test
    fun `pii param key rejected`() {
        val useCase = SubmitAnalyticsBatchUseCase(FakeAnalyticsEventRepository(), rateLimiter)
        assertFalse(useCase.validateEvent(sampleEvent(params = mapOf("search_query" to "secret"))))
        assertFalse(useCase.validateEvent(sampleEvent(params = mapOf("query" to "secret"))))
    }

    @Test
    fun `pii user_properties key rejected`() {
        val useCase = SubmitAnalyticsBatchUseCase(FakeAnalyticsEventRepository(), rateLimiter)
        assertFalse(useCase.validateEvent(sampleEvent(userProperties = mapOf("query" to "x"))))
        assertFalse(useCase.validateEvent(sampleEvent(userProperties = mapOf("email" to "a@b.c"))))
        assertFalse(useCase.validateEvent(sampleEvent(userProperties = mapOf("search_query" to "secret"))))
        assertTrue(useCase.validateEvent(sampleEvent(userProperties = mapOf("is_pro" to "true"))))
    }

    @Test
    fun `query_length and message_length accepted`() {
        val useCase = SubmitAnalyticsBatchUseCase(FakeAnalyticsEventRepository(), rateLimiter)
        assertTrue(
            useCase.validateEvent(
                sampleEvent(
                    eventName = "search",
                    params = mapOf(
                        "query_length" to "5",
                        "results_count" to "12",
                    ),
                ),
            ),
        )
        assertTrue(
            useCase.validateEvent(
                sampleEvent(
                    eventName = "rating_low_feedback_submit",
                    params = mapOf(
                        "stars" to "2",
                        "message_length" to "42",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `search event accepted in batch`() {
        val repo = FakeAnalyticsEventRepository()
        val useCase = SubmitAnalyticsBatchUseCase(repo, rateLimiter)
        val search = sampleEvent(
            eventName = "search",
            params = mapOf("query_length" to "4", "results_count" to "3"),
        )

        val result = useCase.execute(
            clientIp = "127.0.0.1",
            request = AnalyticsBatchRequest(events = listOf(search)),
        )

        assertEquals(1, result.accepted)
        assertEquals(0, result.rejected)
        assertEquals(1, repo.inserted.size)
        assertEquals("search", repo.inserted.single().eventName)
    }

    @Test
    fun `rate limit blocks batch`() {
        val blockingLimiter = object : AnalyticsRateLimiter {
            override fun isRequestBlocked(ip: String) = true
            override fun recordRequest(ip: String) = Unit
            override fun isDailyEventCapExceeded(ip: String, incomingCount: Int) = false
        }
        val useCase = SubmitAnalyticsBatchUseCase(FakeAnalyticsEventRepository(), blockingLimiter)

        org.junit.jupiter.api.assertThrows<ru.appforsale.alicecommands.api.application.feedback.RateLimitException> {
            useCase.execute("127.0.0.1", AnalyticsBatchRequest(listOf(sampleEvent())))
        }
    }

    @Test
    fun `daily event cap blocks batch`() {
        val cappedLimiter = object : AnalyticsRateLimiter {
            override fun isRequestBlocked(ip: String) = false
            override fun recordRequest(ip: String) = Unit
            override fun isDailyEventCapExceeded(ip: String, incomingCount: Int) = true
        }
        val useCase = SubmitAnalyticsBatchUseCase(FakeAnalyticsEventRepository(), cappedLimiter)

        org.junit.jupiter.api.assertThrows<ru.appforsale.alicecommands.api.application.feedback.RateLimitException> {
            useCase.execute("127.0.0.1", AnalyticsBatchRequest(listOf(sampleEvent())))
        }
    }

    @Test
    fun `duplicate eventId counted`() {
        val repo = FakeAnalyticsEventRepository()
        val useCase = SubmitAnalyticsBatchUseCase(repo, rateLimiter)
        val event = sampleEvent()

        useCase.execute("127.0.0.1", AnalyticsBatchRequest(listOf(event)))
        val second = useCase.execute("127.0.0.1", AnalyticsBatchRequest(listOf(event)))

        assertEquals(0, second.accepted)
        assertEquals(1, second.duplicates)
    }

    @Test
    fun `occurredAt outside window rejected`() {
        val useCase = SubmitAnalyticsBatchUseCase(FakeAnalyticsEventRepository(), rateLimiter)
        val tooOld = sampleEvent(occurredAt = System.currentTimeMillis() - 31L * 24 * 3600 * 1000)
        val tooFuture = sampleEvent(occurredAt = System.currentTimeMillis() + 10 * 60 * 1000)

        assertFalse(useCase.validateEvent(tooOld))
        assertFalse(useCase.validateEvent(tooFuture))
    }

    @Test
    fun `more than 32 map keys rejected`() {
        val useCase = SubmitAnalyticsBatchUseCase(FakeAnalyticsEventRepository(), rateLimiter)
        val params = (1..33).associate { "key$it" to "v" }
        assertFalse(useCase.validateEvent(sampleEvent(params = params)))
    }

    private fun sampleEvent(
        eventName: String = "screen_view",
        occurredAt: Long = System.currentTimeMillis(),
        params: Map<String, String> = mapOf("route" to "home/catalog"),
        userProperties: Map<String, String> = mapOf("is_pro" to "false"),
    ) = AnalyticsEventDto(
        installId = "11111111-1111-4111-8111-111111111111",
        sessionId = "22222222-2222-4222-8222-222222222222",
        eventId = "33333333-3333-4333-8333-333333333333",
        eventName = eventName,
        occurredAt = occurredAt,
        appVersion = "1.2.0",
        androidVersion = "14",
        locale = "ru-RU",
        userProperties = userProperties,
        params = params,
    )

    private class FakeAnalyticsEventRepository : AnalyticsEventRepository {
        val inserted = mutableListOf<AnalyticsEventDto>()
        private val seenIds = mutableSetOf<String>()

        override fun insertBatchIgnoreDuplicates(
            clientIp: String,
            events: List<AnalyticsEventDto>,
        ): AnalyticsInsertResult {
            var accepted = 0
            var duplicates = 0
            events.forEach { event ->
                if (seenIds.add(event.eventId)) {
                    inserted += event
                    accepted++
                } else {
                    duplicates++
                }
            }
            return AnalyticsInsertResult(accepted, duplicates)
        }

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
    }
}
