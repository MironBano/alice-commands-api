package ru.appforsale.alicecommands.api.domain.ports

import ru.appforsale.alicecommands.api.domain.AnalyticsBreakdownResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsEventAdminDto
import ru.appforsale.alicecommands.api.domain.AnalyticsEventDto
import ru.appforsale.alicecommands.api.domain.AnalyticsFunnelResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsSummaryResponse
import java.time.OffsetDateTime

data class AnalyticsInsertResult(
    val accepted: Int,
    val duplicates: Int,
)

interface AnalyticsEventRepository {
    fun insertBatchIgnoreDuplicates(clientIp: String, events: List<AnalyticsEventDto>): AnalyticsInsertResult

    fun countEventsForIpSince(clientIp: String, since: OffsetDateTime): Long

    fun querySummary(from: OffsetDateTime, to: OffsetDateTime): AnalyticsSummaryResponse

    fun listEvents(
        from: OffsetDateTime,
        to: OffsetDateTime,
        eventName: String?,
        installId: String?,
        limit: Int,
        offset: Int,
    ): Pair<List<AnalyticsEventAdminDto>, Int>

    fun queryFunnel(
        from: OffsetDateTime,
        to: OffsetDateTime,
        steps: List<String>,
    ): AnalyticsFunnelResponse

    fun queryBreakdown(
        from: OffsetDateTime,
        to: OffsetDateTime,
        eventName: String,
        param: String,
        limit: Int,
        fieldSource: String = "params",
    ): AnalyticsBreakdownResponse
}

interface AnalyticsRateLimiter {
    fun isRequestBlocked(ip: String): Boolean
    fun recordRequest(ip: String)
    fun isDailyEventCapExceeded(ip: String, incomingCount: Int): Boolean
}
