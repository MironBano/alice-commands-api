package ru.appforsale.alicecommands.api.application.analytics

import ru.appforsale.alicecommands.api.application.feedback.RateLimitException
import ru.appforsale.alicecommands.api.domain.AnalyticsBatchRequest
import ru.appforsale.alicecommands.api.domain.AnalyticsBatchResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsBreakdownResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsEventDto
import ru.appforsale.alicecommands.api.domain.AnalyticsEventsListResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsFunnelResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsSummaryResponse
import ru.appforsale.alicecommands.api.domain.ValidationException
import org.slf4j.LoggerFactory
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsEventRepository
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsRateLimiter
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

class SubmitAnalyticsBatchUseCase(
    private val repository: AnalyticsEventRepository,
    private val rateLimiter: AnalyticsRateLimiter,
) {
    private val log = LoggerFactory.getLogger(SubmitAnalyticsBatchUseCase::class.java)

    fun execute(clientIp: String, request: AnalyticsBatchRequest): AnalyticsBatchResponse {
        validateBatchSize(request)
        if (rateLimiter.isRequestBlocked(clientIp)) {
            throw RateLimitException()
        }
        if (rateLimiter.isDailyEventCapExceeded(clientIp, request.events.size)) {
            throw RateLimitException()
        }

        val acceptedEvents = mutableListOf<AnalyticsEventDto>()
        val rejectedEventIds = mutableListOf<String>()
        val rejectReasons = mutableMapOf<String, Int>()
        request.events.forEach { event ->
            val reason = validateEventReason(event)
            if (reason == null) {
                acceptedEvents += event
            } else {
                rejectedEventIds += event.eventId
                rejectReasons.merge(reason, 1, Int::plus)
            }
        }
        if (rejectedEventIds.isNotEmpty()) {
            log.info(
                "analytics batch rejected={} reasons={} ip={} batchSize={}",
                rejectedEventIds.size,
                rejectReasons,
                clientIp,
                request.events.size,
            )
        }

        val insertResult = if (acceptedEvents.isNotEmpty()) {
            repository.insertBatchIgnoreDuplicates(clientIp, acceptedEvents)
        } else {
            ru.appforsale.alicecommands.api.domain.ports.AnalyticsInsertResult(0, 0)
        }

        rateLimiter.recordRequest(clientIp)

        return AnalyticsBatchResponse(
            accepted = insertResult.accepted,
            duplicates = insertResult.duplicates,
            rejected = rejectedEventIds.size,
            rejectedEventIds = rejectedEventIds,
        )
    }

    private fun validateBatchSize(request: AnalyticsBatchRequest) {
        val errors = mutableListOf<String>()
        if (request.events.isEmpty()) errors += "events must contain at least 1 item"
        if (request.events.size > MAX_BATCH_SIZE) errors += "events must contain at most $MAX_BATCH_SIZE items"
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    fun validateEvent(event: AnalyticsEventDto): Boolean = validateEventReason(event) == null

    /** Machine-readable reject reason for ops logs (no param values). */
    fun validateEventReason(event: AnalyticsEventDto): String? {
        if (!isUuid(event.installId)) return "invalid_install_id"
        if (!isUuid(event.sessionId)) return "invalid_session_id"
        if (!isUuid(event.eventId)) return "invalid_event_id"
        if (!EVENT_NAME_REGEX.matches(event.eventName)) return "invalid_event_name"
        if (!isValidOccurredAt(event.occurredAt)) return "invalid_occurred_at"
        if (event.appVersion != null && event.appVersion.length > MAX_APP_VERSION) return "invalid_app_version"
        if (event.androidVersion != null && event.androidVersion.length > MAX_ANDROID_VERSION) {
            return "invalid_android_version"
        }
        if (event.locale != null && event.locale.length > MAX_LOCALE) return "invalid_locale"
        if (!isValidStringMap(event.userProperties)) return "invalid_user_properties"
        if (!isValidStringMap(event.params)) return "invalid_params"
        blockedParamKey(event.params)?.let { return "pii_key:$it" }
        blockedParamKey(event.userProperties)?.let { return "pii_user_property:$it" }
        return null
    }

    private fun isUuid(value: String): Boolean =
        runCatching { UUID.fromString(value) }.isSuccess

    private fun isValidOccurredAt(ms: Long): Boolean {
        val now = Instant.now()
        val occurred = Instant.ofEpochMilli(ms)
        val maxFuture = now.plusSeconds(FUTURE_TOLERANCE_SECONDS)
        val minPast = now.minusSeconds(MAX_AGE_DAYS * 24L * 3600)
        return !occurred.isAfter(maxFuture) && !occurred.isBefore(minPast)
    }

    private fun isValidStringMap(map: Map<String, String>): Boolean {
        if (map.size > MAX_MAP_KEYS) return false
        return map.all { (key, value) ->
            key.length <= MAX_MAP_KEY_LENGTH && value.length <= MAX_MAP_VALUE_LENGTH
        }
    }

    private fun blockedParamKey(map: Map<String, String>): String? =
        map.keys.firstOrNull { key -> key.lowercase() in PII_BLOCKLIST }

    companion object {
        const val MAX_BATCH_SIZE = 50
        private const val MAX_APP_VERSION = 32
        private const val MAX_ANDROID_VERSION = 16
        private const val MAX_LOCALE = 16
        private const val MAX_MAP_KEYS = 32
        private const val MAX_MAP_KEY_LENGTH = 64
        private const val MAX_MAP_VALUE_LENGTH = 512
        private const val FUTURE_TOLERANCE_SECONDS = 5L * 60
        private const val MAX_AGE_DAYS = 30L
        private val EVENT_NAME_REGEX = Regex("^[a-z0-9_]{1,64}$")
        private val PII_BLOCKLIST = setOf(
            "query",
            "message",
            "email",
            "phone",
            "text",
            "search_query",
        )
    }
}

class AnalyticsDashboardUseCase(
    private val repository: AnalyticsEventRepository,
    private val maxRangeDays: Int,
) {
    fun execute(from: String, to: String): AnalyticsSummaryResponse {
        val range = parseDateRange(from, to, maxRangeDays)
        return repository.querySummary(range.first, range.second)
    }
}

class ListAnalyticsEventsUseCase(
    private val repository: AnalyticsEventRepository,
    private val maxRangeDays: Int,
) {
    fun execute(
        from: String,
        to: String,
        eventName: String?,
        installId: String?,
        limit: Int?,
        offset: Int?,
    ): AnalyticsEventsListResponse {
        val range = parseDateRange(from, to, maxRangeDays)
        val safeLimit = (limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
        val safeOffset = (offset ?: 0).coerceAtLeast(0)
        val (items, total) = repository.listEvents(
            from = range.first,
            to = range.second,
            eventName = eventName?.trim()?.takeIf { it.isNotEmpty() },
            installId = installId?.trim()?.takeIf { it.isNotEmpty() },
            limit = safeLimit,
            offset = safeOffset,
        )
        return AnalyticsEventsListResponse(
            items = items,
            total = total,
            limit = safeLimit,
            offset = safeOffset,
        )
    }

    companion object {
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 500
    }
}

class AnalyticsFunnelUseCase(
    private val repository: AnalyticsEventRepository,
    private val maxRangeDays: Int,
) {
    fun execute(from: String, to: String, stepsRaw: String?): AnalyticsFunnelResponse {
        val range = parseDateRange(from, to, maxRangeDays)
        val steps = parseSteps(stepsRaw)
        return repository.queryFunnel(range.first, range.second, steps)
    }

    private fun parseSteps(stepsRaw: String?): List<String> {
        val raw = stepsRaw?.trim().orEmpty()
        val steps = if (raw.isEmpty()) {
            DEFAULT_STEPS
        } else {
            raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        }
        val errors = mutableListOf<String>()
        if (steps.isEmpty()) errors += "steps must contain at least 1 event_name"
        if (steps.size > MAX_STEPS) errors += "steps must contain at most $MAX_STEPS event names"
        steps.forEachIndexed { index, name ->
            if (!EVENT_NAME_REGEX.matches(name)) {
                errors += "steps[$index]: invalid event_name '$name'"
            }
        }
        if (errors.isNotEmpty()) throw ValidationException(errors)
        return steps
    }

    companion object {
        val DEFAULT_STEPS = listOf("paywall_view", "pro_purchase_start", "pro_activated")
        const val MAX_STEPS = 8
        private val EVENT_NAME_REGEX = Regex("^[a-z0-9_]{1,64}$")
    }
}

class AnalyticsBreakdownUseCase(
    private val repository: AnalyticsEventRepository,
    private val maxRangeDays: Int,
) {
    fun execute(
        from: String,
        to: String,
        eventName: String?,
        param: String?,
        limit: Int?,
        fieldSourceRaw: String?,
    ): AnalyticsBreakdownResponse {
        val range = parseDateRange(from, to, maxRangeDays)
        val name = eventName?.trim().orEmpty()
        val paramKey = param?.trim().orEmpty()
        val fieldSource = parseFieldSource(fieldSourceRaw)
        val errors = mutableListOf<String>()
        if (!EVENT_NAME_REGEX.matches(name)) errors += "event_name is required and must match ^[a-z0-9_]{1,64}$"
        if (!PARAM_KEY_REGEX.matches(paramKey)) errors += "param is required and must match ^[a-z][a-z0-9_]{0,63}$"
        if (errors.isNotEmpty()) throw ValidationException(errors)
        val safeLimit = (limit ?: DEFAULT_LIMIT).coerceIn(1, MAX_LIMIT)
        return repository.queryBreakdown(
            from = range.first,
            to = range.second,
            eventName = name,
            param = paramKey,
            limit = safeLimit,
            fieldSource = fieldSource,
        )
    }

    private fun parseFieldSource(raw: String?): String {
        val value = raw?.trim()?.lowercase().orEmpty()
        return when (value) {
            "", "params" -> "params"
            "user_properties" -> "user_properties"
            else -> throw ValidationException(
                listOf("field_source must be params or user_properties"),
            )
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 100
        private val EVENT_NAME_REGEX = Regex("^[a-z0-9_]{1,64}$")
        private val PARAM_KEY_REGEX = Regex("^[a-z][a-z0-9_]{0,63}$")
    }
}

internal fun parseDateRange(
    from: String,
    to: String,
    maxRangeDays: Int,
): Pair<OffsetDateTime, OffsetDateTime> {
    val errors = mutableListOf<String>()
    val fromDate = runCatching { LocalDate.parse(from) }.getOrElse {
        errors += "from must be ISO date (YYYY-MM-DD)"
        null
    }
    val toDate = runCatching { LocalDate.parse(to) }.getOrElse {
        errors += "to must be ISO date (YYYY-MM-DD)"
        null
    }
    if (errors.isNotEmpty()) throw ValidationException(errors)
    // Product calendar days are Europe/Moscow (aligned with admin local date inputs in RU).
    val zone = ZoneId.of("Europe/Moscow")
    val start = fromDate!!.atStartOfDay(zone).toOffsetDateTime()
    val end = toDate!!.atTime(23, 59, 59, 999_000_000).atZone(zone).toOffsetDateTime()
    if (start.isAfter(end)) {
        throw ValidationException(listOf("from must be on or before to"))
    }
    val inclusiveDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1
    if (inclusiveDays > maxRangeDays) {
        throw ValidationException(
            listOf("date range must be at most $maxRangeDays calendar days (got $inclusiveDays)"),
        )
    }
    return start to end
}
