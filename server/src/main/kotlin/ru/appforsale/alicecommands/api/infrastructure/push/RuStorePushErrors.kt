package ru.appforsale.alicecommands.api.infrastructure.push

/**
 * Classifies RuStore Push HTTP errors so campaign quarantine only hits dead tokens,
 * not misconfigured project/URL storms.
 */
object RuStorePushErrors {
    const val TOKEN_NOT_FOUND = "rustore_token_not_found"

    fun errorMessage(statusCode: Int, responseBody: String): String {
        if (isTokenNotFound(statusCode, responseBody)) return TOKEN_NOT_FOUND
        return "rustore_push_http_$statusCode"
    }

    fun isTokenNotFound(statusCode: Int, responseBody: String): Boolean {
        if (statusCode != 404) return false
        val body = responseBody.lowercase()
        return body.contains("\"status\":\"not_found\"") ||
            body.contains("requested entity was not found")
    }
}

/**
 * Defers applying delivery blocks until end of tick so a misconfigured RuStore project
 * cannot mass-quarantine every token in one pass.
 */
object PushDeliveryQuarantine {
    /** Apply blocks only when not-found failures are a minority / small absolute count. */
    fun shouldApply(tokenNotFoundCount: Int, sendAttempts: Int): Boolean {
        if (tokenNotFoundCount <= 0) return false
        if (sendAttempts <= 0) return false
        // Small absolute counts are normal (a few expired tokens).
        if (tokenNotFoundCount < MIN_SUSPECT_COUNT) return true
        val ratio = tokenNotFoundCount.toDouble() / sendAttempts.toDouble()
        return ratio < MASS_FAILURE_RATIO
    }

    private const val MIN_SUSPECT_COUNT = 10
    private const val MASS_FAILURE_RATIO = 0.9
}
