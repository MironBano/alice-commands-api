package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import ru.appforsale.alicecommands.api.config.AppConfig
import ru.appforsale.alicecommands.api.domain.push.PushRegisterRequest
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedPushTokenRepository
import ru.appforsale.alicecommands.api.infrastructure.persistence.initDatabase
import ru.appforsale.alicecommands.api.infrastructure.push.RuStorePushErrors
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Repository contract for delivery quarantine without Testcontainers Java client
 * (Docker Desktop 4.x npipe proxy breaks docker-java). Uses a local Postgres on 55432
 * when available (`docker run ... -p 55432:5432 postgres:16-alpine`).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExposedPushTokenRepositoryTest {

    private lateinit var repository: ExposedPushTokenRepository

    @BeforeAll
    fun setup() {
        assumeTrue(isPostgresReachable("127.0.0.1", 55432), "Postgres on :55432 unavailable — skip")
        System.setProperty("APP_ENV", "local")
        System.setProperty("DATABASE_URL", "jdbc:postgresql://127.0.0.1:55432/alice_commands")
        System.setProperty("DATABASE_USER", "alice")
        System.setProperty("DATABASE_PASSWORD", "alice_dev")
        System.setProperty("SESSION_SECRET", "test-session-secret-32chars-minimum")
        System.setProperty("ADMIN_USERNAME", "admin")
        System.setProperty("ADMIN_PASSWORD", "test-password")
        System.setProperty("BUNDLE_STORAGE_PATH", "./storage/bundles")
        System.setProperty("MANIFEST_STORAGE_PATH", "./storage/manifest")
        val database = initDatabase(AppConfig.load())
        repository = ExposedPushTokenRepository(database)
    }

    @AfterAll
    fun cleanup() {
        if (!::repository.isInitialized) return
        repository.delete("repo-block-install")
        repository.delete("repo-active-install")
    }

    @Test
    fun markDeliveryBlocked_excludesFromListActive_untilNewToken() {
        val installId = "repo-block-install"
        val oldToken = "tok-old"
        val newToken = "tok-new"
        repository.delete(installId)

        val first = repository.upsertRegister(register(installId, oldToken))
        assertFalse(first.tokenStale)
        assertEquals(1, repository.listActive().count { it.installId == installId })

        repository.markDeliveryBlocked(installId, RuStorePushErrors.TOKEN_NOT_FOUND)
        assertEquals(0, repository.listActive().count { it.installId == installId })

        val same = repository.upsertRegister(register(installId, oldToken))
        assertTrue(same.tokenStale)
        assertEquals(0, repository.listActive().count { it.installId == installId })

        val rotated = repository.upsertRegister(register(installId, newToken))
        assertFalse(rotated.tokenStale)
        val active = repository.listActive().first { it.installId == installId }
        assertEquals(newToken, active.rustoreToken)
        assertNull(active.deliveryBlockedReason)
    }

    @Test
    fun listActive_requiresMasterEnabled() {
        val installId = "repo-active-install"
        repository.delete(installId)
        repository.upsertRegister(register(installId, "tok-a", masterEnabled = true))
        assertEquals(1, repository.listActive().count { it.installId == installId })
        repository.upsertRegister(register(installId, "tok-a", masterEnabled = false))
        assertEquals(0, repository.listActive().count { it.installId == installId })
    }

    private fun register(
        installId: String,
        token: String,
        masterEnabled: Boolean = true,
    ) = PushRegisterRequest(
        installId = installId,
        rustoreToken = token,
        timezone = "Europe/Moscow",
        persona = "NEW_SPEAKER",
        contentVersion = 1,
        masterEnabled = masterEnabled,
        codEnabled = false,
        codReminderTime = "09:00",
        checklistCompletedCount = 0,
        frequentCommands = emptyList(),
        appVersion = "1.0.7",
        appInstalledAt = "2026-07-13T10:00:00Z",
    )

    private fun isPostgresReachable(host: String, port: Int): Boolean =
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 1_000)
            }
            true
        }.getOrDefault(false)
}
