package ru.appforsale.alicecommands.api.infrastructure.security

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.appforsale.alicecommands.api.domain.ports.PublicSubmissionRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.persistence.PublicSubmissionAttemptsTable
import java.time.OffsetDateTime
import java.time.ZoneOffset

class NoOpPublicSubmissionRateLimiter : PublicSubmissionRateLimiter {
    override fun isBlocked(ip: String): Boolean = false
    override fun recordSubmission(ip: String) = Unit
}

class ExposedPublicSubmissionRateLimiter(
    private val database: Database,
    private val maxSubmissions: Int = 20,
) : PublicSubmissionRateLimiter {

    private val windowMinutes = 15L

    override fun isBlocked(ip: String): Boolean = transaction(database) {
        val normalized = ClientIpNormalizer.normalize(ip)
        val since = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(windowMinutes)
        PublicSubmissionAttemptsTable.selectAll()
            .where {
                (PublicSubmissionAttemptsTable.ipAddress eq normalized) and
                    (PublicSubmissionAttemptsTable.attemptedAt greater since)
            }
            .count() >= maxSubmissions
    }

    override fun recordSubmission(ip: String) {
        val normalized = ClientIpNormalizer.normalize(ip)
        transaction(database) {
            PublicSubmissionAttemptsTable.insert {
                it[ipAddress] = normalized
                it[attemptedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }
            val cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(windowMinutes * 4)
            PublicSubmissionAttemptsTable.deleteWhere { attemptedAt less cutoff }
        }
    }
}
