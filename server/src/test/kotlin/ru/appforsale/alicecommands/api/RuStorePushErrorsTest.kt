package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.infrastructure.push.PushDeliveryQuarantine
import ru.appforsale.alicecommands.api.infrastructure.push.RuStorePushErrors

class RuStorePushErrorsTest {

    @Test
    fun tokenNotFound_requires404AndBody() {
        val body = """{"error":{"code":404,"message":"Requested entity was not found.","status":"NOT_FOUND"}}"""
        assertTrue(RuStorePushErrors.isTokenNotFound(404, body))
        assertEquals(RuStorePushErrors.TOKEN_NOT_FOUND, RuStorePushErrors.errorMessage(404, body))
        assertFalse(RuStorePushErrors.isTokenNotFound(404, """{"error":"project missing"}"""))
        assertEquals("rustore_push_http_404", RuStorePushErrors.errorMessage(404, """{"error":"project missing"}"""))
        assertFalse(RuStorePushErrors.isTokenNotFound(500, body))
    }

    @Test
    fun quarantine_allowsSmallTokenNotFoundCounts() {
        assertTrue(PushDeliveryQuarantine.shouldApply(tokenNotFoundCount = 4, sendAttempts = 4))
        assertTrue(PushDeliveryQuarantine.shouldApply(tokenNotFoundCount = 4, sendAttempts = 50))
    }

    @Test
    fun quarantine_skipsMassFailureStorm() {
        assertFalse(PushDeliveryQuarantine.shouldApply(tokenNotFoundCount = 50, sendAttempts = 50))
        assertFalse(PushDeliveryQuarantine.shouldApply(tokenNotFoundCount = 45, sendAttempts = 50))
        assertTrue(PushDeliveryQuarantine.shouldApply(tokenNotFoundCount = 20, sendAttempts = 50))
    }
}
