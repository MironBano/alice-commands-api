package ru.appforsale.alicecommands.api.infrastructure.security

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header

object ClientIpResolver {
    fun resolve(call: ApplicationCall): String {
        val forwarded = call.request.header("X-Forwarded-For")
            ?.split(',')
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val realIp = call.request.header("X-Real-IP")?.trim()?.takeIf { it.isNotBlank() }
        val raw = forwarded ?: realIp ?: call.request.local.remoteHost
        return ClientIpNormalizer.normalize(raw)
    }
}
