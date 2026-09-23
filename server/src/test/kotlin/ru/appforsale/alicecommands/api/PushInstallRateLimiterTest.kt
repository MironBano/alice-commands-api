package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import ru.appforsale.alicecommands.api.config.AppConfig
import ru.appforsale.alicecommands.api.infrastructure.persistence.initDatabase
import ru.appforsale.alicecommands.api.infrastructure.security.ExposedPushInstallRateLimiter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PushInstallRateLimiterTest {

    private lateinit var postgres: PostgreSQLContainer<*>
    private lateinit var limiter: ExposedPushInstallRateLimiter

    @BeforeAll
    fun startPostgres() {
        assumeTrue(
            DockerClientFactory.instance().isDockerAvailable,
            "Docker недоступен — пропуск интеграционных тестов",
        )
        postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("alice_commands")
            .withUsername("alice")
            .withPassword("alice_dev")
        postgres.start()

        System.setProperty("APP_ENV", "local")
        System.setProperty("DATABASE_URL", postgres.jdbcUrl)
        System.setProperty("DATABASE_USER", postgres.username)
        System.setProperty("DATABASE_PASSWORD", postgres.password)
        System.setProperty("SESSION_SECRET", "test-session-secret-32chars-minimum")
        System.setProperty("ADMIN_USERNAME", "admin")
        System.setProperty("ADMIN_PASSWORD", "test-password")
        System.setProperty("BUNDLE_STORAGE_PATH", "./storage/bundles")
        System.setProperty("MANIFEST_STORAGE_PATH", "./storage/manifest")

        val database = initDatabase(AppConfig.load())
        limiter = ExposedPushInstallRateLimiter(database, maxSubmissions = 2)
    }

    @Test
    fun blocksAfterMaxSubmissionsPerInstall() {
        val installId = "rate-limit-install"
        assertFalse(limiter.isBlocked(installId))
        limiter.recordSubmission(installId)
        assertFalse(limiter.isBlocked(installId))
        limiter.recordSubmission(installId)
        assertTrue(limiter.isBlocked(installId))
    }

    @Test
    fun differentInstallIdsAreIndependent() {
        limiter.recordSubmission("install-a")
        limiter.recordSubmission("install-a")
        assertTrue(limiter.isBlocked("install-a"))
        assertFalse(limiter.isBlocked("install-b"))
    }
}
