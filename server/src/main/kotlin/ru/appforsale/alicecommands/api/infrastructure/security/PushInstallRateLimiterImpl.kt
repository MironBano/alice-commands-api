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
import ru.appforsale.alicecommands.api.domain.ports.PushInstallRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.persistence.PublicSubmissionAttemptsTable
import java.time.OffsetDateTime
import java.time.ZoneOffset

class NoOpPushInstallRateLimiter : PushInstallRateLimiter {
    override fun isBlocked(installId: String): Boolean = false
    override fun recordSubmission(installId: String) = Unit
}

class ExposedPushInstallRateLimiter(
    private val database: Database,
    private val maxSubmissions: Int = 30,
) : PushInstallRateLimiter {

    private val windowMinutes = 15L

    override fun isBlocked(installId: String): Boolean = transaction(database) {
        val key = installKey(installId)
        val since = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(windowMinutes)
        PublicSubmissionAttemptsTable.selectAll()
            .where {
                (PublicSubmissionAttemptsTable.ipAddress eq key) and
                    (PublicSubmissionAttemptsTable.attemptedAt greater since)
            }
            .count() >= maxSubmissions
    }

    override fun recordSubmission(installId: String) {
        val key = installKey(installId)
        transaction(database) {
            PublicSubmissionAttemptsTable.insert {
                it[ipAddress] = key
                it[attemptedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }
            val cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(windowMinutes * 4)
            PublicSubmissionAttemptsTable.deleteWhere { attemptedAt less cutoff }
        }
    }

    private fun installKey(installId: String): String =
        "push-install:${installId.trim().lowercase()}"
}
