package ru.appforsale.alicecommands.api.application.push

import ru.appforsale.alicecommands.api.domain.ports.PushTokenRepository
import ru.appforsale.alicecommands.api.domain.push.PushPreferencesRequest
import ru.appforsale.alicecommands.api.domain.push.PushRegisterRequest
import ru.appforsale.alicecommands.api.domain.push.PushRegisterResult
import ru.appforsale.alicecommands.api.domain.push.PushUnregisterRequest

class RegisterPushTokenUseCase(
    private val repository: PushTokenRepository,
) {
    fun execute(request: PushRegisterRequest): PushRegisterResult {
        require(request.installId.isNotBlank()) { "installId required" }
        require(request.rustoreToken.isNotBlank()) { "rustoreToken required" }
        return repository.upsertRegister(request)
    }
}

class UpdatePushPreferencesUseCase(
    private val repository: PushTokenRepository,
) {
    fun execute(request: PushPreferencesRequest) {
        require(request.installId.isNotBlank()) { "installId required" }
        val updated = repository.updatePreferences(request)
        require(updated) { "not_registered" }
    }
}

class UnregisterPushTokenUseCase(
    private val repository: PushTokenRepository,
) {
    fun execute(request: PushUnregisterRequest) {
        require(request.installId.isNotBlank()) { "installId required" }
        require(request.rustoreToken.isNotBlank()) { "rustoreToken required" }
        val deleted = repository.deleteIfTokenMatches(request.installId, request.rustoreToken)
        require(deleted) { "token_mismatch_or_missing" }
    }
}
