package ru.appforsale.alicecommands.api.plugins

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import ru.appforsale.alicecommands.api.domain.ApiError
import ru.appforsale.alicecommands.api.domain.ValidationException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("validation_failed", cause.message ?: "Validation failed", cause.errors),
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("validation_failed", cause.message ?: "Invalid request"),
            )
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ApiError("not_found", cause.message ?: "Not found"))
        }
        exception<SecurityException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ApiError("unauthorized", cause.message ?: "Unauthorized"))
        }
        exception<io.ktor.server.plugins.BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("validation_failed", cause.message ?: "Invalid request body"),
            )
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("internal_error", "Internal server error"))
        }
    }
}

object PasswordHasher {
    fun verify(plain: String, stored: String): Boolean {
        if (stored.startsWith("$2")) {
            return BCrypt.verifyer().verify(plain.toCharArray(), stored).verified
        }
        return plain == stored
    }
}
