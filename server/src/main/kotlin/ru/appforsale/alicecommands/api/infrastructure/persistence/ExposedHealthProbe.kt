package ru.appforsale.alicecommands.api.infrastructure.persistence

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import ru.appforsale.alicecommands.api.domain.ports.HealthProbe

class ExposedHealthProbe(private val database: Database) : HealthProbe {
    override fun isDatabaseOk(): Boolean = try {
        transaction(database) {
            exec("SELECT 1")
        }
        true
    } catch (_: Exception) {
        false
    }
}
