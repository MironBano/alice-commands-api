package ru.appforsale.alicecommands.api.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.application.publish.DraftCommandMerge
import ru.appforsale.alicecommands.api.application.publish.ImportJsonUseCase
import ru.appforsale.alicecommands.api.deps
import ru.appforsale.alicecommands.api.domain.AffiliateBlock
import ru.appforsale.alicecommands.api.domain.ApiError
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.ChecklistItem
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.BulkAssignGroupRequest
import ru.appforsale.alicecommands.api.domain.ContentQueueItemDto
import ru.appforsale.alicecommands.api.domain.EditorialBatchSaveRequest
import ru.appforsale.alicecommands.api.domain.PipelineSyncPayload
import ru.appforsale.alicecommands.api.domain.QueueActionRequest
import ru.appforsale.alicecommands.api.domain.ScenarioTemplate
import ru.appforsale.alicecommands.api.domain.UploadIconRequest
import ru.appforsale.alicecommands.api.infrastructure.security.ClientIpResolver
import ru.appforsale.alicecommands.api.plugins.PasswordHasher
import java.net.URI
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
                        inbox = deps.feedbackInboxCountsUseCase.execute(),
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

        route("/icons") {
            get("/catalog") {
                call.withAdminAuth {
                    respond(application.deps.iconCatalogService.loadCatalog())
                }
            }
            post("/upload") {
                call.withAdminAuth {
                    val request = receive<UploadIconRequest>()
                    respond(application.deps.uploadIconUseCase.execute(request))
                }
            }
        }

        route("/command-groups") {
            get {
                call.withAdminAuth {
                    val categoryId = request.queryParameters["category_id"]
                    respond(application.deps.draftRepository.listCommandGroups(categoryId))
                }
            }
            post {
                call.withAdminAuth {
                    val group = receive<CommandGroup>()
                    application.deps.draftRepository.createCommandGroup(group)
                    respond(HttpStatusCode.Created, group)
                }
            }
            put("/reorder") {
                call.withAdminAuth {
                    val body = receive<ReorderRequest>()
                    application.deps.draftRepository.reorderCommandGroups(body.ordered_ids)
                    respond(mapOf("ok" to true))
                }
            }
            put("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    val group = receive<CommandGroup>()
                    if (group.id != id) {
                        return@withAdminAuth respond(HttpStatusCode.BadRequest, ApiError("validation_failed", "id mismatch"))
                    }
                    if (application.deps.draftRepository.getCommandGroup(id) == null) {
                        return@withAdminAuth respond(HttpStatusCode.NotFound, ApiError("not_found", "Command group not found"))
                    }
                    application.deps.draftRepository.updateCommandGroup(group)
                    respond(group)
                }
            }
            delete("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    application.deps.draftRepository.deleteCommandGroup(id)
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
            put("/bulk-assign-group") {
                call.withAdminAuth {
                    val body = receive<BulkAssignGroupRequest>()
                    application.deps.draftRepository.bulkAssignCommandsToGroup(body.command_ids, body.group_id)
                    respond(mapOf("ok" to true, "updated" to body.command_ids.size))
                }
            }
            put("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    val incoming = receive<Command>().copy(id = id, updated_at = Instant.now().toString())
                    val existing = application.deps.draftRepository.getCommand(id)
                        ?: return@withAdminAuth respond(HttpStatusCode.NotFound, ApiError("not_found", "Command not found"))
                    val command = DraftCommandMerge.fromAdminPut(existing, incoming)
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
                    val block = normalizeAffiliateBlock(receive())
                    validateAffiliateBlock(block)?.let { error ->
                        return@withAdminAuth respond(HttpStatusCode.BadRequest, error)
                    }
                    application.deps.draftRepository.createAffiliateBlock(block)
                    application.deps.publishAffiliateUseCase.execute()
                    respond(HttpStatusCode.Created, block)
                }
            }
            put("/{id}") {
                call.withAdminAuth {
                    val id = parameters["id"] ?: return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "id required"),
                    )
                    val block = normalizeAffiliateBlock(receive<AffiliateBlock>().copy(id = id))
                    validateAffiliateBlock(block)?.let { error ->
                        return@withAdminAuth respond(HttpStatusCode.BadRequest, error)
                    }
                    if (application.deps.draftRepository.getAffiliateBlock(id) == null) {
                        return@withAdminAuth respond(HttpStatusCode.NotFound, ApiError("not_found", "Block not found"))
                    }
                    application.deps.draftRepository.updateAffiliateBlock(block)
                    application.deps.publishAffiliateUseCase.execute()
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
                    application.deps.publishAffiliateUseCase.execute()
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
                val modeParam = request.queryParameters["mode"] ?: "sync"
                val mode = when (modeParam.lowercase()) {
                    "replace" -> ImportJsonUseCase.Mode.REPLACE
                    "merge" -> ImportJsonUseCase.Mode.MERGE
                    else -> ImportJsonUseCase.Mode.SYNC
                }
                application.deps.importJsonUseCase.execute(receiveText(), mode)
                respond(ImportJsonResponse(mode = modeParam))
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
                val live = deps.manifestRepository.getCurrent()
                val draftStats = deps.draftRepository.stats()
                val needsReviewCount = deps.draftRepository.listCommands()
                    .count { "needs_review" in it.tags }
                val pipelineStats = deps.syncPipelineUseCase.status()
                respond(
                    ContentPipelineResponse(
                        seed = seedInfo,
                        live = ContentPipelineLiveInfo(
                            contentVersion = live?.contentVersion,
                            publishedAt = live?.publishedAt,
                        ),
                        draft = ContentPipelineDraftInfo(
                            categoriesCount = draftStats.categoriesCount,
                            commandsCount = draftStats.commandsCount,
                            scenarioTemplatesCount = draftStats.scenarioTemplatesCount,
                            checklistItemsCount = draftStats.checklistItemsCount,
                            needsReviewCount = needsReviewCount,
                        ),
                        pipeline = pipelineStats,
                        hasUnpublishedChanges = deps.draftPublishStatusService.hasUnpublishedChanges(),
                        adminUrl = "${deps.config.publicBaseUrl}/admin",
                        localScript = "scripts/update-content.ps1",
                        pushScript = "scripts/push-draft.ps1 -Mode sync",
                        verifyScript = "scripts/verify-staging.ps1",
                        shortcutsScript = "scripts/desktop/1-Obnovit-katalog.bat",
                        guidePath = "docs/ADMIN-CONTENT-GUIDE.md",
                    ),
                )
            }
        }

        get("/content/draft-diff") {
            call.withAdminAuth {
                respond(application.deps.contentDiffService.draftVsPublished())
            }
        }

        get("/content/validation-warnings") {
            call.withAdminAuth {
                val deps = application.deps
                val draft = deps.draftRepository.loadFull()
                val base = deps.commandGroupValidationUseCase.collectWarnings(draft)
                val settings = deps.draftRepository.getCommandOfDaySettings()
                val cod = deps.commandOfDayValidationUseCase.collectWarnings(draft, settings)
                respond(deps.commandOfDayValidationUseCase.mergeWarnings(base, cod))
            }
        }

        route("/command-of-day") {
            get {
                call.withAdminAuth {
                    respond(application.deps.commandOfDayAdminUseCase.get())
                }
            }
            put {
                call.withAdminAuth {
                    val body = receive<ru.appforsale.alicecommands.api.domain.UpdateCommandOfDayRequest>()
                    val result = application.deps.commandOfDayAdminUseCase.update(
                        body,
                        application.deps.config.adminUsername,
                    )
                    respond(result)
                }
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
                val modeParam = request.queryParameters["mode"] ?: "sync"
                val mode = when (modeParam.lowercase()) {
                    "replace" -> ImportJsonUseCase.Mode.REPLACE
                    "merge" -> ImportJsonUseCase.Mode.MERGE
                    else -> ImportJsonUseCase.Mode.SYNC
                }
                deps.importJsonUseCase.execute(file.readText(), mode)
                respond(ImportSeedResponse(path = path.toString(), mode = modeParam))
            }
        }

        post("/content/pipeline-sync") {
            call.withAdminAuth {
                val payload = receive<PipelineSyncPayload>()
                application.deps.syncPipelineUseCase.execute(payload)
                respond(mapOf("ok" to true))
            }
        }

        get("/content/queue") {
            call.withAdminAuth {
                val status = request.queryParameters["status"] ?: "open"
                respond(application.deps.contentPipelineRepository.listQueue(status))
            }
        }

        post("/content/queue/{id}/approve") {
            call.withAdminAuth {
                val id = call.parameters["id"] ?: return@withAdminAuth respond(
                    HttpStatusCode.BadRequest,
                    ApiError("validation_failed", "Missing queue item id"),
                )
                val body = receive<QueueActionRequest>()
                application.deps.approveQueueItemUseCase.execute(id, body)
                respond(mapOf("ok" to true))
            }
        }

        post("/content/queue/{id}/dismiss") {
            call.withAdminAuth {
                val id = call.parameters["id"] ?: return@withAdminAuth respond(
                    HttpStatusCode.BadRequest,
                    ApiError("validation_failed", "Missing queue item id"),
                )
                application.deps.dismissQueueItemUseCase.execute(id)
                respond(mapOf("ok" to true))
            }
        }

        post("/content/rebuild-draft") {
            call.withAdminAuth {
                val count = application.deps.rebuildDraftFromPipelineUseCase.execute()
                respond(RebuildDraftResponse(commands_updated = count))
            }
        }

        get("/content/editorial-review") {
            call.withAdminAuth {
                val filter = request.queryParameters["filter"] ?: "review"
                val search = request.queryParameters["search"]
                respond(application.deps.editorialReviewService.review(filter, search))
            }
        }

        get("/content/editorial-export") {
            call.withAdminAuth {
                val filter = request.queryParameters["filter"] ?: "review"
                val search = request.queryParameters["search"]
                val doc = application.deps.editorialReviewService.exportDocument(filter, search)
                val json = BundleCodec.json.encodeToString(doc)
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"editorial-export.json\"",
                )
                call.respondText(json, ContentType.Application.Json)
            }
        }

        post("/content/editorial-import") {
            call.withAdminAuth {
                val result = application.deps.importEditorialReviewUseCase.execute(receiveText())
                respond(result)
            }
        }

        post("/content/editorial/batch") {
            call.withAdminAuth {
                val body = receive<EditorialBatchSaveRequest>()
                val result = application.deps.saveEditorialBatchUseCase.execute(body.records)
                respond(result)
            }
        }

        get("/docs") {
            call.withAdminAuth {
                respond(adminApiDocs(application.deps.config.publicBaseUrl))
            }
        }

        get("/feedback") {
            call.withAdminAuth {
                val status = request.queryParameters["status"]
                val search = request.queryParameters["search"]
                respond(application.deps.listFeedbackUseCase.execute(status, search))
            }
        }

        post("/feedback/{id}/resolve") {
            call.withAdminAuth {
                val id = call.parameters["id"] ?: return@withAdminAuth respond(
                    HttpStatusCode.BadRequest,
                    ApiError("validation_failed", "id required"),
                )
                application.deps.resolveFeedbackUseCase.execute(id)
                respond(mapOf("ok" to true))
            }
        }

        post("/feedback/{id}/dismiss") {
            call.withAdminAuth {
                val id = call.parameters["id"] ?: return@withAdminAuth respond(
                    HttpStatusCode.BadRequest,
                    ApiError("validation_failed", "id required"),
                )
                application.deps.dismissFeedbackUseCase.execute(id)
                respond(mapOf("ok" to true))
            }
        }

        get("/command-reports") {
            call.withAdminAuth {
                val status = request.queryParameters["status"]
                val commandId = request.queryParameters["command_id"]
                val search = request.queryParameters["search"]
                respond(application.deps.listCommandReportsUseCase.execute(status, commandId, search))
            }
        }

        post("/command-reports/{id}/resolve") {
            call.withAdminAuth {
                val id = call.parameters["id"] ?: return@withAdminAuth respond(
                    HttpStatusCode.BadRequest,
                    ApiError("validation_failed", "id required"),
                )
                application.deps.resolveCommandReportUseCase.execute(id)
                respond(mapOf("ok" to true))
            }
        }

        post("/command-reports/{id}/dismiss") {
            call.withAdminAuth {
                val id = call.parameters["id"] ?: return@withAdminAuth respond(
                    HttpStatusCode.BadRequest,
                    ApiError("validation_failed", "id required"),
                )
                application.deps.dismissCommandReportUseCase.execute(id)
                respond(mapOf("ok" to true))
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
data class ImportJsonResponse(val ok: Boolean = true, val mode: String)

@Serializable
data class ImportSeedResponse(val ok: Boolean = true, val path: String, val mode: String)

@Serializable
data class RebuildDraftResponse(val ok: Boolean = true, val commands_updated: Int)

@Serializable
data class RollbackRequest(val content_version: Int)

@Serializable
data class DashboardResponse(
    val live: ru.appforsale.alicecommands.api.domain.CurrentManifest?,
    val draft: ru.appforsale.alicecommands.api.domain.DraftStats,
    val hasUnpublishedChanges: Boolean,
    val inbox: ru.appforsale.alicecommands.api.domain.FeedbackInboxCounts = ru.appforsale.alicecommands.api.domain.FeedbackInboxCounts(),
)

@Serializable
data class ContentSeedInfo(
    val path: String?,
    val exists: Boolean,
    val sizeBytes: Long?,
    val lastModified: String?,
)

@Serializable
data class ContentPipelineLiveInfo(
    val contentVersion: Int? = null,
    val publishedAt: String? = null,
)

@Serializable
data class ContentPipelineDraftInfo(
    val categoriesCount: Int,
    val commandsCount: Int,
    val scenarioTemplatesCount: Int,
    val checklistItemsCount: Int,
    val needsReviewCount: Int,
)

@Serializable
data class ContentPipelineResponse(
    val seed: ContentSeedInfo,
    val live: ContentPipelineLiveInfo,
    val draft: ContentPipelineDraftInfo,
    val pipeline: ru.appforsale.alicecommands.api.domain.PipelineStatusResponse? = null,
    val hasUnpublishedChanges: Boolean,
    val adminUrl: String,
    val localScript: String,
    val pushScript: String,
    val verifyScript: String,
    val shortcutsScript: String,
    val guidePath: String,
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

private fun normalizeAffiliateBlock(block: AffiliateBlock): AffiliateBlock = block.copy(
    id = block.id.trim(),
    context_category_id = block.context_category_id?.trim()?.ifBlank { null },
    title_ru = block.title_ru.trim(),
    erid = block.erid?.trim()?.ifBlank { null },
    advertiser_name = block.advertiser_name?.trim()?.ifBlank { null },
    products = block.products.map { product ->
        product.copy(
            title_ru = product.title_ru.trim(),
            market_url = product.market_url.trim(),
            price_hint = product.price_hint?.trim()?.ifBlank { null },
        )
    },
)

private fun validateAffiliateBlock(block: AffiliateBlock): ApiError? {
    if (block.id.isBlank()) return ApiError("validation_failed", "id required")
    if (block.title_ru.isBlank()) return ApiError("validation_failed", "title_ru required")
    if (block.erid.isNullOrBlank()) return ApiError("validation_failed", "erid required")
    if (block.advertiser_name.isNullOrBlank()) return ApiError("validation_failed", "advertiser_name required")
    if (block.products.isEmpty()) return ApiError("validation_failed", "at least one product required")

    block.products.forEachIndexed { index, product ->
        val productNum = index + 1
        if (product.title_ru.isBlank()) {
            return ApiError("validation_failed", "products[$productNum].title_ru required")
        }
        val marketUrl = product.market_url.trim()
        if (marketUrl.isBlank()) {
            return ApiError("validation_failed", "products[$productNum].market_url required")
        }
        val uri = runCatching { URI(marketUrl) }.getOrNull()
        if (uri?.scheme != "https" || uri.host.isNullOrBlank()) {
            return ApiError("validation_failed", "products[$productNum].market_url must be https URL")
        }
    }

    return null
}

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
                ApiDocEndpoint("GET", "/v1/content/delta?from={version}", "Delta sync между версиями", auth = false),
                ApiDocEndpoint("GET", "/v1/affiliate/blocks", "Affiliate blocks", auth = false),
                ApiDocEndpoint("POST", "/v1/feedback", "In-app feedback", auth = false, body = """{ "message", "rating"?, "app_version"?, "platform"?, "locale"?, "content_version"?, "device_model"? }"""),
                ApiDocEndpoint("POST", "/v1/commands/{command_id}/report", "Report command issue", auth = false, body = """{ "issue_type", "message"?, "content_version"?, ... }"""),
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
                ApiDocEndpoint("GET", "/admin/api/command-groups?category_id=", "Группы команд"),
                ApiDocEndpoint("POST", "/admin/api/command-groups", "Создать группу"),
                ApiDocEndpoint("PUT", "/admin/api/command-groups/{id}", "Обновить группу"),
                ApiDocEndpoint("PUT", "/admin/api/command-groups/reorder", "Reorder групп"),
                ApiDocEndpoint("DELETE", "/admin/api/command-groups/{id}", "Удалить группу"),
                ApiDocEndpoint("PUT", "/admin/api/commands/bulk-assign-group", "Bulk assign group_id"),
                ApiDocEndpoint("GET", "/admin/api/content/validation-warnings", "Orphan/empty group warnings"),
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
                ApiDocEndpoint("GET", "/admin/api/command-of-day", "Команда дня: settings + preview"),
                ApiDocEndpoint("PUT", "/admin/api/command-of-day", "Сохранить draft", body = """{ "mode", "command_id"?, "auto_category_id"?, "auto_seed"? }"""),
                ApiDocEndpoint("GET", "/admin/api/affiliate-blocks", "Affiliate blocks"),
                ApiDocEndpoint("POST", "/admin/api/affiliate-blocks", "Создать блок", body = """{ "id", "title_ru", "erid", "advertiser_name", "products": [{ "title_ru", "market_url": "https://...", "price_hint"? }] }"""),
                ApiDocEndpoint("PUT", "/admin/api/affiliate-blocks/{id}", "Обновить блок", body = """{ "title_ru", "erid", "advertiser_name", "products": [{ "title_ru", "market_url": "https://...", "price_hint"? }] }"""),
                ApiDocEndpoint("DELETE", "/admin/api/affiliate-blocks/{id}", "Удалить"),
            ),
        ),
        ApiDocSection(
            title = "Publish & Import",
            endpoints = listOf(
                ApiDocEndpoint("POST", "/admin/api/publish", "Publish draft", body = """{ "min_app_version"?, "notes"? }"""),
                ApiDocEndpoint("POST", "/admin/api/publish/rollback", "Rollback", body = """{ "content_version": 41 }"""),
                ApiDocEndpoint("GET", "/admin/api/publish/history", "Последние 5 публикаций"),
                ApiDocEndpoint("POST", "/admin/api/import/json?mode=sync|merge|replace", "Raw JSON; sync = catalog + merge approved editorial"),
                ApiDocEndpoint("POST", "/admin/api/import/preview", "Diff vs published"),
                ApiDocEndpoint("GET", "/admin/api/content/pipeline", "Статус pipeline + draft/live"),
                ApiDocEndpoint("POST", "/admin/api/content/pipeline-sync", "Inventory + editorial + queue с ПК"),
                ApiDocEndpoint("GET", "/admin/api/content/queue?status=open", "Очередь editorial"),
                ApiDocEndpoint("POST", "/admin/api/content/queue/{id}/approve", "Approve → editorial"),
                ApiDocEndpoint("POST", "/admin/api/content/queue/{id}/dismiss", "Dismiss queue item"),
                ApiDocEndpoint("POST", "/admin/api/content/rebuild-draft", "Draft из pipeline DB"),
                ApiDocEndpoint("GET", "/admin/api/content/editorial-review?filter=review", "Редактор: все изменения для review"),
                ApiDocEndpoint("GET", "/admin/api/content/editorial-export?filter=review", "Скачать JSON для правки в ИИ"),
                ApiDocEndpoint("POST", "/admin/api/content/editorial-import", "Загрузить JSON после ИИ"),
                ApiDocEndpoint("POST", "/admin/api/content/editorial/batch", "Сохранить правки из UI"),
                ApiDocEndpoint("GET", "/admin/api/content/draft-diff", "Diff draft vs опубликованная версия"),
                ApiDocEndpoint("POST", "/admin/api/content/import-seed?mode=sync|merge|replace", "Import seed с диска сервера"),
            ),
        ),
        ApiDocSection(
            title = "User feedback inbox",
            description = "Обращения из Android app. Без ПДн.",
            endpoints = listOf(
                ApiDocEndpoint("GET", "/admin/api/feedback?status=open&search=", "Список отзывов"),
                ApiDocEndpoint("POST", "/admin/api/feedback/{id}/resolve", "Закрыть отзыв"),
                ApiDocEndpoint("POST", "/admin/api/feedback/{id}/dismiss", "Отклонить отзыв"),
                ApiDocEndpoint("GET", "/admin/api/command-reports?status=open&command_id=&search=", "Сообщения об ошибках команд"),
                ApiDocEndpoint("POST", "/admin/api/command-reports/{id}/resolve", "Закрыть report"),
                ApiDocEndpoint("POST", "/admin/api/command-reports/{id}/dismiss", "Отклонить report"),
            ),
        ),
    ),
)
