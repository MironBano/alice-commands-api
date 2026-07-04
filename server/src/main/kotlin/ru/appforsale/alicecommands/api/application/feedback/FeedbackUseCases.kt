package ru.appforsale.alicecommands.api.application.feedback

import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.domain.CommandReportDto
import ru.appforsale.alicecommands.api.domain.CommandReportIssueType
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.FeedbackInboxCounts
import ru.appforsale.alicecommands.api.domain.FeedbackStatus
import ru.appforsale.alicecommands.api.domain.ReportCommandIssueRequest
import ru.appforsale.alicecommands.api.domain.ReportCommandIssueResponse
import ru.appforsale.alicecommands.api.domain.SubmitFeedbackRequest
import ru.appforsale.alicecommands.api.domain.SubmitFeedbackResponse
import ru.appforsale.alicecommands.api.domain.UserFeedbackDto
import ru.appforsale.alicecommands.api.domain.ValidationException
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository
import ru.appforsale.alicecommands.api.domain.ports.PublicSubmissionRateLimiter
import ru.appforsale.alicecommands.api.domain.ports.UserFeedbackRepository

class PublishedBundleLookup(
    private val manifestRepository: ManifestRepository,
    private val bundleStorage: BundleStorage,
) {
    fun currentContentVersion(): Int? = manifestRepository.getCurrent()?.contentVersion

    fun commandExistsInCurrent(commandId: String): Boolean =
        loadCurrentBundle()?.commands?.any { it.id == commandId } == true

    fun commandExistsInCurrent(commandId: String, clientContentVersion: Int?): CommandValidationResult {
        val current = manifestRepository.getCurrent()
            ?: return CommandValidationResult(existsInCurrent = false, rejectNotFound = false)
        val existsNow = commandExistsInCurrent(commandId)
        val rejectNotFound = clientContentVersion != null &&
            clientContentVersion == current.contentVersion &&
            !existsNow
        return CommandValidationResult(existsInCurrent = existsNow, rejectNotFound = rejectNotFound)
    }

    private fun loadCurrentBundle(): ContentBundle? {
        val current = manifestRepository.getCurrent() ?: return null
        val bytes = bundleStorage.read(current.bundlePath) ?: return null
        return BundleCodec.json.decodeFromString<ContentBundle>(BundleCodec.gunzip(bytes))
    }
}

data class CommandValidationResult(
    val existsInCurrent: Boolean,
    val rejectNotFound: Boolean,
)

class SubmitFeedbackUseCase(
    private val repository: UserFeedbackRepository,
    private val rateLimiter: PublicSubmissionRateLimiter,
) {
    fun execute(clientIp: String, request: SubmitFeedbackRequest): SubmitFeedbackResponse {
        if (rateLimiter.isBlocked(clientIp)) {
            throw RateLimitException()
        }
        validateFeedback(request)
        val saved = repository.insertFeedback(clientIp, request)
        rateLimiter.recordSubmission(clientIp)
        return SubmitFeedbackResponse(id = saved.id, status = saved.status)
    }

    private fun validateFeedback(request: SubmitFeedbackRequest) {
        val errors = mutableListOf<String>()
        val message = request.message.trim()
        if (message.isEmpty()) errors += "message is required"
        if (message.length > MAX_MESSAGE_LENGTH) errors += "message must be at most $MAX_MESSAGE_LENGTH characters"
        request.rating?.let { rating ->
            if (rating !in 1..5) errors += "rating must be between 1 and 5"
        }
        validateOptionalField(request.app_version, "app_version", MAX_SHORT_FIELD, errors)
        validateOptionalField(request.platform, "platform", MAX_SHORT_FIELD, errors)
        validateOptionalField(request.locale, "locale", MAX_SHORT_FIELD, errors)
        validateOptionalField(request.device_model, "device_model", MAX_SHORT_FIELD, errors)
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 2000
        const val MAX_SHORT_FIELD = 128
    }
}

class ReportCommandIssueUseCase(
    private val repository: UserFeedbackRepository,
    private val rateLimiter: PublicSubmissionRateLimiter,
    private val publishedBundleLookup: PublishedBundleLookup,
) {
    fun execute(
        clientIp: String,
        commandId: String,
        request: ReportCommandIssueRequest,
    ): ReportCommandIssueResponse {
        if (rateLimiter.isBlocked(clientIp)) {
            throw RateLimitException()
        }
        validateReport(commandId, request)
        val validation = publishedBundleLookup.commandExistsInCurrent(commandId, request.content_version)
        if (validation.rejectNotFound) {
            throw NoSuchElementException("Command not found in published bundle: $commandId")
        }
        val saved = repository.insertCommandReport(
            clientIp = clientIp,
            commandId = commandId.trim(),
            request = request,
            commandExistsCurrent = validation.existsInCurrent,
        )
        rateLimiter.recordSubmission(clientIp)
        return ReportCommandIssueResponse(
            id = saved.id,
            status = saved.status,
            command_exists_current = saved.command_exists_current,
        )
    }

    private fun validateReport(commandId: String, request: ReportCommandIssueRequest) {
        val errors = mutableListOf<String>()
        if (commandId.isBlank()) errors += "command_id is required"
        if (commandId.length > MAX_ID_LENGTH) errors += "command_id is too long"
        if (request.issue_type !in CommandReportIssueType.ALL) {
            errors += "issue_type must be one of: ${CommandReportIssueType.ALL.joinToString()}"
        }
        request.message?.trim()?.takeIf { it.isNotEmpty() }?.let { message ->
            if (message.length > SubmitFeedbackUseCase.MAX_MESSAGE_LENGTH) {
                errors += "message must be at most ${SubmitFeedbackUseCase.MAX_MESSAGE_LENGTH} characters"
            }
        }
        validateOptionalField(request.category_id, "category_id", MAX_ID_LENGTH, errors)
        validateOptionalField(request.command_title, "command_title", MAX_TITLE_LENGTH, errors)
        validateOptionalField(request.phrase_used, "phrase_used", MAX_TITLE_LENGTH, errors)
        validateOptionalField(request.app_version, "app_version", SubmitFeedbackUseCase.MAX_SHORT_FIELD, errors)
        validateOptionalField(request.platform, "platform", SubmitFeedbackUseCase.MAX_SHORT_FIELD, errors)
        validateOptionalField(request.locale, "locale", SubmitFeedbackUseCase.MAX_SHORT_FIELD, errors)
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    companion object {
        const val MAX_ID_LENGTH = 128
        const val MAX_TITLE_LENGTH = 512
    }
}

class ListFeedbackUseCase(private val repository: UserFeedbackRepository) {
    fun execute(status: String?, search: String?): List<UserFeedbackDto> {
        validateStatus(status)
        return repository.listFeedback(status, search?.trim()?.takeIf { it.isNotEmpty() })
    }
}

class ListCommandReportsUseCase(private val repository: UserFeedbackRepository) {
    fun execute(status: String?, commandId: String?, search: String?): List<CommandReportDto> {
        validateStatus(status)
        return repository.listCommandReports(
            status = status,
            commandId = commandId?.trim()?.takeIf { it.isNotEmpty() },
            search = search?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}

class ResolveFeedbackUseCase(private val repository: UserFeedbackRepository) {
    fun execute(id: String) {
        repository.getFeedback(id) ?: throw NoSuchElementException("Feedback not found: $id")
        repository.updateFeedbackStatus(id, FeedbackStatus.RESOLVED)
    }
}

class DismissFeedbackUseCase(private val repository: UserFeedbackRepository) {
    fun execute(id: String) {
        repository.getFeedback(id) ?: throw NoSuchElementException("Feedback not found: $id")
        repository.updateFeedbackStatus(id, FeedbackStatus.DISMISSED)
    }
}

class ResolveCommandReportUseCase(private val repository: UserFeedbackRepository) {
    fun execute(id: String) {
        repository.getCommandReport(id) ?: throw NoSuchElementException("Command report not found: $id")
        repository.updateCommandReportStatus(id, FeedbackStatus.RESOLVED)
    }
}

class DismissCommandReportUseCase(private val repository: UserFeedbackRepository) {
    fun execute(id: String) {
        repository.getCommandReport(id) ?: throw NoSuchElementException("Command report not found: $id")
        repository.updateCommandReportStatus(id, FeedbackStatus.DISMISSED)
    }
}

class FeedbackInboxCountsUseCase(private val repository: UserFeedbackRepository) {
    fun execute(): FeedbackInboxCounts = repository.inboxCounts()
}

class RateLimitException : Exception("Too many submissions")

private fun validateStatus(status: String?) {
    if (status == null) return
    if (status !in setOf(FeedbackStatus.OPEN, FeedbackStatus.RESOLVED, FeedbackStatus.DISMISSED)) {
        throw ValidationException(listOf("status must be open, resolved, or dismissed"))
    }
}

private fun validateOptionalField(value: String?, field: String, maxLength: Int, errors: MutableList<String>) {
    value?.trim()?.takeIf { it.isNotEmpty() }?.let {
        if (it.length > maxLength) errors += "$field must be at most $maxLength characters"
    }
}
