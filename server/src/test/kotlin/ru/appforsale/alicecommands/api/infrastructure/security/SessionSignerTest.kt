package ru.appforsale.alicecommands.api.infrastructure.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SessionSignerTest {

    @Test
    fun `sign and verify roundtrip`() {
        val signer = SessionSigner("test-secret-at-least-32-characters-long")
        val signed = signer.sign("abc-123-session")
        assertNotNull(signer.verify(signed))
        assertEquals("abc-123-session", signer.verify(signed))
    }

    @Test
    fun `tampered cookie is rejected`() {
        val signer = SessionSigner("test-secret-at-least-32-characters-long")
        val signed = signer.sign("session-id")
        assertNull(signer.verify(signed + "x"))
        assertNull(signer.verify("forged.signature"))
    }
}
