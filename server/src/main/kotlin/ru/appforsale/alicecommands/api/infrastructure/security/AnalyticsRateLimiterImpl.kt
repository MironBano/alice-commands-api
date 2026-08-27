package ru.appforsale.alicecommands.api.infrastructure.security

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsEventRepository
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.persistence.AnalyticsRequestAttemptsTable
import java.time.OffsetDateTime
import java.time.ZoneOffset

class NoOpAnalyticsRateLimiter : AnalyticsRateLimiter {
    override fun isRequestBlocked(ip: String): Boolean = false
    override fun recordRequest(ip: String) = Unit
    override fun isDailyEventCapExceeded(ip: String, incomingCount: Int): Boolean = false
}

class ExposedAnalyticsRateLimiter(
    private val database: Database,
    private val maxRequestsPerWindow: Int,
    private val maxEventsPerDay: Int,
    private val eventRepository: AnalyticsEventRepository,
) : AnalyticsRateLimiter {

    private val windowMinutes = 15L

    override fun isRequestBlocked(ip: String): Boolean = transaction(database) {
        val normalized = ClientIpNormalizer.normalize(ip)
        val since = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(windowMinutes)
        AnalyticsRequestAttemptsTable.selectAll()
            .where {
                (AnalyticsRequestAttemptsTable.ipAddress eq normalized) and
                    (AnalyticsRequestAttemptsTable.attemptedAt greater since)
            }
            .count() >= maxRequestsPerWindow
    }

    override fun recordRequest(ip: String) {
        val normalized = ClientIpNormalizer.normalize(ip)
        transaction(database) {
            AnalyticsRequestAttemptsTable.insert {
                it[ipAddress] = normalized
                it[attemptedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }
            val cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(windowMinutes * 4)
            AnalyticsRequestAttemptsTable.deleteWhere { attemptedAt less cutoff }
        }
    }

    override fun isDailyEventCapExceeded(ip: String, incomingCount: Int): Boolean {
        val normalized = ClientIpNormalizer.normalize(ip)
        val startOfDay = OffsetDateTime.now(ZoneOffset.UTC)
            .toLocalDate()
            .atStartOfDay()
            .atOffset(ZoneOffset.UTC)
        val existing = eventRepository.countEventsForIpSince(normalized, startOfDay)
        return existing + incomingCount > maxEventsPerDay
    }
}
