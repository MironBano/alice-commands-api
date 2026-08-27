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
import ru.appforsale.alicecommands.api.domain.DeviceGuide
import ru.appforsale.alicecommands.api.domain.DevicePick
import ru.appforsale.alicecommands.api.domain.UploadDeviceImageRequest
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

        route("/smarthome") {
            post("/upload-image") {
                call.withAdminAuth {
                    val request = receive<UploadDeviceImageRequest>()
                    respond(application.deps.uploadDeviceImageUseCase.execute(request))
                }
            }
            route("/device-guides") {
                get {
                    call.withAdminAuth { respond(application.deps.draftRepository.listDeviceGuides()) }
                }
                post {
                    call.withAdminAuth {
                        val guide = normalizeDeviceGuide(receive())
                        validateDeviceGuide(guide, application.deps)?.let { error ->
                            return@withAdminAuth respond(HttpStatusCode.BadRequest, error)
                        }
                        application.deps.draftRepository.createDeviceGuide(guide)
                        application.deps.publishSmartHomeDevicesUseCase.execute()
                        respond(HttpStatusCode.Created, guide)
                    }
                }
                put("/{id}") {
                    call.withAdminAuth {
                        val id = parameters["id"] ?: return@withAdminAuth respond(
                            HttpStatusCode.BadRequest,
                            ApiError("validation_failed", "id required"),
                        )
                        val guide = normalizeDeviceGuide(receive<DeviceGuide>().copy(id = id))
                        validateDeviceGuide(guide, application.deps)?.let { error ->
                            return@withAdminAuth respond(HttpStatusCode.BadRequest, error)
                        }
                        if (application.deps.draftRepository.getDeviceGuide(id) == null) {
                            return@withAdminAuth respond(HttpStatusCode.NotFound, ApiError("not_found", "Guide not found"))
                        }
                        application.deps.draftRepository.updateDeviceGuide(guide)
                        application.deps.publishSmartHomeDevicesUseCase.execute()
                        respond(guide)
                    }
                }
                delete("/{id}") {
                    call.withAdminAuth {
                        val id = parameters["id"] ?: return@withAdminAuth respond(
                            HttpStatusCode.BadRequest,
                            ApiError("validation_failed", "id required"),
                        )
                        application.deps.draftRepository.deleteDeviceGuide(id)
                        application.deps.publishSmartHomeDevicesUseCase.execute()
                        respond(mapOf("ok" to true))
                    }
                }
            }
            route("/device-picks") {
                get {
                    call.withAdminAuth { respond(application.deps.draftRepository.listDevicePicks()) }
                }
                post {
                    call.withAdminAuth {
                        val pick = normalizeDevicePick(receive())
                        validateDevicePick(pick, application.deps)?.let { error ->
                            return@withAdminAuth respond(HttpStatusCode.BadRequest, error)
                        }
                        application.deps.draftRepository.createDevicePick(pick)
                        application.deps.publishSmartHomeDevicesUseCase.execute()
                        respond(HttpStatusCode.Created, pick)
                    }
                }
                put("/{id}") {
                    call.withAdminAuth {
                        val id = parameters["id"] ?: return@withAdminAuth respond(
                            HttpStatusCode.BadRequest,
                            ApiError("validation_failed", "id required"),
                        )
                        val pick = normalizeDevicePick(receive<DevicePick>().copy(id = id))
                        validateDevicePick(pick, application.deps)?.let { error ->
                            return@withAdminAuth respond(HttpStatusCode.BadRequest, error)
                        }
                        if (application.deps.draftRepository.getDevicePick(id) == null) {
                            return@withAdminAuth respond(HttpStatusCode.NotFound, ApiError("not_found", "Pick not found"))
                        }
                        application.deps.draftRepository.updateDevicePick(pick)
                        application.deps.publishSmartHomeDevicesUseCase.execute()
                        respond(pick)
                    }
                }
                delete("/{id}") {
                    call.withAdminAuth {
                        val id = parameters["id"] ?: return@withAdminAuth respond(
                            HttpStatusCode.BadRequest,
                            ApiError("validation_failed", "id required"),
                        )
                        application.deps.draftRepository.deleteDevicePick(id)
                        application.deps.publishSmartHomeDevicesUseCase.execute()
                        respond(mapOf("ok" to true))
                    }
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
                val modeParam = request.queryParameters["mode"] ?: "replace"
                val mode = when (modeParam.lowercase()) {
                    "replace" -> ImportJsonUseCase.Mode.REPLACE
                    "merge" -> ImportJsonUseCase.Mode.MERGE
                    "sync" -> return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "Import mode 'sync' removed; use replace (canon) or merge"),
                    )
                    else -> return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "Unknown import mode: $modeParam"),
                    )
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
                        pipeline = null,
                        hasUnpublishedChanges = deps.draftPublishStatusService.hasUnpublishedChanges(),
                        adminUrl = "${deps.config.publicBaseUrl}/admin",
                        localScript = "scripts/push-draft.ps1",
                        pushScript = "scripts/push-draft.ps1",
                        verifyScript = "scripts/verify-staging.ps1",
                        shortcutsScript = "scripts/desktop/3-Pull-catalog.bat",
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
                    respond(application.deps.enrichCommandOfDayAdmin(application.deps.commandOfDayAdminUseCase.get()))
                }
            }
            put {
                call.withAdminAuth {
                    val body = receive<ru.appforsale.alicecommands.api.domain.UpdateCommandOfDayRequest>()
                    val result = application.deps.commandOfDayAdminUseCase.update(
                        body,
                        application.deps.config.adminUsername,
                    )
                    respond(application.deps.enrichCommandOfDayAdmin(result))
                }
            }
            post("/publish") {
                call.withAdminAuth {
                    val body = runCatching { receive<CommandOfDayPublishRequest>() }.getOrElse { CommandOfDayPublishRequest() }
                    val result = application.deps.publishCommandOfDayUseCase.execute(
                        application.deps.config.adminUsername,
                        body.notes,
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
                val modeParam = request.queryParameters["mode"] ?: "replace"
                val mode = when (modeParam.lowercase()) {
                    "replace" -> ImportJsonUseCase.Mode.REPLACE
                    "merge" -> ImportJsonUseCase.Mode.MERGE
                    "sync" -> return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "Import mode 'sync' removed; use replace (canon) or merge"),
                    )
                    else -> return@withAdminAuth respond(
                        HttpStatusCode.BadRequest,
                        ApiError("validation_failed", "Unknown import mode: $modeParam"),
                    )
                }
                deps.importJsonUseCase.execute(file.readText(), mode)
                respond(ImportSeedResponse(path = path.toString(), mode = modeParam))
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

        route("/analytics") {
            get("/summary") {
                call.withAdminAuth {
                    val from = request.queryParameters["from"]
                    val to = request.queryParameters["to"]
                    if (from.isNullOrBlank() || to.isNullOrBlank()) {
                        respond(
                            HttpStatusCode.BadRequest,
                            ApiError("validation_failed", "from and to query parameters are required"),
                        )
                        return@withAdminAuth
                    }
                    respond(application.deps.analyticsDashboardUseCase.execute(from, to))
                }
            }

            get("/events") {
                call.withAdminAuth {
                    val from = request.queryParameters["from"]
                    val to = request.queryParameters["to"]
                    if (from.isNullOrBlank() || to.isNullOrBlank()) {
                        respond(
                            HttpStatusCode.BadRequest,
                            ApiError("validation_failed", "from and to query parameters are required"),
                        )
                        return@withAdminAuth
                    }
                    val eventName = request.queryParameters["event_name"]
                    val installId = request.queryParameters["install_id"]
                    val limit = request.queryParameters["limit"]?.toIntOrNull()
                    val offset = request.queryParameters["offset"]?.toIntOrNull()
                    respond(
                        application.deps.listAnalyticsEventsUseCase.execute(
                            from = from,
                            to = to,
                            eventName = eventName,
                            installId = installId,
                            limit = limit,
                            offset = offset,
                        ),
                    )
                }
            }

            get("/funnel") {
                call.withAdminAuth {
                    val from = request.queryParameters["from"]
                    val to = request.queryParameters["to"]
                    if (from.isNullOrBlank() || to.isNullOrBlank()) {
                        respond(
                            HttpStatusCode.BadRequest,
                            ApiError("validation_failed", "from and to query parameters are required"),
                        )
                        return@withAdminAuth
                    }
                    respond(
                        application.deps.analyticsFunnelUseCase.execute(
                            from = from,
                            to = to,
                            stepsRaw = request.queryParameters["steps"],
                        ),
                    )
                }
            }

            get("/breakdown") {
                call.withAdminAuth {
                    val from = request.queryParameters["from"]
                    val to = request.queryParameters["to"]
                    if (from.isNullOrBlank() || to.isNullOrBlank()) {
                        respond(
                            HttpStatusCode.BadRequest,
                            ApiError("validation_failed", "from and to query parameters are required"),
                        )
                        return@withAdminAuth
                    }
                    respond(
                        application.deps.analyticsBreakdownUseCase.execute(
                            from = from,
                            to = to,
                            eventName = request.queryParameters["event_name"],
                            param = request.queryParameters["param"],
                            limit = request.queryParameters["limit"]?.toIntOrNull(),
                            fieldSourceRaw = request.queryParameters["field_source"],
                        ),
                    )
                }
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
data class CommandOfDayPublishRequest(val notes: String? = null)

private fun ru.appforsale.alicecommands.api.AppDependencies.enrichCommandOfDayAdmin(
    response: ru.appforsale.alicecommands.api.domain.CommandOfDayAdminResponse,
): ru.appforsale.alicecommands.api.domain.CommandOfDayAdminResponse =
    response.copy(
        has_unpublished_changes = draftPublishStatusService.hasUnpublishedCommandOfDayChanges(),
        live_content_version = manifestRepository.getCurrent()?.contentVersion,
        live_command_of_day = publishedBundleLookup.loadCurrentBundle()?.command_of_day,
    )

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

private fun normalizeDeviceGuide(guide: DeviceGuide): DeviceGuide = guide.copy(
    id = guide.id.trim(),
    title_ru = guide.title_ru.trim(),
    summary_ru = guide.summary_ru.trim(),
    capabilities_ru = guide.capabilities_ru.trim(),
    setup_ru = guide.setup_ru.trim(),
    setup_steps_ru = guide.setup_steps_ru.map { it.trim() }.filter { it.isNotEmpty() },
    related_devices_ru = guide.related_devices_ru?.trim()?.ifBlank { null },
    related_device_ids = guide.related_device_ids.map { it.trim() }.filter { it.isNotEmpty() },
    command_device_filter_id = guide.command_device_filter_id?.trim()?.ifBlank { null },
    image_url = guide.image_url?.trim()?.ifBlank { null },
    action_url = guide.action_url.trim(),
)

private fun normalizeDevicePick(pick: DevicePick): DevicePick = pick.copy(
    id = pick.id.trim(),
    title_ru = pick.title_ru.trim(),
    description_ru = pick.description_ru?.trim()?.ifBlank { null },
    price_hint_ru = pick.price_hint_ru?.trim()?.ifBlank { null },
    image_url = pick.image_url?.trim()?.ifBlank { null },
    action_url = pick.action_url.trim(),
    erid = pick.erid?.trim()?.ifBlank { null },
    advertiser_name = pick.advertiser_name?.trim()?.ifBlank { null },
    disclosure_ru = pick.disclosure_ru?.trim()?.ifBlank { null },
    cta_ru = pick.cta_ru?.trim()?.ifBlank { null },
    tags = pick.tags.map { it.trim() }.filter { it.isNotEmpty() },
    device_types = pick.device_types.map { it.trim() }.filter { it.isNotEmpty() },
    category_ids = pick.category_ids.map { it.trim() }.filter { it.isNotEmpty() },
    command_group_ids = pick.command_group_ids.map { it.trim() }.filter { it.isNotEmpty() },
    command_ids = pick.command_ids.map { it.trim() }.filter { it.isNotEmpty() },
    scenario_template_ids = pick.scenario_template_ids.map { it.trim() }.filter { it.isNotEmpty() },
    guide_ids = pick.guide_ids.map { it.trim() }.filter { it.isNotEmpty() },
    placements = pick.placements.map { it.trim() }.filter { it.isNotEmpty() },
    starts_at = pick.starts_at?.trim()?.ifBlank { null },
    ends_at = pick.ends_at?.trim()?.ifBlank { null },
)

private fun validateDeviceGuide(guide: DeviceGuide, deps: ru.appforsale.alicecommands.api.AppDependencies): ApiError? {
    val errors = deps.smartHomeDevicesValidationUseCase.validateGuide(guide)
    if (errors.isNotEmpty()) {
        return ApiError("validation_failed", errors.first(), errors)
    }
    guide.related_device_ids.forEach { relatedId ->
        if (relatedId != guide.id && deps.draftRepository.getDeviceGuide(relatedId) == null) {
            return ApiError("validation_failed", "related_device_ids: unknown guide '$relatedId'")
        }
    }
    return null
}

private fun validateDevicePick(pick: DevicePick, deps: ru.appforsale.alicecommands.api.AppDependencies): ApiError? {
    val errors = deps.smartHomeDevicesValidationUseCase.validatePick(pick)
    return if (errors.isNotEmpty()) {
        ApiError("validation_failed", errors.first(), errors)
    } else {
        null
    }
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
                ApiDocEndpoint("GET", "/v1/affiliate/blocks", "Affiliate blocks (deprecated)", auth = false),
                ApiDocEndpoint("GET", "/v1/smarthome/devices", "Smart home guides + picks", auth = false),
                ApiDocEndpoint("POST", "/v1/feedback", "In-app feedback", auth = false, body = """{ "message", "rating"?, "app_version"?, "platform"?, "locale"?, "content_version"?, "device_model"? }"""),
                ApiDocEndpoint("POST", "/v1/commands/{command_id}/report", "Report command issue", auth = false, body = """{ "issue_type", "message"?, "content_version"?, ... }"""),
                ApiDocEndpoint("POST", "/v1/analytics/events/batch", "Analytics batch ingest", auth = false, body = """{ "events": [{ "installId", "sessionId", "eventId", "eventName", "occurredAt", ... }] }""", response = """202 { "accepted", "duplicates", "rejected", "rejectedEventIds" }"""),
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
                ApiDocEndpoint("POST", "/admin/api/command-of-day/publish", "Опубликовать только command_of_day в live bundle"),
                ApiDocEndpoint("GET", "/admin/api/affiliate-blocks", "Affiliate blocks"),
                ApiDocEndpoint("POST", "/admin/api/affiliate-blocks", "Создать блок", body = """{ "id", "title_ru", "erid"?, "advertiser_name"?, "products": [{ "title_ru", "market_url": "https://...", "price_hint"? }] }"""),
                ApiDocEndpoint("PUT", "/admin/api/affiliate-blocks/{id}", "Обновить блок", body = """{ "title_ru", "erid"?, "advertiser_name"?, "products": [{ "title_ru", "market_url": "https://...", "price_hint"? }] }"""),
                ApiDocEndpoint("DELETE", "/admin/api/affiliate-blocks/{id}", "Удалить"),
                ApiDocEndpoint("GET", "/admin/api/smarthome/device-guides", "Типы устройств (guides)"),
                ApiDocEndpoint("POST", "/admin/api/smarthome/device-guides", "Создать guide"),
                ApiDocEndpoint("PUT", "/admin/api/smarthome/device-guides/{id}", "Обновить guide"),
                ApiDocEndpoint("DELETE", "/admin/api/smarthome/device-guides/{id}", "Удалить guide"),
                ApiDocEndpoint("GET", "/admin/api/smarthome/device-picks", "Подборки (picks)"),
                ApiDocEndpoint("POST", "/admin/api/smarthome/device-picks", "Создать pick"),
                ApiDocEndpoint("PUT", "/admin/api/smarthome/device-picks/{id}", "Обновить pick"),
                ApiDocEndpoint("DELETE", "/admin/api/smarthome/device-picks/{id}", "Удалить pick"),
                ApiDocEndpoint("POST", "/admin/api/smarthome/upload-image", "Загрузить image на CDN", body = """{ "slug", "image_base64", "content_type"? }"""),
            ),
        ),
        ApiDocSection(
            title = "Publish & Import",
            endpoints = listOf(
                ApiDocEndpoint("POST", "/admin/api/publish", "Publish draft", body = """{ "min_app_version"?, "notes"? }"""),
                ApiDocEndpoint("POST", "/admin/api/publish/rollback", "Rollback", body = """{ "content_version": 41 }"""),
                ApiDocEndpoint("GET", "/admin/api/publish/history", "Последние 5 публикаций"),
                ApiDocEndpoint("POST", "/admin/api/import/json?mode=replace", "Import bundle JSON → draft (replace)"),
                ApiDocEndpoint("POST", "/admin/api/import/preview", "Diff vs published"),
                ApiDocEndpoint("GET", "/admin/api/content/pipeline", "Статус draft vs live"),
                ApiDocEndpoint("GET", "/admin/api/content/draft-diff", "Diff draft vs опубликованная версия"),
                ApiDocEndpoint("POST", "/admin/api/content/import-seed?mode=replace", "Import seed с диска сервера"),
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
        ApiDocSection(
            title = "Analytics",
            description = "Продуктовые события из Android. Без PII в params.",
            endpoints = listOf(
                ApiDocEndpoint("GET", "/admin/api/analytics/summary?from=YYYY-MM-DD&to=YYYY-MM-DD", "KPI: DAU, total events, top events, daily series"),
                ApiDocEndpoint("GET", "/admin/api/analytics/events?from=&to=&event_name=&install_id=&limit=100&offset=0", "Список raw событий"),
                ApiDocEndpoint("GET", "/admin/api/analytics/funnel?from=&to=&steps=paywall_view,pro_purchase_start,pro_activated", "Воронка по distinct install_id"),
                ApiDocEndpoint("GET", "/admin/api/analytics/breakdown?from=&to=&event_name=ui_click&param=element_id", "Top values параметра события"),
            ),
        ),
    ),
)
