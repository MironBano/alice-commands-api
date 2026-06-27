package ru.appforsale.alicecommands.api.infrastructure.security

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SessionSigner(secret: String) {
    private val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")

    fun sign(sessionId: String): String {
        val signature = hmac(sessionId)
        return "$sessionId.$signature"
    }

    fun verify(signedValue: String): String? {
        val dot = signedValue.lastIndexOf('.')
        if (dot <= 0) return null
        val sessionId = signedValue.substring(0, dot)
        val signature = signedValue.substring(dot + 1)
        if (sessionId.isBlank() || signature.isBlank()) return null
        return if (constantTimeEquals(hmac(sessionId), signature)) sessionId else null
    }

    private fun hmac(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)))
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
