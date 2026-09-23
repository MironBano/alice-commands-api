package ru.appforsale.alicecommands.api.domain.ports

interface PushInstallRateLimiter {
    fun isBlocked(installId: String): Boolean
    fun recordSubmission(installId: String)
}
