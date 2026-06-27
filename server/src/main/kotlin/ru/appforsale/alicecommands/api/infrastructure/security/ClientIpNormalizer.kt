package ru.appforsale.alicecommands.api.infrastructure.security

object ClientIpNormalizer {
    fun normalize(raw: String): String = when (raw) {
        "127.0.0.1", "0:0:0:0:0:0:0:1", "::1", "localhost" -> "localhost"
        else -> raw
    }
}
