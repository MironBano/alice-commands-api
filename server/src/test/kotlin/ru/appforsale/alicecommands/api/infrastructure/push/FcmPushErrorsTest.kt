package ru.appforsale.alicecommands.api.infrastructure.push

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FcmPushErrorsTest {
    @Test
    fun unregisteredBodyMapsToTokenNotFound() {
        val body = """{"error":{"status":"NOT_FOUND","details":[{"errorCode":"UNREGISTERED"}]}}"""
        assertEquals(FcmPushErrors.TOKEN_NOT_FOUND, FcmPushErrors.errorMessage(404, body))
        assertTrue(FcmPushErrors.isTokenNotFound(404, body))
    }

    @Test
    fun otherHttpErrorsKeepStatusCode() {
        assertEquals("fcm_push_http_503", FcmPushErrors.errorMessage(503, "unavailable"))
        assertFalse(FcmPushErrors.isTokenNotFound(503, "unavailable"))
    }
}
