package ru.appforsale.alicecommands.api

import io.ktor.client.request.get
import io.ktor.client.request.delete
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
        System.setProperty("ICON_STORAGE_PATH", storageRoot.resolve("icons").toString())
        System.setProperty("ICON_PUBLIC_BASE_URL", "http://localhost:8080")
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
        val seed = TestResourcePaths.readText(TestResourcePaths.INTEGRATION_SEED)

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

        val affiliateId = "affiliate_after_catalog_publish_${System.nanoTime()}"
        val affiliate = client.post("/admin/api/affiliate-blocks") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                  "id": "$affiliateId",
                  "context_category_id": "smart_home",
                  "title_ru": "Affiliate without catalog publish",
                  "erid": "test-erid",
                  "advertiser_name": "ООО Тест",
                  "products": [
                    {
                      "title_ru": "Умная лампа",
                      "market_url": "https://example.com/affiliate-after-publish",
                      "price_hint": "от 990 ₽"
                    }
                  ]
                }""",
            )
        }
        assertEquals(HttpStatusCode.Created, affiliate.status)

        val affiliatePublic = client.get("/v1/affiliate/blocks")
        assertEquals(HttpStatusCode.OK, affiliatePublic.status)
        assertTrue(affiliatePublic.bodyAsText().contains(affiliateId))

        val dashboardAfterAffiliate = client.get("/admin/api/dashboard")
        assertEquals(HttpStatusCode.OK, dashboardAfterAffiliate.status)
        assertTrue(dashboardAfterAffiliate.bodyAsText().contains(""""hasUnpublishedChanges":false"""))
    }

    @Test
    fun `import preview diff vs published bundle`() = testEnv {
        val seed = TestResourcePaths.readText(TestResourcePaths.INTEGRATION_SEED)
        val fullCatalog = TestResourcePaths.readText("seed/full-catalog.json")

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

    @Test
    fun `content pipeline and draft diff endpoints`() = testEnv {
        val seed = TestResourcePaths.readText(TestResourcePaths.INTEGRATION_SEED)

        client.post("/admin/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin", "test-password"))
        }

        client.post("/admin/api/import/json?mode=replace") {
            contentType(ContentType.Application.Json)
            setBody(seed)
        }

        val pipeline = client.get("/admin/api/content/pipeline")
        assertEquals(HttpStatusCode.OK, pipeline.status)
        val pipelineBody = pipeline.bodyAsText()
        assertTrue(pipelineBody.contains("hasUnpublishedChanges"))
        assertTrue(pipelineBody.contains("needsReviewCount"))
        assertTrue(pipelineBody.contains("adminUrl"))
        assertTrue(pipelineBody.contains("inventory_count"))

        val queue = client.get("/admin/api/content/queue?status=open")
        assertEquals(HttpStatusCode.OK, queue.status)

        val syncPayload = """{"inventory":[],"editorial":[],"queue":[]}"""
        val sync = client.post("/admin/api/content/pipeline-sync") {
            contentType(ContentType.Application.Json)
            setBody(syncPayload)
        }
        assertEquals(HttpStatusCode.OK, sync.status)

        val rebuild = client.post("/admin/api/content/rebuild-draft") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, rebuild.status)

        val draftDiff = client.get("/admin/api/content/draft-diff")
        assertEquals(HttpStatusCode.OK, draftDiff.status)
        assertTrue(draftDiff.bodyAsText().contains("commands"))
    }

    @Test
    fun `affiliate blocks require compliant product links`() = testEnv {
        client.post("/admin/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin", "test-password"))
        }

        val invalid = client.post("/admin/api/affiliate-blocks") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                  "id": "test_affiliate_invalid",
                  "context_category_id": "smart_home",
                  "title_ru": "Плохая ссылка",
                  "erid": "test-erid",
                  "advertiser_name": "ООО Тест",
                  "products": [
                    {
                      "title_ru": "Умная лампа",
                      "market_url": "http://example.com/lamp",
                      "price_hint": "от 990 ₽"
                    }
                  ]
                }""",
            )
        }
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
        assertTrue(invalid.bodyAsText().contains("market_url must be https URL"))

        val emptyProducts = client.post("/admin/api/affiliate-blocks") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                  "id": "test_affiliate_empty_products",
                  "context_category_id": "smart_home",
                  "title_ru": "Без товаров",
                  "erid": "test-erid",
                  "advertiser_name": "ООО Тест",
                  "products": []
                }""",
            )
        }
        assertEquals(HttpStatusCode.BadRequest, emptyProducts.status)
        assertTrue(emptyProducts.bodyAsText().contains("at least one product required"))

        val blockId = "test_affiliate_valid_${System.nanoTime()}"
        val valid = client.post("/admin/api/affiliate-blocks") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                  "id": "$blockId",
                  "context_category_id": "smart_home",
                  "title_ru": "С чего начать умный дом",
                  "erid": "test-erid",
                  "advertiser_name": "ООО Тест",
                  "products": [
                    {
                      "title_ru": "Умная лампа",
                      "market_url": "https://example.com/lamp",
                      "price_hint": "от 990 ₽"
                    }
                  ]
                }""",
            )
        }
        assertEquals(HttpStatusCode.Created, valid.status)

        try {
            val publicBlocks = client.get("/v1/affiliate/blocks")
            assertEquals(HttpStatusCode.OK, publicBlocks.status)
            assertTrue(publicBlocks.bodyAsText().contains(blockId))
            assertTrue(publicBlocks.bodyAsText().contains("https://example.com/lamp"))

            val blocks = client.get("/admin/api/affiliate-blocks")
            assertEquals(HttpStatusCode.OK, blocks.status)
            assertTrue(blocks.bodyAsText().contains(blockId))
            assertTrue(blocks.bodyAsText().contains("https://example.com/lamp"))
        } finally {
            client.delete("/admin/api/affiliate-blocks/$blockId")
        }

        val publicAfterDelete = client.get("/v1/affiliate/blocks")
        assertEquals(HttpStatusCode.OK, publicAfterDelete.status)
        assertTrue(!publicAfterDelete.bodyAsText().contains(blockId))
    }

    @Test
    fun `feedback and command report public and admin flow`() = testEnv {
        val seed = TestResourcePaths.readText(TestResourcePaths.INTEGRATION_SEED)

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

        val manifestRes = client.get("/v1/content/manifest")
        assertEquals(HttpStatusCode.OK, manifestRes.status)
        val manifest = Json.decodeFromString<ru.appforsale.alicecommands.api.domain.ManifestResponse>(
            manifestRes.bodyAsText(),
        )

        val feedbackRes = client.post("/v1/feedback") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                  "message": "Отличное приложение!",
                  "rating": 5,
                  "app_version": "1.0.0",
                  "platform": "android",
                  "locale": "ru-RU",
                  "content_version": ${manifest.content_version}
                }""",
            )
        }
        assertEquals(HttpStatusCode.Created, feedbackRes.status)
        assertTrue(feedbackRes.bodyAsText().contains("\"status\":\"open\""))

        val reportRes = client.post("/v1/commands/sh_light_on/report") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                  "issue_type": "wrong_effect",
                  "message": "Эффект не совпадает",
                  "content_version": ${manifest.content_version},
                  "category_id": "smart_home",
                  "command_title": "Включить свет",
                  "phrase_used": "Алиса, включи свет"
                }""",
            )
        }
        assertEquals(HttpStatusCode.Created, reportRes.status)
        assertTrue(reportRes.bodyAsText().contains("command_exists_current"))

        val notFoundRes = client.post("/v1/commands/missing_command/report") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                  "issue_type": "other",
                  "content_version": ${manifest.content_version}
                }""",
            )
        }
        assertEquals(HttpStatusCode.NotFound, notFoundRes.status)

        val unauthorizedFeedback = client.get("/admin/api/feedback")
        assertEquals(HttpStatusCode.Unauthorized, unauthorizedFeedback.status)

        val feedbackList = client.get("/admin/api/feedback?status=open")
        assertEquals(HttpStatusCode.OK, feedbackList.status)
        assertTrue(feedbackList.bodyAsText().contains("Отличное приложение"))

        val reportsList = client.get("/admin/api/command-reports?status=open")
        assertEquals(HttpStatusCode.OK, reportsList.status)
        assertTrue(reportsList.bodyAsText().contains("sh_light_on"))

        val dashboard = client.get("/admin/api/dashboard")
        assertEquals(HttpStatusCode.OK, dashboard.status)
        assertTrue(dashboard.bodyAsText().contains("open_feedback"))
        assertTrue(dashboard.bodyAsText().contains("open_command_reports"))

        val feedbackId = Regex(""""id"\s*:\s*"([^"]+)"""")
            .find(feedbackList.bodyAsText())?.groupValues?.get(1)
        assertTrue(!feedbackId.isNullOrBlank())

        val resolveFeedback = client.post("/admin/api/feedback/$feedbackId/resolve") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, resolveFeedback.status)

        val reportId = Regex(""""id"\s*:\s*"([^"]+)"""")
            .find(reportsList.bodyAsText())?.groupValues?.get(1)
        assertTrue(!reportId.isNullOrBlank())

        val dismissReport = client.post("/admin/api/command-reports/$reportId/dismiss") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, dismissReport.status)
    }

    @Test
    fun `icon upload catalog and static serve`() = testEnv {
        client.post("/admin/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin", "test-password"))
        }

        val svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2L2 7l10 5 10-5-10-5z"/></svg>"""
        val upload = client.post("/admin/api/icons/upload") {
            contentType(ContentType.Application.Json)
            setBody("""{"slug":"test_star","svg":${Json.encodeToString(svg)}}""")
        }
        assertEquals(HttpStatusCode.OK, upload.status)
        assertTrue(upload.bodyAsText().contains("test_star"))
        assertTrue(upload.bodyAsText().contains("/icons/v1/test_star.svg"))

        val catalog = client.get("/admin/api/icons/catalog")
        assertEquals(HttpStatusCode.OK, catalog.status)
        assertTrue(catalog.bodyAsText().contains("test_star"))

        val iconFile = client.get("/icons/v1/test_star.svg")
        assertEquals(HttpStatusCode.OK, iconFile.status)
        assertTrue(iconFile.bodyAsText().contains("<svg"))
    }
}
