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
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
        System.setProperty("DEVICE_IMAGE_STORAGE_PATH", storageRoot.resolve("devices").toString())
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
        val altCatalog = TestResourcePaths.readText("seed/smart-home-groups-v2.json")

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
            setBody(altCatalog)
        }
        assertEquals(HttpStatusCode.OK, catalogDiff.status)
        val body = catalogDiff.bodyAsText()
        assertTrue(body.contains("published_v"))
        assertTrue(body.contains("\"added\""))
        assertTrue(body.contains("commands"))
    }

    @Test
    fun `content status and draft diff endpoints`() = testEnv {
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
        assertTrue(pipelineBody.contains("push-draft.ps1"))
        assertTrue(pipelineBody.contains("\"pipeline\":null"))

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
    fun `analytics batch public and admin flow`() = testEnv {
        val installId = "11111111-1111-4111-8111-111111111111"
        val sessionId = "22222222-2222-4222-8222-222222222222"
        val eventId = "33333333-3333-4333-8333-333333333333"
        val occurredAt = System.currentTimeMillis()
        val today = java.time.LocalDate.now().toString()

        val emptyBatch = client.post("/v1/analytics/events/batch") {
            contentType(ContentType.Application.Json)
            setBody("""{"events":[]}""")
        }
        assertEquals(HttpStatusCode.BadRequest, emptyBatch.status)

        val batchBody = """
            {
              "events": [
                {
                  "installId": "$installId",
                  "sessionId": "$sessionId",
                  "eventId": "$eventId",
                  "eventName": "screen_view",
                  "occurredAt": $occurredAt,
                  "appVersion": "1.2.0",
                  "androidVersion": "14",
                  "locale": "ru-RU",
                  "userProperties": { "is_pro": "false" },
                  "params": { "route": "home/catalog" }
                }
              ]
            }
        """.trimIndent()

        val accepted = client.post("/v1/analytics/events/batch") {
            contentType(ContentType.Application.Json)
            setBody(batchBody)
        }
        assertEquals(HttpStatusCode.Accepted, accepted.status)
        assertTrue(accepted.bodyAsText().contains("\"accepted\":1"))
        assertTrue(accepted.bodyAsText().contains("\"duplicates\":0"))

        val duplicate = client.post("/v1/analytics/events/batch") {
            contentType(ContentType.Application.Json)
            setBody(batchBody)
        }
        assertEquals(HttpStatusCode.Accepted, duplicate.status)
        assertTrue(duplicate.bodyAsText().contains("\"duplicates\":1"))

        val mixedBatch = client.post("/v1/analytics/events/batch") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "events": [
                    {
                      "installId": "$installId",
                      "sessionId": "$sessionId",
                      "eventId": "44444444-4444-4444-8444-444444444444",
                      "eventName": "INVALID",
                      "occurredAt": $occurredAt
                    },
                    {
                      "installId": "$installId",
                      "sessionId": "$sessionId",
                      "eventId": "55555555-5555-4555-8555-555555555555",
                      "eventName": "daily_active",
                      "occurredAt": $occurredAt
                    }
                  ]
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Accepted, mixedBatch.status)
        assertTrue(mixedBatch.bodyAsText().contains("\"rejected\":1"))
        assertTrue(mixedBatch.bodyAsText().contains("\"accepted\":1"))

        val unauthorizedSummary = client.get("/admin/api/analytics/summary?from=$today&to=$today")
        assertEquals(HttpStatusCode.Unauthorized, unauthorizedSummary.status)

        client.post("/admin/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin", "test-password"))
        }

        val summary = client.get("/admin/api/analytics/summary?from=$today&to=$today")
        assertEquals(HttpStatusCode.OK, summary.status)
        assertTrue(summary.bodyAsText().contains("total_events"))
        assertTrue(summary.bodyAsText().contains("top_events"))
        assertTrue(summary.bodyAsText().contains("raw_unique_installs"))
        assertTrue(summary.bodyAsText().contains("new_installs"))
        assertTrue(summary.bodyAsText().contains("\"daily\""))
        assertTrue(summary.bodyAsText().contains("avg_dau"))
        assertTrue(summary.bodyAsText().contains("days_in_range"))

        val tooLongFrom = java.time.LocalDate.now().minusDays(90).toString()
        val tooLong = client.get("/admin/api/analytics/summary?from=$tooLongFrom&to=$today")
        assertEquals(HttpStatusCode.BadRequest, tooLong.status)

        val events = client.get("/admin/api/analytics/events?from=$today&to=$today&event_name=screen_view")
        assertEquals(HttpStatusCode.OK, events.status)
        assertTrue(events.bodyAsText().contains("screen_view"))
        assertTrue(events.bodyAsText().contains(installId))

        val funnel = client.get(
            "/admin/api/analytics/funnel?from=$today&to=$today&steps=screen_view,daily_active",
        )
        assertEquals(HttpStatusCode.OK, funnel.status)
        assertTrue(funnel.bodyAsText().contains("screen_view"))

        val breakdown = client.get(
            "/admin/api/analytics/breakdown?from=$today&to=$today&event_name=screen_view&param=route",
        )
        assertEquals(HttpStatusCode.OK, breakdown.status)
        assertTrue(breakdown.bodyAsText().contains("home/catalog"))

        val userPropsBreakdown = client.get(
            "/admin/api/analytics/breakdown?from=$today&to=$today&event_name=screen_view&param=is_pro&field_source=user_properties",
        )
        assertEquals(HttpStatusCode.OK, userPropsBreakdown.status)
        assertTrue(userPropsBreakdown.bodyAsText().contains("\"field_source\":\"user_properties\""))
        assertTrue(userPropsBreakdown.bodyAsText().contains("false"))

        val searchBatch = client.post("/v1/analytics/events/batch") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "events": [
                    {
                      "installId": "$installId",
                      "sessionId": "$sessionId",
                      "eventId": "66666666-6666-4666-8666-666666666666",
                      "eventName": "search",
                      "occurredAt": $occurredAt,
                      "params": { "query_length": "5", "results_count": "3" }
                    }
                  ]
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Accepted, searchBatch.status)
        assertTrue(searchBatch.bodyAsText().contains("\"rejected\":0"))
        assertTrue(searchBatch.bodyAsText().contains("\"accepted\":1"))

        val piiBatch = client.post("/v1/analytics/events/batch") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "events": [
                    {
                      "installId": "$installId",
                      "sessionId": "$sessionId",
                      "eventId": "77777777-7777-4777-8777-777777777777",
                      "eventName": "search",
                      "occurredAt": $occurredAt,
                      "params": { "query": "secret" }
                    }
                  ]
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Accepted, piiBatch.status)
        assertTrue(piiBatch.bodyAsText().contains("\"rejected\":1"))
        assertTrue(piiBatch.bodyAsText().contains("\"accepted\":0"))
        assertTrue(piiBatch.bodyAsText().contains("77777777-7777-4777-8777-777777777777"))

        // Simulate client install_id race: 3 install ids, one session_id.
        val raceSession = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        val ghostA = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
        val ghostB = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
        val canonical = "dddddddd-dddd-4ddd-8ddd-dddddddddddd"
        val raceBatch = client.post("/v1/analytics/events/batch") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "events": [
                    {
                      "installId": "$ghostA",
                      "sessionId": "$raceSession",
                      "eventId": "a1111111-1111-4111-8111-111111111111",
                      "eventName": "session_start",
                      "occurredAt": $occurredAt
                    },
                    {
                      "installId": "$ghostB",
                      "sessionId": "$raceSession",
                      "eventId": "a2222222-2222-4222-8222-222222222222",
                      "eventName": "app_foreground",
                      "occurredAt": $occurredAt
                    },
                    {
                      "installId": "$canonical",
                      "sessionId": "$raceSession",
                      "eventId": "a3333333-3333-4333-8333-333333333333",
                      "eventName": "daily_active",
                      "occurredAt": $occurredAt
                    },
                    {
                      "installId": "$canonical",
                      "sessionId": "$raceSession",
                      "eventId": "a4444444-4444-4444-8444-444444444444",
                      "eventName": "screen_view",
                      "occurredAt": $occurredAt,
                      "params": { "route": "home/catalog" }
                    },
                    {
                      "installId": "$canonical",
                      "sessionId": "$raceSession",
                      "eventId": "a5555555-5555-4555-8555-555555555555",
                      "eventName": "ui_click",
                      "occurredAt": $occurredAt,
                      "params": { "element_id": "tab_catalog" }
                    }
                  ]
                }
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.Accepted, raceBatch.status)

        val summaryAfterRace = client.get("/admin/api/analytics/summary?from=$today&to=$today")
        assertEquals(HttpStatusCode.OK, summaryAfterRace.status)
        val summaryJson = Json.parseToJsonElement(summaryAfterRace.bodyAsText()).jsonObject
        // Prior installId + race canonical (ghosts collapsed via session dominance).
        assertEquals(2, summaryJson["unique_installs"]?.jsonPrimitive?.int)
        assertEquals(4, summaryJson["raw_unique_installs"]?.jsonPrimitive?.int)
        assertTrue(summaryJson.containsKey("new_installs"))
        val daily = summaryJson["daily"]?.jsonArray
        assertNotNull(daily)
        assertTrue(daily!!.isNotEmpty())
        val todayPoint = daily.first().jsonObject
        assertTrue(todayPoint.containsKey("new_installs"))
        // All install_ids in this test first appear on $today → period new_installs == raw distinct that day.
        assertEquals(
            todayPoint["unique_installs"]?.jsonPrimitive?.int,
            todayPoint["new_installs"]?.jsonPrimitive?.int,
        )
        assertEquals(
            todayPoint["new_installs"]?.jsonPrimitive?.int,
            summaryJson["new_installs"]?.jsonPrimitive?.int,
        )
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

    @Test
    fun `smart home devices publish and public endpoint`() = testEnv {
        client.post("/admin/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin", "test-password"))
        }

        val guideId = "guide_test_${System.nanoTime()}"
        val pickId = "pick_test_${System.nanoTime()}"
        val createGuide = client.post("/admin/api/smarthome/device-guides") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                  "id": "$guideId",
                  "title_ru": "Тест колонка",
                  "summary_ru": "Кратко",
                  "capabilities_ru": "Полный текст возможностей для detail-screen",
                  "setup_ru": "Полный текст подключения",
                  "setup_steps_ru": ["Шаг 1", "Шаг 2"],
                  "command_device_filter_id": "station",
                  "action_url": "https://alice.yandex.ru/support/ru/station/",
                  "sort_order": 5
                }""",
            )
        }
        assertEquals(HttpStatusCode.Created, createGuide.status)

        val createPick = client.post("/admin/api/smarthome/device-picks") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                  "id": "$pickId",
                  "title_ru": "Тестовая подборка",
                  "description_ru": "Описание",
                  "price_hint_ru": "от 100 ₽",
                  "action_url": "https://market.yandex.ru/test",
                  "sort_order": 1
                }""",
            )
        }
        assertEquals(HttpStatusCode.Created, createPick.status)

        val publicRes = client.get("/v1/smarthome/devices")
        assertEquals(HttpStatusCode.OK, publicRes.status)
        val body = publicRes.bodyAsText()
        assertTrue(body.contains(guideId))
        assertTrue(body.contains(pickId))
        assertTrue(body.contains(""""guides""""))
        assertTrue(body.contains(""""picks""""))

        val deprecatedAffiliate = client.get("/v1/affiliate/blocks")
        assertEquals("true", deprecatedAffiliate.headers["Deprecation"])
        assertTrue(deprecatedAffiliate.headers["Link"]?.contains("/v1/smarthome/devices") == true)
    }
}
