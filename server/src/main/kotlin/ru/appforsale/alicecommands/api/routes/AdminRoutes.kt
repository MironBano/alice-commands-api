package ru.appforsale.alicecommands.api.routes

import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import ru.appforsale.alicecommands.api.application.publish.ImportJsonUseCase
import ru.appforsale.alicecommands.api.deps
import ru.appforsale.alicecommands.api.domain.AffiliateBlock
import ru.appforsale.alicecommands.api.domain.ApiError
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.ChecklistItem
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.ScenarioTemplate
import ru.appforsale.alicecommands.api.infrastructure.security.ClientIpResolver
import ru.appforsale.alicecommands.api.plugins.PasswordHasher
import java.time.Instant

fun Route.adminRoutes() {
    route("/admin/api") {
        post("/login") {
            val deps = call.application.deps
            val ip = ClientIpResolver.resolve(call)
            if (deps.loginRateLimiter.isBlocked(ip)) {
                call.respond(HttpStatusCode.TooManyRequests, ApiError("rate_limited", "Too many failed login attempts"))
                return@post
            }
            val body = call.receive<LoginRequest>()
            val valid = body.username == deps.config.adminUsername &&
                PasswordHasher.verify(body.password, deps.config.adminPassword)
            if (!valid) {
                deps.loginRateLimiter.recordFailure(ip)
                call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "Invalid credentials"))
                return@post
            }
            deps.loginRateLimiter.clearFailures(ip)
            deps.sessionRepository.cleanupExpired()
            val sessionId = deps.sessionRepository.createSession()
            val signed = deps.sessionSigner.sign(sessionId)
            call.response.cookies.append(
                Cookie(
                    name = SESSION_COOKIE,
                    value = signed,
                    httpOnly = true,
                    secure = deps.config.isProduction,
                    path = "/",
                    extensions = mapOf("SameSite" to "Lax"),
                ),
            )
            call.respond(mapOf("ok" to true))
        }

        post("/logout") {
            val sessionId = call.resolveSessionId()
            if (sessionId != null) call.application.deps.sessionRepository.invalidate(sessionId)
            call.response.cookies.append(Cookie(name = SESSION_COOKIE, value = "", maxAge = 0, path = "/"))
            call.respond(mapOf("ok" to true))
        }

        get("/dashboard") {
            call.withAdminAuth {
                val deps = application.deps
                respond(
                    DashboardResponse(
                        live = deps.manifestRepository.getCurrent(),
                        draft = deps.draftRepository.stats(),
                        hasUnpublishedChanges = deps.draftPublishStatusService.hasUnpublishedChanges(),
                    ),
                )
            }
        }

        route("/categories") {
            get {
                call.withAdminAuth { respond(application.deps.draftRepository.listCategories()) }
            }
            post {
                call.withAdminAuth {
                    val category = receive<Category>()
                    application.deps.draftRepository.createCategory(category)
                    respond(HttpStatusCode.Created, category)
                }
            }
            put("/reorder") {
                call.withAdminAuth {
                    val body = receive<ReorderRequest>()
                    application.deps.draftRepository.reorderCategories(body.ordered_ids)
                    respond(mapOf("ok" to true))
                }
            }
            put("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    val category = receive<Category>()
                    if (category.id != id) {
                        return@withAdminAuth respond(HttpStatusCode.BadRequest, ApiError("validation_failed", "id mismatch"))
                    }
                    if (application.deps.draftRepository.getCategory(id) == null) {
                        return@withAdminAuth respond(HttpStatusCode.NotFound, ApiError("not_found", "Category not found"))
                    }
                    application.deps.draftRepository.updateCategory(category)
                    respond(category)
                }
            }
            delete("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    application.deps.draftRepository.deleteCategory(id)
                    respond(mapOf("ok" to true))
                }
            }
        }

        route("/commands") {
            get {
                call.withAdminAuth {
                    val categoryId = request.queryParameters["category_id"]
                    respond(application.deps.draftRepository.listCommands(categoryId))
                }
            }
            post {
                call.withAdminAuth {
                    val command = receive<Command>()
                    application.deps.draftRepository.createCommand(command)
                    respond(HttpStatusCode.Created, command)
                }
            }
            put("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    val command = receive<Command>().copy(id = id, updated_at = Instant.now().toString())
                    if (application.deps.draftRepository.getCommand(id) == null) {
                        return@withAdminAuth respond(HttpStatusCode.NotFound, ApiError("not_found", "Command not found"))
                    }
                    application.deps.draftRepository.updateCommand(command)
                    respond(command)
                }
            }
            delete("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    application.deps.draftRepository.deleteCommand(id)
                    respond(mapOf("ok" to true))
                }
            }
        }

        route("/scenario-templates") {
            get {
                call.withAdminAuth { respond(application.deps.draftRepository.listScenarioTemplates()) }
            }
            post {
                call.withAdminAuth {
                    val template = receive<ScenarioTemplate>()
                    application.deps.draftRepository.createScenarioTemplate(template)
                    respond(HttpStatusCode.Created, template)
                }
            }
            put("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    val template = receive<ScenarioTemplate>().copy(id = id)
                    if (application.deps.draftRepository.getScenarioTemplate(id) == null) {
                        return@withAdminAuth respond(HttpStatusCode.NotFound, ApiError("not_found", "Template not found"))
                    }
                    application.deps.draftRepository.updateScenarioTemplate(template)
                    respond(template)
                }
            }
            delete("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    application.deps.draftRepository.deleteScenarioTemplate(id)
                    respond(mapOf("ok" to true))
                }
            }
        }

        get("/checklist-items") {
            call.withAdminAuth { respond(application.deps.draftRepository.listChecklistItems()) }
        }
        put("/checklist-items") {
            call.withAdminAuth {
                val items = receive<List<ChecklistItem>>()
                application.deps.draftRepository.updateChecklistItems(items)
                respond(items)
            }
        }

        route("/affiliate-blocks") {
            get {
                call.withAdminAuth { respond(application.deps.draftRepository.listAffiliateBlocks()) }
            }
            post {
                call.withAdminAuth {
                    val block = receive<AffiliateBlock>()
                    application.deps.draftRepository.createAffiliateBlock(block)
                    respond(HttpStatusCode.Created, block)
                }
            }
            put("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    val block = receive<AffiliateBlock>().copy(id = id)
                    if (application.deps.draftRepository.getAffiliateBlock(id) == null) {
                        return@withAdminAuth respond(HttpStatusCode.NotFound, ApiError("not_found", "Block not found"))
                    }
                    application.deps.draftRepository.updateAffiliateBlock(block)
                    respond(block)
                }
            }
            delete("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    application.deps.draftRepository.deleteAffiliateBlock(id)
                    respond(mapOf("ok" to true))
                }
            }
        }

        get("/preview/bundle") {
            call.withAdminAuth {
                respond(application.deps.previewBundleUseCase.execute())
            }
        }

        post("/publish") {
            call.withAdminAuth {
                val deps = application.deps
                val body = runCatching { receive<PublishRequest>() }.getOrElse { PublishRequest() }
                val result = deps.publishContentUseCase.execute(
                    adminUser = deps.config.adminUsername,
                    minAppVersion = body.min_app_version ?: "1.0",
                    notes = body.notes,
                )
                respond(result)
            }
        }

        post("/publish/rollback") {
            call.withAdminAuth {
                val deps = application.deps
                val body = receive<RollbackRequest>()
                val result = deps.rollbackPublishUseCase.execute(body.content_version, deps.config.adminUsername)
                respond(result)
            }
        }

        get("/publish/history") {
            call.withAdminAuth { respond(application.deps.manifestRepository.listHistory(5)) }
        }

        post("/import/json") {
            call.withAdminAuth {
                val modeParam = request.queryParameters["mode"] ?: "merge"
                val mode = when (modeParam.lowercase()) {
                    "replace" -> ImportJsonUseCase.Mode.REPLACE
                    else -> ImportJsonUseCase.Mode.MERGE
                }
                application.deps.importJsonUseCase.execute(receiveText(), mode)
                respond(mapOf("ok" to true))
            }
        }

        post("/import/preview") {
            call.withAdminAuth {
                val diff = application.deps.contentDiffService.previewImport(receiveText())
                respond(diff)
            }
        }

        get("/content/pipeline") {
            call.withAdminAuth {
                val deps = application.deps
                val seedPath = deps.config.contentSeedPath
                val seedInfo = seedPath?.toFile()?.takeIf { it.isFile }?.let { file ->
                    ContentSeedInfo(
                        path = seedPath.toString(),
                        exists = true,
                        sizeBytes = file.length(),
                        lastModified = Instant.ofEpochMilli(file.lastModified()).toString(),
                    )
                } ?: ContentSeedInfo(
                    path = seedPath?.toString(),
                    exists = false,
                    sizeBytes = null,
                    lastModified = null,
                )
                respond(
                    ContentPipelineResponse(
                        seed = seedInfo,
                        localScript = "scripts/update-content.ps1",
                        pushScript = "scripts/push-draft.ps1 -Mode merge",
                        verifyScript = "scripts/verify-staging.ps1",
                    ),
                )
            }
        }

        post("/content/import-seed") {
            call.withAdminAuth {
                val deps = application.deps
                val path = deps.config.contentSeedPath
                    ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "CONTENT_SEED_PATH is not configured on server"),
                    )
                val file = path.toFile()
                if (!file.isFile) {
                    return@withAdminAuth respond(
                        HttpStatusCode.NotFound,
                        ApiError("not_found", "Seed file not found: ${path}"),
                    )
                }
                val modeParam = request.queryParameters["mode"] ?: "merge"
                val mode = when (modeParam.lowercase()) {
                    "replace" -> ImportJsonUseCase.Mode.REPLACE
                    else -> ImportJsonUseCase.Mode.MERGE
                }
                deps.importJsonUseCase.execute(file.readText(), mode)
                respond(mapOf("ok" to true, "path" to path.toString(), "mode" to modeParam))
            }
        }

        get("/docs") {
            call.withAdminAuth {
                respond(adminApiDocs(application.deps.config.publicBaseUrl))
            }
        }
    }
}

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class ReorderRequest(val ordered_ids: List<String>)

@Serializable
data class PublishRequest(val min_app_version: String? = null, val notes: String? = null)

@Serializable
data class RollbackRequest(val content_version: Int)

@Serializable
data class DashboardResponse(
    val live: ru.appforsale.alicecommands.api.domain.CurrentManifest?,
    val draft: ru.appforsale.alicecommands.api.domain.DraftStats,
    val hasUnpublishedChanges: Boolean,
)

@Serializable
data class ContentSeedInfo(
    val path: String?,
    val exists: Boolean,
    val sizeBytes: Long?,
    val lastModified: String?,
)

@Serializable
data class ContentPipelineResponse(
    val seed: ContentSeedInfo,
    val localScript: String,
    val pushScript: String,
    val verifyScript: String,
)

@Serializable
data class ApiDocEndpoint(
    val method: String,
    val path: String,
    val summary: String,
    val auth: Boolean = true,
    val body: String? = null,
    val response: String? = null,
)

@Serializable
data class ApiDocSection(
    val title: String,
    val description: String? = null,
    val endpoints: List<ApiDocEndpoint>,
)

@Serializable
data class ApiDocsResponse(
    val version: String,
    val baseUrl: String,
    val sections: List<ApiDocSection>,
)

private fun adminApiDocs(baseUrl: String): ApiDocsResponse = ApiDocsResponse(
    version = "v1",
    baseUrl = baseUrl,
    sections = listOf(
        ApiDocSection(
            title = "Public",
            description = "Доступны без авторизации. Android app использует /v1/content/*.",
            endpoints = listOf(
                ApiDocEndpoint("GET", "/health", "Процесс жив", auth = false, response = """{ "status": "ok" }"""),
                ApiDocEndpoint("GET", "/ready", "DB + storage готовы", auth = false, response = """{ "status": "ready", "database": "ok", "storage": "ok" }"""),
                ApiDocEndpoint("GET", "/v1/content/manifest", "Манифест опубликованного bundle", auth = false),
                ApiDocEndpoint("GET", "/v1/content/bundle", "Gzip bundle (published)", auth = false),
                ApiDocEndpoint("GET", "/v1/affiliate/blocks", "Affiliate blocks", auth = false),
            ),
        ),
        ApiDocSection(
            title = "Auth",
            endpoints = listOf(
                ApiDocEndpoint("POST", "/admin/api/login", "Вход", auth = false, body = """{ "username", "password" }"""),
                ApiDocEndpoint("POST", "/admin/api/logout", "Выход", auth = false),
            ),
        ),
        ApiDocSection(
            title = "Dashboard & Draft",
            endpoints = listOf(
                ApiDocEndpoint("GET", "/admin/api/dashboard", "Live + draft stats + hasUnpublishedChanges"),
                ApiDocEndpoint("GET", "/admin/api/preview/bundle", "Draft JSON без gzip"),
            ),
        ),
        ApiDocSection(
            title = "CRUD",
            endpoints = listOf(
                ApiDocEndpoint("GET", "/admin/api/categories", "Список категорий"),
                ApiDocEndpoint("POST", "/admin/api/categories", "Создать категорию"),
                ApiDocEndpoint("PUT", "/admin/api/categories/{id}", "Обновить категорию"),
                ApiDocEndpoint("PUT", "/admin/api/categories/reorder", "Reorder", body = """{ "ordered_ids": [...] }"""),
                ApiDocEndpoint("DELETE", "/admin/api/categories/{id}", "Удалить"),
                ApiDocEndpoint("GET", "/admin/api/commands?category_id=", "Список команд"),
                ApiDocEndpoint("POST", "/admin/api/commands", "Создать команду"),
                ApiDocEndpoint("PUT", "/admin/api/commands/{id}", "Обновить команду"),
                ApiDocEndpoint("DELETE", "/admin/api/commands/{id}", "Удалить"),
                ApiDocEndpoint("GET", "/admin/api/scenario-templates", "Список шаблонов"),
                ApiDocEndpoint("POST", "/admin/api/scenario-templates", "Создать шаблон"),
                ApiDocEndpoint("PUT", "/admin/api/scenario-templates/{id}", "Обновить шаблон"),
                ApiDocEndpoint("DELETE", "/admin/api/scenario-templates/{id}", "Удалить"),
                ApiDocEndpoint("GET", "/admin/api/checklist-items", "Список чеклиста"),
                ApiDocEndpoint("PUT", "/admin/api/checklist-items", "Batch replace/reorder", body = "array of ChecklistItem"),
                ApiDocEndpoint("GET", "/admin/api/affiliate-blocks", "Affiliate blocks"),
                ApiDocEndpoint("POST", "/admin/api/affiliate-blocks", "Создать блок"),
                ApiDocEndpoint("PUT", "/admin/api/affiliate-blocks/{id}", "Обновить блок"),
                ApiDocEndpoint("DELETE", "/admin/api/affiliate-blocks/{id}", "Удалить"),
            ),
        ),
        ApiDocSection(
            title = "Publish & Import",
            endpoints = listOf(
                ApiDocEndpoint("POST", "/admin/api/publish", "Publish draft", body = """{ "min_app_version"?, "notes"? }"""),
                ApiDocEndpoint("POST", "/admin/api/publish/rollback", "Rollback", body = """{ "content_version": 41 }"""),
                ApiDocEndpoint("GET", "/admin/api/publish/history", "Последние 5 публикаций"),
                ApiDocEndpoint("POST", "/admin/api/import/json?mode=merge|replace", "Raw JSON body"),
                ApiDocEndpoint("POST", "/admin/api/import/preview", "Diff vs published"),
                ApiDocEndpoint("GET", "/admin/api/content/pipeline", "Статус seed на сервере"),
                ApiDocEndpoint("POST", "/admin/api/content/import-seed?mode=merge|replace", "Import seed с диска сервера"),
            ),
        ),
    ),
)
