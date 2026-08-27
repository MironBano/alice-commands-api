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
import ru.appforsale.alicecommands.api.domain.AnalyticsBatchRequest
import ru.appforsale.alicecommands.api.domain.ApiError
import ru.appforsale.alicecommands.api.infrastructure.security.ClientIpResolver

fun Route.analyticsRoutes() {
    route("/v1/analytics") {
        post("/events/batch") {
            val deps = call.application.deps
            val contentLength = call.request.headers["Content-Length"]?.toLongOrNull()
            if (contentLength != null && contentLength > deps.config.analyticsMaxBodyBytes) {
                call.respond(
                    HttpStatusCode.PayloadTooLarge,
                    ApiError("payload_too_large", "Request body exceeds analytics limit"),
                )
                return@post
            }
            val ip = ClientIpResolver.resolve(call)
            try {
                val body = call.receive<AnalyticsBatchRequest>()
                val result = deps.submitAnalyticsBatchUseCase.execute(ip, body)
                call.respond(HttpStatusCode.Accepted, result)
            } catch (_: RateLimitException) {
                call.respond(HttpStatusCode.TooManyRequests, ApiError("rate_limited", "Too many analytics submissions"))
            }
        }
    }
}
