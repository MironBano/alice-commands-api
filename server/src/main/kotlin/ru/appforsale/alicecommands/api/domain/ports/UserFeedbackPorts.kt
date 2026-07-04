package ru.appforsale.alicecommands.api.domain.ports

import ru.appforsale.alicecommands.api.domain.CommandReportDto
import ru.appforsale.alicecommands.api.domain.FeedbackInboxCounts
import ru.appforsale.alicecommands.api.domain.ReportCommandIssueRequest
import ru.appforsale.alicecommands.api.domain.SubmitFeedbackRequest
import ru.appforsale.alicecommands.api.domain.UserFeedbackDto

interface UserFeedbackRepository {
    fun insertFeedback(clientIp: String, request: SubmitFeedbackRequest): UserFeedbackDto
    fun insertCommandReport(
        clientIp: String,
        commandId: String,
        request: ReportCommandIssueRequest,
        commandExistsCurrent: Boolean,
    ): CommandReportDto
    fun listFeedback(status: String?, search: String?): List<UserFeedbackDto>
    fun listCommandReports(status: String?, commandId: String?, search: String?): List<CommandReportDto>
    fun getFeedback(id: String): UserFeedbackDto?
    fun getCommandReport(id: String): CommandReportDto?
    fun updateFeedbackStatus(id: String, status: String)
    fun updateCommandReportStatus(id: String, status: String)
    fun inboxCounts(): FeedbackInboxCounts
}

interface PublicSubmissionRateLimiter {
    fun isBlocked(ip: String): Boolean
    fun recordSubmission(ip: String)
}
