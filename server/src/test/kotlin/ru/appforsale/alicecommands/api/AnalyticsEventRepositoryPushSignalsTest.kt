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
import ru.appforsale.alicecommands.api.domain.AnalyticsEventDto
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedAnalyticsEventRepository
import ru.appforsale.alicecommands.api.infrastructure.persistence.initDatabase
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsEventRepositoryPushSignalsTest {

    private lateinit var postgres: PostgreSQLContainer<*>
    private lateinit var repository: ExposedAnalyticsEventRepository

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
        repository = ExposedAnalyticsEventRepository(database)
    }

    @Test
    fun loadPushUserSignals_scopesSessionStartFromAppInstalledAt() {
        val installId = UUID.randomUUID().toString()
        val appInstalledAt = OffsetDateTime.of(2026, 9, 10, 0, 0, 0, 0, ZoneOffset.UTC)
        val beforeInstall = appInstalledAt.minusDays(1)
        val afterInstall = appInstalledAt.plusHours(2)
        val dayStartUtc = OffsetDateTime.of(2026, 9, 10, 0, 0, 0, 0, ZoneOffset.UTC)

        repository.insertBatchIgnoreDuplicates(
            clientIp = "127.0.0.1",
            events = listOf(
                sessionStart(installId, beforeInstall),
                sessionStart(installId, afterInstall),
            ),
        )

        val signals = repository.loadPushUserSignals(
            installId = installId,
            dayStartUtc = dayStartUtc,
            appInstalledAt = appInstalledAt,
        )

        assertEquals(1, signals.sessionStartCount)
        assertFalse(signals.hasFirstValueTts)
    }

    private fun sessionStart(installId: String, occurredAt: OffsetDateTime): AnalyticsEventDto =
        AnalyticsEventDto(
            eventId = UUID.randomUUID().toString(),
            installId = installId,
            sessionId = UUID.randomUUID().toString(),
            eventName = "session_start",
            occurredAt = occurredAt.toInstant().toEpochMilli(),
            appVersion = "1.0.1",
            androidVersion = "14",
            locale = "ru",
            userProperties = emptyMap(),
            params = emptyMap(),
        )
}
