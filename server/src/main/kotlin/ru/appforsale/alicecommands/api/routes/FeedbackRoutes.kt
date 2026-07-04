package ru.appforsale.alicecommands.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.appforsale.alicecommands.api.application.feedback.RateLimitException
import ru.appforsale.alicecommands.api.deps
import ru.appforsale.alicecommands.api.domain.ApiError
import ru.appforsale.alicecommands.api.domain.ReportCommandIssueRequest
import ru.appforsale.alicecommands.api.domain.SubmitFeedbackRequest
import ru.appforsale.alicecommands.api.infrastructure.security.ClientIpResolver

fun Route.feedbackRoutes() {
    route("/v1") {
        post("/feedback") {
            val deps = call.application.deps
            val ip = ClientIpResolver.resolve(call)
            try {
                val body = call.receive<SubmitFeedbackRequest>()
                val result = deps.submitFeedbackUseCase.execute(ip, body)
                call.respond(HttpStatusCode.Created, result)
            } catch (_: RateLimitException) {
                call.respond(HttpStatusCode.TooManyRequests, ApiError("rate_limited", "Too many submissions"))
            }
        }

        post("/commands/{command_id}/report") {
            val deps = call.application.deps
            val ip = ClientIpResolver.resolve(call)
            val commandId = call.parameters["command_id"]
            if (commandId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiError("validation_failed", "command_id required"))
                return@post
            }
            try {
                val body = call.receive<ReportCommandIssueRequest>()
                val result = deps.reportCommandIssueUseCase.execute(ip, commandId, body)
                call.respond(HttpStatusCode.Created, result)
            } catch (_: RateLimitException) {
                call.respond(HttpStatusCode.TooManyRequests, ApiError("rate_limited", "Too many submissions"))
            }
        }
    }
}
