package ru.appforsale.alicecommands.api

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import ru.appforsale.alicecommands.api.config.AppConfig
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.routes.LoginRequest
import java.nio.file.Files
import kotlin.io.path.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiIntegrationTest {

    private lateinit var postgres: PostgreSQLContainer<*>

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
    }

    private fun testEnv(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) {
        val storageRoot = Files.createTempDirectory("alice-test-storage")
        val bundlePath = storageRoot.resolve("bundles")
        val manifestPath = storageRoot.resolve("manifest")
        Files.createDirectories(bundlePath)
        Files.createDirectories(manifestPath)

        System.setProperty("APP_ENV", "local")
        System.setProperty("APP_PORT", "0")
        System.setProperty("PUBLIC_BASE_URL", "http://localhost:8080")
        System.setProperty("ADMIN_USERNAME", "admin")
        System.setProperty("ADMIN_PASSWORD", "test-password")
        System.setProperty("DATABASE_URL", postgres.jdbcUrl)
        System.setProperty("DATABASE_USER", postgres.username)
        System.setProperty("DATABASE_PASSWORD", postgres.password)
        System.setProperty("BUNDLE_STORAGE_PATH", bundlePath.toString())
        System.setProperty("MANIFEST_STORAGE_PATH", manifestPath.toString())
        System.setProperty("SESSION_SECRET", "test-session-secret-32chars-min")

        val config = AppConfig.load()

        testApplication {
            application { module(config) }
            block()
        }
    }

    @Test
    fun `health and ready endpoints`() = testEnv {
        val health = client.get("/health")
        assertEquals(HttpStatusCode.OK, health.status)
        assertTrue(health.bodyAsText().contains("ok"))

        val ready = client.get("/ready")
        assertEquals(HttpStatusCode.OK, ready.status)
        assertTrue(ready.bodyAsText().contains("ready"))
    }

    @Test
    fun `admin routes require signed session`() = testEnv {
        val unauthorized = client.get("/admin/api/dashboard")
        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)

        val login = client.post("/admin/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin", "test-password"))
        }
        assertEquals(HttpStatusCode.OK, login.status)

        val dashboard = client.get("/admin/api/dashboard")
        assertEquals(HttpStatusCode.OK, dashboard.status)
        assertTrue(dashboard.bodyAsText().contains("draft"))
    }

    @Test
    fun `import publish and public manifest bundle flow`() = testEnv {
        val seed = Path("seed/import-smart-home.json").toFile().readText()

        val login = client.post("/admin/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin", "test-password"))
        }
        assertEquals(HttpStatusCode.OK, login.status)

        val importRes = client.post("/admin/api/import/json?mode=replace") {
            contentType(ContentType.Application.Json)
            setBody(seed)
        }
        assertEquals(HttpStatusCode.OK, importRes.status)

        val publish = client.post("/admin/api/publish") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, publish.status)

        val manifestRes = client.get("/v1/content/manifest")
        assertEquals(HttpStatusCode.OK, manifestRes.status)
        val manifestText = manifestRes.bodyAsText()
        assertTrue(manifestText.contains("content_version"))
        val etag = manifestRes.headers[HttpHeaders.ETag]
        assertTrue(etag?.startsWith("\"content-") == true)

        val notModified = client.get("/v1/content/manifest") {
            header(HttpHeaders.IfNoneMatch, etag!!)
        }
        assertEquals(HttpStatusCode.NotModified, notModified.status)

        val bundleRes = client.get("/v1/content/bundle")
        assertEquals(HttpStatusCode.OK, bundleRes.status)
        assertEquals("gzip", bundleRes.headers[HttpHeaders.ContentEncoding])
        val bundleBytes = bundleRes.bodyAsBytes()
        assertTrue(bundleBytes.isNotEmpty())

        val manifest = Json.decodeFromString<ru.appforsale.alicecommands.api.domain.ManifestResponse>(manifestText)
        assertEquals(manifest.bundle_sha256, BundleCodec.sha256(bundleBytes))

        val preview = client.get("/admin/api/preview/bundle")
        assertEquals(HttpStatusCode.OK, preview.status)
        assertTrue(preview.bodyAsText().contains("smart_home"))
    }

    @Test
    fun `import preview diff vs published bundle`() = testEnv {
        val seed = Path("seed/import-smart-home.json").toFile().readText()
        val fullCatalog = Path("seed/full-catalog.json").toFile().readText()

        client.post("/admin/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin", "test-password"))
        }

        client.post("/admin/api/import/json?mode=replace") {
            contentType(ContentType.Application.Json)
            setBody(seed)
        }
        client.post("/admin/api/publish") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        val emptyDiff = client.post("/admin/api/import/preview") {
            contentType(ContentType.Application.Json)
            setBody(seed)
        }
        assertEquals(HttpStatusCode.OK, emptyDiff.status)
        assertTrue(emptyDiff.bodyAsText().contains("\"unchanged\""))

        val catalogDiff = client.post("/admin/api/import/preview") {
            contentType(ContentType.Application.Json)
            setBody(fullCatalog)
        }
        assertEquals(HttpStatusCode.OK, catalogDiff.status)
        val body = catalogDiff.bodyAsText()
        assertTrue(body.contains("published_v"))
        assertTrue(body.contains("\"added\""))
        assertTrue(body.contains("commands"))
    }
}
