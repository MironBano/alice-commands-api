package ru.appforsale.alicecommands.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.appforsale.alicecommands.api.deps
import ru.appforsale.alicecommands.api.domain.ApiError
import ru.appforsale.alicecommands.api.domain.push.PushPreferencesRequest
import ru.appforsale.alicecommands.api.domain.push.PushRegisterRequest
import ru.appforsale.alicecommands.api.domain.push.PushUnregisterRequest
import ru.appforsale.alicecommands.api.infrastructure.security.ClientIpResolver

fun Route.pushRoutes() {
    route("/v1/push") {
        post("/register") {
            val deps = call.application.deps
            val ip = ClientIpResolver.resolve(call)
            val body = call.receive<PushRegisterRequest>()
            if (isPushRateLimited(deps, ip, body.installId, call)) return@post
            try {
                val result = deps.registerPushTokenUseCase.execute(body)
                recordPushSubmission(deps, ip, body.installId)
                if (result.tokenStale) {
                    call.response.headers.append(HEADER_PUSH_TOKEN_STALE, "1")
                }
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("validation_failed", e.message ?: "invalid"),
                )
            }
        }

        patch("/preferences") {
            val deps = call.application.deps
            val ip = ClientIpResolver.resolve(call)
            val body = call.receive<PushPreferencesRequest>()
            if (isPushRateLimited(deps, ip, body.installId, call)) return@patch
            try {
                deps.updatePushPreferencesUseCase.execute(body)
                recordPushSubmission(deps, ip, body.installId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                val status = when (e.message) {
                    "not_registered" -> HttpStatusCode.NotFound
                    else -> HttpStatusCode.BadRequest
                }
                val code = when (e.message) {
                    "not_registered" -> "not_registered"
                    else -> "validation_failed"
                }
                call.respond(
                    status,
                    ApiError(code, e.message ?: "invalid"),
                )
            }
        }

        delete("/unregister") {
            val deps = call.application.deps
            val ip = ClientIpResolver.resolve(call)
            val body = call.receive<PushUnregisterRequest>()
            if (isPushRateLimited(deps, ip, body.installId, call)) return@delete
            try {
                deps.unregisterPushTokenUseCase.execute(body)
                recordPushSubmission(deps, ip, body.installId)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("validation_failed", e.message ?: "invalid"),
                )
            }
        }
    }
}

internal const val HEADER_PUSH_TOKEN_STALE = "X-Push-Token-Stale"

private suspend fun isPushRateLimited(
    deps: ru.appforsale.alicecommands.api.AppDependencies,
    ip: String,
    installId: String,
    call: io.ktor.server.application.ApplicationCall,
): Boolean {
    if (deps.publicSubmissionRateLimiter.isBlocked(ip)) {
        call.respond(HttpStatusCode.TooManyRequests, ApiError("rate_limited", "Too many push requests"))
        return true
    }
    if (installId.isBlank()) return false
    if (deps.pushInstallRateLimiter.isBlocked(installId)) {
        call.respond(
            HttpStatusCode.TooManyRequests,
            ApiError("rate_limited", "Too many push requests for install"),
        )
        return true
    }
    return false
}

private fun recordPushSubmission(
    deps: ru.appforsale.alicecommands.api.AppDependencies,
    ip: String,
    installId: String,
) {
    deps.publicSubmissionRateLimiter.recordSubmission(ip)
    if (installId.isNotBlank()) {
        deps.pushInstallRateLimiter.recordSubmission(installId)
    }
}
