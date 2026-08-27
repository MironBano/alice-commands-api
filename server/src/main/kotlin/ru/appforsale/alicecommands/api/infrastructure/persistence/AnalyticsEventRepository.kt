package ru.appforsale.alicecommands.api.infrastructure.persistence

import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.domain.AnalyticsBreakdownItemDto
import ru.appforsale.alicecommands.api.domain.AnalyticsBreakdownResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsDailyPointDto
import ru.appforsale.alicecommands.api.domain.AnalyticsEventAdminDto
import ru.appforsale.alicecommands.api.domain.AnalyticsEventDto
import ru.appforsale.alicecommands.api.domain.AnalyticsFunnelResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsFunnelStepDto
import ru.appforsale.alicecommands.api.domain.AnalyticsSummaryResponse
import ru.appforsale.alicecommands.api.domain.AnalyticsTopEventDto
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsEventRepository
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsInsertResult
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ExposedAnalyticsEventRepository(
    private val database: Database,
) : AnalyticsEventRepository {

    override fun insertBatchIgnoreDuplicates(
        clientIp: String,
        events: List<AnalyticsEventDto>,
    ): AnalyticsInsertResult = transaction(database) {
        if (events.isEmpty()) return@transaction AnalyticsInsertResult(0, 0)

        val conn = connection.connection as java.sql.Connection
        val sql = """
            INSERT INTO analytics_events (
                event_id, install_id, session_id, event_name, occurred_at,
                app_version, android_version, locale, user_properties, params, client_ip
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
            ON CONFLICT (event_id) DO NOTHING
        """.trimIndent()
        val ps = conn.prepareStatement(sql)
        var accepted = 0
        var duplicates = 0
        try {
            events.forEach { event ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.installId)
                ps.setString(3, event.sessionId)
                ps.setString(4, event.eventName)
                ps.setObject(5, occurredAtToOffset(event.occurredAt))
                ps.setString(6, event.appVersion)
                ps.setString(7, event.androidVersion)
                ps.setString(8, event.locale)
                ps.setString(9, BundleCodec.json.encodeToString(event.userProperties))
                ps.setString(10, BundleCodec.json.encodeToString(event.params))
                ps.setString(11, clientIp)
                val rows = ps.executeUpdate()
                if (rows > 0) accepted++ else duplicates++
                ps.clearParameters()
            }
        } finally {
            ps.close()
        }
        AnalyticsInsertResult(accepted = accepted, duplicates = duplicates)
    }

    override fun countEventsForIpSince(clientIp: String, since: OffsetDateTime): Long = transaction(database) {
        AnalyticsEventsTable.selectAll()
            .where {
                (AnalyticsEventsTable.clientIp eq clientIp) and
                    (AnalyticsEventsTable.receivedAt greaterEq since)
            }
            .count()
    }

    override fun querySummary(from: OffsetDateTime, to: OffsetDateTime): AnalyticsSummaryResponse {
        val zone = ANALYTICS_ZONE
        val fromDate = from.atZoneSameInstant(zone).toLocalDate().toString()
        val toDate = to.atZoneSameInstant(zone).toLocalDate().toString()
        return transaction(database) {
            val rangeExpr = (AnalyticsEventsTable.occurredAt greaterEq from) and
                (AnalyticsEventsTable.occurredAt lessEq to)

            val totalEvents = AnalyticsEventsTable.selectAll()
                .where { rangeExpr }
                .count()
                .toInt()

            // Use SQL COUNT(DISTINCT …): Exposed withDistinct().count() is unreliable.
            val rawUniqueInstalls = countDistinctInstalls(from, to, eventName = null)

            // Client race: first cold start may emit session_start / app_foreground / daily_active
            // with different install_id but the same session_id. Count the dominant install per
            // session, then distinct — so ghosts from that race do not inflate the KPI.
            val uniqueInstalls = countCanonicalUniqueInstalls(from, to)

            val dailyActiveInstalls = countDistinctInstalls(from, to, eventName = DAILY_ACTIVE_EVENT)

            val topEvents = AnalyticsEventsTable
                .select(AnalyticsEventsTable.eventName, AnalyticsEventsTable.eventName.count())
                .where { rangeExpr }
                .groupBy(AnalyticsEventsTable.eventName)
                .orderBy(AnalyticsEventsTable.eventName.count() to SortOrder.DESC)
                .limit(TOP_EVENTS_LIMIT)
                .map { row ->
                    AnalyticsTopEventDto(
                        event_name = row[AnalyticsEventsTable.eventName],
                        count = row[AnalyticsEventsTable.eventName.count()].toInt(),
                    )
                }

            val daily = queryDailySeries(from, to)
            val newInstalls = daily.sumOf { it.new_installs }
            val avgDau = if (daily.isEmpty()) {
                0.0
            } else {
                ((daily.sumOf { it.dau }.toDouble() / daily.size) * 10.0).toInt() / 10.0
            }

            AnalyticsSummaryResponse(
                from = fromDate,
                to = toDate,
                daily_active_installs = dailyActiveInstalls,
                total_events = totalEvents,
                unique_installs = uniqueInstalls,
                top_events = topEvents,
                raw_unique_installs = rawUniqueInstalls,
                new_installs = newInstalls,
                daily = daily,
                avg_dau = avgDau,
                days_in_range = daily.size,
            )
        }
    }

    private fun queryDailySeries(from: OffsetDateTime, to: OffsetDateTime): List<AnalyticsDailyPointDto> {
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        val activitySql = """
            SELECT (occurred_at AT TIME ZONE 'Europe/Moscow')::date AS day,
                   COUNT(*)::int AS events,
                   COUNT(DISTINCT CASE WHEN event_name = ? THEN install_id END)::int AS dau,
                   COUNT(DISTINCT install_id)::int AS unique_installs
            FROM analytics_events
            WHERE occurred_at >= ? AND occurred_at <= ?
            GROUP BY 1
            ORDER BY 1
        """.trimIndent()
        // First calendar day we ever saw this install_id (Moscow) = “new install” for the chart.
        // Scoped to installs that appear in [from,to] and have no earlier event (avoids full-table GROUP BY).
        val newInstallsSql = """
            SELECT first_day AS day, COUNT(*)::int AS new_installs
            FROM (
              SELECT ae.install_id,
                     (MIN(ae.occurred_at) AT TIME ZONE 'Europe/Moscow')::date AS first_day
              FROM analytics_events ae
              WHERE ae.occurred_at >= ? AND ae.occurred_at <= ?
                AND NOT EXISTS (
                  SELECT 1 FROM analytics_events earlier
                  WHERE earlier.install_id = ae.install_id
                    AND earlier.occurred_at < ?
                )
              GROUP BY ae.install_id
            ) first_seen
            GROUP BY 1
            ORDER BY 1
        """.trimIndent()

        data class DayActivity(val events: Int, val dau: Int, val uniqueInstalls: Int)
        val activityByDay = linkedMapOf<LocalDate, DayActivity>()
        conn.prepareStatement(activitySql).use { ps ->
            ps.setString(1, DAILY_ACTIVE_EVENT)
            ps.setObject(2, from)
            ps.setObject(3, to)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val day = rs.getObject(1, LocalDate::class.java)
                    activityByDay[day] = DayActivity(
                        events = rs.getInt(2),
                        dau = rs.getInt(3),
                        uniqueInstalls = rs.getInt(4),
                    )
                }
            }
        }

        val startDay = from.atZoneSameInstant(ANALYTICS_ZONE).toLocalDate()
        val endDay = to.atZoneSameInstant(ANALYTICS_ZONE).toLocalDate()
        val newByDay = linkedMapOf<LocalDate, Int>()
        conn.prepareStatement(newInstallsSql).use { ps ->
            ps.setObject(1, from)
            ps.setObject(2, to)
            ps.setObject(3, from)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    newByDay[rs.getObject(1, LocalDate::class.java)] = rs.getInt(2)
                }
            }
        }

        val days = ChronoUnit.DAYS.between(startDay, endDay).toInt()
        return (0..days).map { offset ->
            val day = startDay.plusDays(offset.toLong())
            val activity = activityByDay[day]
            AnalyticsDailyPointDto(
                date = day.toString(),
                events = activity?.events ?: 0,
                dau = activity?.dau ?: 0,
                unique_installs = activity?.uniqueInstalls ?: 0,
                new_installs = newByDay[day] ?: 0,
            )
        }
    }

    private fun countDistinctInstalls(
        from: OffsetDateTime,
        to: OffsetDateTime,
        eventName: String?,
    ): Int {
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        val sql = if (eventName == null) {
            """
                SELECT COUNT(DISTINCT install_id)::int
                FROM analytics_events
                WHERE occurred_at >= ? AND occurred_at <= ?
            """.trimIndent()
        } else {
            """
                SELECT COUNT(DISTINCT install_id)::int
                FROM analytics_events
                WHERE occurred_at >= ? AND occurred_at <= ? AND event_name = ?
            """.trimIndent()
        }
        conn.prepareStatement(sql).use { ps ->
            ps.setObject(1, from)
            ps.setObject(2, to)
            if (eventName != null) ps.setString(3, eventName)
            ps.executeQuery().use { rs ->
                check(rs.next()) { "countDistinctInstalls returned no row" }
                return rs.getInt(1)
            }
        }
    }

    private fun countCanonicalUniqueInstalls(from: OffsetDateTime, to: OffsetDateTime): Int {
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        val sql = """
            SELECT COUNT(DISTINCT install_id) FROM (
              SELECT session_id, install_id,
                     ROW_NUMBER() OVER (
                       PARTITION BY session_id
                       ORDER BY event_count DESC, install_id ASC
                     ) AS rn
              FROM (
                SELECT session_id, install_id, COUNT(*) AS event_count
                FROM analytics_events
                WHERE occurred_at >= ? AND occurred_at <= ?
                GROUP BY session_id, install_id
              ) per_pair
            ) ranked
            WHERE rn = 1
        """.trimIndent()
        conn.prepareStatement(sql).use { ps ->
            ps.setObject(1, from)
            ps.setObject(2, to)
            ps.executeQuery().use { rs ->
                check(rs.next()) { "canonical unique installs query returned no row" }
                return rs.getInt(1)
            }
        }
    }

    override fun listEvents(
        from: OffsetDateTime,
        to: OffsetDateTime,
        eventName: String?,
        installId: String?,
        limit: Int,
        offset: Int,
    ): Pair<List<AnalyticsEventAdminDto>, Int> = transaction(database) {
        var condition = (AnalyticsEventsTable.occurredAt greaterEq from) and
            (AnalyticsEventsTable.occurredAt lessEq to)
        eventName?.let { name ->
            condition = condition and (AnalyticsEventsTable.eventName eq name)
        }
        installId?.let { id ->
            condition = condition and (AnalyticsEventsTable.installId eq id)
        }

        val total = AnalyticsEventsTable.selectAll()
            .where { condition }
            .count()
            .toInt()

        val items = AnalyticsEventsTable.selectAll()
            .where { condition }
            .orderBy(AnalyticsEventsTable.occurredAt to SortOrder.DESC)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toAdminDto() }

        items to total
    }

    override fun queryFunnel(
        from: OffsetDateTime,
        to: OffsetDateTime,
        steps: List<String>,
    ): AnalyticsFunnelResponse = transaction(database) {
        val fromDate = from.atZoneSameInstant(ANALYTICS_ZONE).toLocalDate().toString()
        val toDate = to.atZoneSameInstant(ANALYTICS_ZONE).toLocalDate().toString()
        val conn = connection.connection as java.sql.Connection
        val sql = """
            SELECT COUNT(DISTINCT install_id)::int
            FROM analytics_events
            WHERE occurred_at >= ? AND occurred_at <= ? AND event_name = ?
        """.trimIndent()
        val installs = steps.map { step ->
            conn.prepareStatement(sql).use { ps ->
                ps.setObject(1, from)
                ps.setObject(2, to)
                ps.setString(3, step)
                ps.executeQuery().use { rs ->
                    check(rs.next()) { "funnel step query returned no row" }
                    rs.getInt(1)
                }
            }
        }
        val first = installs.firstOrNull() ?: 0
        val stepDtos = steps.mapIndexed { index, name ->
            val count = installs[index]
            val prev = if (index == 0) null else installs[index - 1]
            AnalyticsFunnelStepDto(
                event_name = name,
                installs = count,
                conversion_from_previous = prev?.let { conversionPercent(count, it) },
                conversion_from_first = if (index == 0) null else conversionPercent(count, first),
            )
        }
        AnalyticsFunnelResponse(from = fromDate, to = toDate, steps = stepDtos)
    }

    // Funnel/breakdown from/to labels use Moscow calendar dates (parseDateRange bounds).

    override fun queryBreakdown(
        from: OffsetDateTime,
        to: OffsetDateTime,
        eventName: String,
        param: String,
        limit: Int,
        fieldSource: String,
    ): AnalyticsBreakdownResponse = transaction(database) {
        val fromDate = from.atZoneSameInstant(ANALYTICS_ZONE).toLocalDate().toString()
        val toDate = to.atZoneSameInstant(ANALYTICS_ZONE).toLocalDate().toString()
        val conn = connection.connection as java.sql.Connection
        val jsonColumn = when (fieldSource) {
            "user_properties" -> "user_properties"
            else -> "params"
        }
        // param key is validated upstream to ^[a-z][a-z0-9_]{0,63}$ — bind as value, not identifier.
        val sql = """
            SELECT COALESCE($jsonColumn ->> ?, '') AS value, COUNT(*)::int AS cnt
            FROM analytics_events
            WHERE occurred_at >= ? AND occurred_at <= ?
              AND event_name = ?
              AND jsonb_exists($jsonColumn, ?)
            GROUP BY 1
            ORDER BY cnt DESC, value ASC
            LIMIT ?
        """.trimIndent()
        val items = mutableListOf<AnalyticsBreakdownItemDto>()
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, param)
            ps.setObject(2, from)
            ps.setObject(3, to)
            ps.setString(4, eventName)
            ps.setString(5, param)
            ps.setInt(6, limit)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    items += AnalyticsBreakdownItemDto(
                        value = rs.getString(1).orEmpty().ifEmpty { "(empty)" },
                        count = rs.getInt(2),
                    )
                }
            }
        }
        AnalyticsBreakdownResponse(
            from = fromDate,
            to = toDate,
            event_name = eventName,
            param = param,
            field_source = fieldSource,
            items = items,
        )
    }

    private fun conversionPercent(current: Int, base: Int): Double {
        if (base <= 0) return 0.0
        return ((current.toDouble() / base.toDouble()) * 1000.0).toInt() / 10.0
    }

    private fun ResultRow.toAdminDto() = AnalyticsEventAdminDto(
        event_id = this[AnalyticsEventsTable.eventId],
        install_id = this[AnalyticsEventsTable.installId],
        session_id = this[AnalyticsEventsTable.sessionId],
        event_name = this[AnalyticsEventsTable.eventName],
        occurred_at = this[AnalyticsEventsTable.occurredAt].toIsoString(),
        app_version = this[AnalyticsEventsTable.appVersion],
        android_version = this[AnalyticsEventsTable.androidVersion],
        locale = this[AnalyticsEventsTable.locale],
        user_properties = this[AnalyticsEventsTable.userProperties],
        params = this[AnalyticsEventsTable.params],
    )

    private fun occurredAtToOffset(ms: Long): OffsetDateTime =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneOffset.UTC)

    companion object {
        const val DAILY_ACTIVE_EVENT = "daily_active"
        const val TOP_EVENTS_LIMIT = 10
        val ANALYTICS_ZONE: ZoneId = ZoneId.of("Europe/Moscow")
    }
}

private fun OffsetDateTime.toIsoString(): String =
    format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
