package ru.appforsale.alicecommands.api.domain.ports

interface HealthProbe {
    fun isDatabaseOk(): Boolean
}
