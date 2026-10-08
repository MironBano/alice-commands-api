package ru.appforsale.alicecommands.api.infrastructure.push

/**
 * Classifies FCM HTTP v1 errors so campaign quarantine only hits dead tokens.
 */
object FcmPushErrors {
    const val TOKEN_NOT_FOUND = "fcm_token_not_found"

    fun errorMessage(statusCode: Int, responseBody: String): String {
        if (isTokenNotFound(statusCode, responseBody)) return TOKEN_NOT_FOUND
        return "fcm_push_http_$statusCode"
    }

    fun isTokenNotFound(statusCode: Int, responseBody: String): Boolean {
        val body = responseBody.lowercase()
        if (body.contains("unregistered") || body.contains("\"errorcode\":\"unregistered\"")) {
            return true
        }
        if (statusCode == 404) return true
        // INVALID_ARGUMENT with registration-token wording often means dead/malformed token.
        return statusCode == 400 &&
            (body.contains("registration") || body.contains("not a valid fcm"))
    }
}
