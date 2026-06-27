package ru.appforsale.alicecommands.api.infrastructure.security

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import ru.appforsale.alicecommands.api.domain.ports.LoginRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.persistence.LoginAttemptsTable
import org.jetbrains.exposed.sql.Database
import java.time.OffsetDateTime
import java.time.ZoneOffset

class NoOpLoginRateLimiter : LoginRateLimiter {
    override fun isBlocked(ip: String): Boolean = false
    override fun recordFailure(ip: String) = Unit
    override fun clearFailures(ip: String) = Unit
}

class ExposedLoginRateLimiter(
    private val database: Database,
    private val maxAttempts: Int = 5,
) : LoginRateLimiter {

    private val windowMinutes = 15L

    private inline fun unitTx(crossinline block: org.jetbrains.exposed.sql.Transaction.() -> Unit) {
        transaction(database) { block() }
    }

    override fun isBlocked(ip: String): Boolean = transaction(database) {
        val normalized = ClientIpNormalizer.normalize(ip)
        val since = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(windowMinutes)
        LoginAttemptsTable.selectAll()
            .where { (LoginAttemptsTable.ipAddress eq normalized) and (LoginAttemptsTable.attemptedAt greater since) }
            .count() >= maxAttempts
    }

    override fun recordFailure(ip: String) {
        unitTx {
            LoginAttemptsTable.insert {
                it[ipAddress] = ClientIpNormalizer.normalize(ip)
                it[attemptedAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }
    }

    override fun clearFailures(ip: String) {
        val normalized = ClientIpNormalizer.normalize(ip)
        unitTx { LoginAttemptsTable.deleteWhere { LoginAttemptsTable.ipAddress eq normalized } }
    }
}
