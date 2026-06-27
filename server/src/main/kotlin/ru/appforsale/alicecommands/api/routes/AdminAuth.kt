package ru.appforsale.alicecommands.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import ru.appforsale.alicecommands.api.deps
import ru.appforsale.alicecommands.api.domain.ApiError

const val SESSION_COOKIE = "alice_admin_session"

suspend fun ApplicationCall.withAdminAuth(block: suspend ApplicationCall.() -> Unit) {
    val signed = request.cookies[SESSION_COOKIE]
    val deps = application.deps
    val sessionId = signed?.let { deps.sessionSigner.verify(it) }
    if (sessionId == null || !deps.sessionRepository.isValid(sessionId)) {
        respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", "Login required"))
        return
    }
    deps.sessionRepository.touch(sessionId)
    block()
}

fun ApplicationCall.resolveSessionId(): String? {
    val signed = request.cookies[SESSION_COOKIE] ?: return null
    return application.deps.sessionSigner.verify(signed)
}
