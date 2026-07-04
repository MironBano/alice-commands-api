package ru.appforsale.alicecommands.api.infrastructure.persistence

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.appforsale.alicecommands.api.domain.CommandReportDto
import ru.appforsale.alicecommands.api.domain.FeedbackInboxCounts
import ru.appforsale.alicecommands.api.domain.FeedbackStatus
import ru.appforsale.alicecommands.api.domain.ReportCommandIssueRequest
import ru.appforsale.alicecommands.api.domain.SubmitFeedbackRequest
import ru.appforsale.alicecommands.api.domain.UserFeedbackDto
import ru.appforsale.alicecommands.api.domain.ports.UserFeedbackRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

class ExposedUserFeedbackRepository(
    private val database: Database,
) : UserFeedbackRepository {

    private inline fun unitTx(crossinline block: org.jetbrains.exposed.sql.Transaction.() -> Unit) {
        transaction(database) { block() }
    }

    override fun insertFeedback(clientIp: String, request: SubmitFeedbackRequest): UserFeedbackDto {
        val id = UUID.randomUUID().toString()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        unitTx {
            UserFeedbackTable.insert {
                it[UserFeedbackTable.id] = id
                it[message] = request.message.trim()
                it[rating] = request.rating
                it[appVersion] = request.app_version?.trim()?.takeIf { v -> v.isNotBlank() }
                it[platform] = request.platform?.trim()?.takeIf { v -> v.isNotBlank() }
                it[locale] = request.locale?.trim()?.takeIf { v -> v.isNotBlank() }
                it[contentVersion] = request.content_version
                it[deviceModel] = request.device_model?.trim()?.takeIf { v -> v.isNotBlank() }
                it[UserFeedbackTable.clientIp] = clientIp
                it[status] = FeedbackStatus.OPEN
                it[createdAt] = now
                it[resolvedAt] = null
            }
        }
        return UserFeedbackDto(
            id = id,
            message = request.message.trim(),
            rating = request.rating,
            app_version = request.app_version?.trim()?.takeIf { it.isNotBlank() },
            platform = request.platform?.trim()?.takeIf { it.isNotBlank() },
            locale = request.locale?.trim()?.takeIf { it.isNotBlank() },
            content_version = request.content_version,
            device_model = request.device_model?.trim()?.takeIf { it.isNotBlank() },
            status = FeedbackStatus.OPEN,
            created_at = now.toIsoString(),
            resolved_at = null,
        )
    }

    override fun insertCommandReport(
        clientIp: String,
        commandId: String,
        request: ReportCommandIssueRequest,
        commandExistsCurrent: Boolean,
    ): CommandReportDto {
        val id = UUID.randomUUID().toString()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        unitTx {
            CommandReportsTable.insert {
                it[CommandReportsTable.id] = id
                it[CommandReportsTable.commandId] = commandId
                it[issueType] = request.issue_type
                it[message] = request.message?.trim()?.takeIf { v -> v.isNotBlank() }
                it[contentVersion] = request.content_version
                it[categoryId] = request.category_id?.trim()?.takeIf { v -> v.isNotBlank() }
                it[commandTitle] = request.command_title?.trim()?.takeIf { v -> v.isNotBlank() }
                it[phraseUsed] = request.phrase_used?.trim()?.takeIf { v -> v.isNotBlank() }
                it[appVersion] = request.app_version?.trim()?.takeIf { v -> v.isNotBlank() }
                it[platform] = request.platform?.trim()?.takeIf { v -> v.isNotBlank() }
                it[locale] = request.locale?.trim()?.takeIf { v -> v.isNotBlank() }
                it[CommandReportsTable.commandExistsCurrent] = commandExistsCurrent
                it[CommandReportsTable.clientIp] = clientIp
                it[status] = FeedbackStatus.OPEN
                it[createdAt] = now
                it[resolvedAt] = null
            }
        }
        return CommandReportDto(
            id = id,
            command_id = commandId,
            issue_type = request.issue_type,
            message = request.message?.trim()?.takeIf { it.isNotBlank() },
            content_version = request.content_version,
            category_id = request.category_id?.trim()?.takeIf { it.isNotBlank() },
            command_title = request.command_title?.trim()?.takeIf { it.isNotBlank() },
            phrase_used = request.phrase_used?.trim()?.takeIf { it.isNotBlank() },
            app_version = request.app_version?.trim()?.takeIf { it.isNotBlank() },
            platform = request.platform?.trim()?.takeIf { it.isNotBlank() },
            locale = request.locale?.trim()?.takeIf { it.isNotBlank() },
            command_exists_current = commandExistsCurrent,
            status = FeedbackStatus.OPEN,
            created_at = now.toIsoString(),
            resolved_at = null,
        )
    }

    override fun listFeedback(status: String?, search: String?): List<UserFeedbackDto> = transaction(database) {
        val query = UserFeedbackTable.selectAll()
        val filtered = when {
            status != null && search != null -> query.where {
                (UserFeedbackTable.status eq status) and feedbackSearchExpr(search)
            }
            status != null -> query.where { UserFeedbackTable.status eq status }
            search != null -> query.where { feedbackSearchExpr(search) }
            else -> query
        }
        filtered.orderBy(UserFeedbackTable.createdAt to SortOrder.DESC)
            .map { it.toUserFeedbackDto() }
    }

    override fun listCommandReports(
        status: String?,
        commandId: String?,
        search: String?,
    ): List<CommandReportDto> = transaction(database) {
        val query = CommandReportsTable.selectAll()
        val conditions = buildList {
            status?.let { add(CommandReportsTable.status eq it) }
            commandId?.takeIf { it.isNotBlank() }?.let { add(CommandReportsTable.commandId eq it) }
            search?.takeIf { it.isNotBlank() }?.let { add(commandReportSearchExpr(it)) }
        }
        val filtered = if (conditions.isEmpty()) {
            query
        } else {
            query.where { conditions.reduce { acc, expr -> acc and expr } }
        }
        filtered.orderBy(CommandReportsTable.createdAt to SortOrder.DESC)
            .map { it.toCommandReportDto() }
    }

    override fun getFeedback(id: String): UserFeedbackDto? = transaction(database) {
        UserFeedbackTable.selectAll()
            .where { UserFeedbackTable.id eq id }
            .map { it.toUserFeedbackDto() }
            .singleOrNull()
    }

    override fun getCommandReport(id: String): CommandReportDto? = transaction(database) {
        CommandReportsTable.selectAll()
            .where { CommandReportsTable.id eq id }
            .map { it.toCommandReportDto() }
            .singleOrNull()
    }

    override fun updateFeedbackStatus(id: String, status: String) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        unitTx {
            UserFeedbackTable.update({ UserFeedbackTable.id eq id }) {
                it[UserFeedbackTable.status] = status
                it[resolvedAt] = if (status == FeedbackStatus.OPEN) null else now
            }
        }
    }

    override fun updateCommandReportStatus(id: String, status: String) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        unitTx {
            CommandReportsTable.update({ CommandReportsTable.id eq id }) {
                it[CommandReportsTable.status] = status
                it[resolvedAt] = if (status == FeedbackStatus.OPEN) null else now
            }
        }
    }

    override fun inboxCounts(): FeedbackInboxCounts = transaction(database) {
        FeedbackInboxCounts(
            open_feedback = UserFeedbackTable.selectAll()
                .where { UserFeedbackTable.status eq FeedbackStatus.OPEN }
                .count()
                .toInt(),
            open_command_reports = CommandReportsTable.selectAll()
                .where { CommandReportsTable.status eq FeedbackStatus.OPEN }
                .count()
                .toInt(),
        )
    }

    private fun feedbackSearchExpr(search: String) =
        (UserFeedbackTable.message like "%$search%") or
            (UserFeedbackTable.appVersion like "%$search%") or
            (UserFeedbackTable.platform like "%$search%") or
            (UserFeedbackTable.locale like "%$search%")

    private fun commandReportSearchExpr(search: String) =
        (CommandReportsTable.commandId like "%$search%") or
            (CommandReportsTable.commandTitle like "%$search%") or
            (CommandReportsTable.message like "%$search%") or
            (CommandReportsTable.phraseUsed like "%$search%") or
            (CommandReportsTable.issueType like "%$search%")

    private fun ResultRow.toUserFeedbackDto() = UserFeedbackDto(
        id = this[UserFeedbackTable.id],
        message = this[UserFeedbackTable.message],
        rating = this[UserFeedbackTable.rating],
        app_version = this[UserFeedbackTable.appVersion],
        platform = this[UserFeedbackTable.platform],
        locale = this[UserFeedbackTable.locale],
        content_version = this[UserFeedbackTable.contentVersion],
        device_model = this[UserFeedbackTable.deviceModel],
        status = this[UserFeedbackTable.status],
        created_at = this[UserFeedbackTable.createdAt].toIsoString(),
        resolved_at = this[UserFeedbackTable.resolvedAt]?.toIsoString(),
    )

    private fun ResultRow.toCommandReportDto() = CommandReportDto(
        id = this[CommandReportsTable.id],
        command_id = this[CommandReportsTable.commandId],
        issue_type = this[CommandReportsTable.issueType],
        message = this[CommandReportsTable.message],
        content_version = this[CommandReportsTable.contentVersion],
        category_id = this[CommandReportsTable.categoryId],
        command_title = this[CommandReportsTable.commandTitle],
        phrase_used = this[CommandReportsTable.phraseUsed],
        app_version = this[CommandReportsTable.appVersion],
        platform = this[CommandReportsTable.platform],
        locale = this[CommandReportsTable.locale],
        command_exists_current = this[CommandReportsTable.commandExistsCurrent],
        status = this[CommandReportsTable.status],
        created_at = this[CommandReportsTable.createdAt].toIsoString(),
        resolved_at = this[CommandReportsTable.resolvedAt]?.toIsoString(),
    )

    private fun OffsetDateTime.toIsoString(): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this)
}
