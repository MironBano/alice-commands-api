package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandOfDayAdminResponse
import ru.appforsale.alicecommands.api.domain.CommandOfDayPreview
import ru.appforsale.alicecommands.api.domain.CommandOfDaySettings
import ru.appforsale.alicecommands.api.domain.UpdateCommandOfDayRequest
import ru.appforsale.alicecommands.api.domain.ValidationException
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import java.time.Instant
import java.time.LocalDate

class CommandOfDayAdminUseCase(
    private val draftRepository: DraftRepository,
) {
    fun get(): CommandOfDayAdminResponse {
        val settings = draftRepository.getCommandOfDaySettings()
            ?: throw IllegalStateException("Command of day settings unavailable — catalog may be empty")
        val commands = draftRepository.listCommands()
        return buildResponse(settings, commands)
    }

    fun update(request: UpdateCommandOfDayRequest, adminUser: String): CommandOfDayAdminResponse {
        val mode = request.mode.trim().lowercase()
        if (mode !in setOf(CommandOfDayValidationUseCase.MODE_MANUAL, CommandOfDayValidationUseCase.MODE_AUTO)) {
            throw ValidationException(listOf("mode must be manual or auto"))
        }
        val commands = draftRepository.listCommands()
        val categories = draftRepository.listCategories()
        val categoryIds = categories.map { it.id }.toSet()
        val commandIds = commands.map { it.id }.toSet()
        val seed = request.auto_seed ?: CommandOfDayResolver.DEFAULT_SEED
        if (seed < 1) {
            throw ValidationException(listOf("auto_seed must be >= 1"))
        }

        val (commandId, autoCategoryId) = when (mode) {
            CommandOfDayValidationUseCase.MODE_MANUAL -> {
                val id = request.command_id?.trim().orEmpty()
                if (id.isBlank()) {
                    throw ValidationException(listOf("command_id is required for manual mode"))
                }
                if (id !in commandIds) {
                    throw ValidationException(listOf("command_id '$id' not found"))
                }
                id to null
            }
            else -> {
                val categoryId = request.auto_category_id?.trim().orEmpty()
                if (categoryId.isBlank()) {
                    throw ValidationException(listOf("auto_category_id is required for auto mode"))
                }
                if (categoryId !in categoryIds) {
                    throw ValidationException(listOf("auto_category_id '$categoryId' not found"))
                }
                val pool = CommandOfDayResolver.buildPool(commands, categoryId)
                if (pool.isEmpty()) {
                    throw ValidationException(listOf("category '$categoryId' has no commands"))
                }
                val resolved = CommandOfDayResolver.resolveCommandId(
                    pool,
                    CommandOfDayResolver.todayMoscow(),
                    seed,
                )
                resolved to categoryId
            }
        }

        val settings = CommandOfDaySettings(
            mode = mode,
            command_id = commandId,
            auto_category_id = autoCategoryId,
            auto_seed = seed,
            updated_at = Instant.now().toString(),
            updated_by = adminUser,
        )
        draftRepository.upsertCommandOfDaySettings(settings)
        return buildResponse(settings, commands)
    }

    fun previewForDate(settings: CommandOfDaySettings, commands: List<Command>, date: LocalDate): CommandOfDayPreview? {
        val commandId = when (settings.mode) {
            CommandOfDayValidationUseCase.MODE_AUTO -> {
                val categoryId = settings.auto_category_id ?: return null
                val pool = CommandOfDayResolver.buildPool(commands, categoryId)
                if (pool.isEmpty()) return null
                CommandOfDayResolver.resolveCommandId(pool, date, settings.auto_seed)
            }
            else -> settings.command_id
        }
        val command = commands.firstOrNull { it.id == commandId } ?: return null
        return CommandOfDayPreview(
            date = date.toString(),
            command_id = commandId,
            title_ru = command.title_ru,
            phrase = command.phrases.firstOrNull(),
            category_id = command.category_id,
        )
    }

    private fun buildResponse(settings: CommandOfDaySettings, commands: List<Command>): CommandOfDayAdminResponse {
        val today = CommandOfDayResolver.todayMoscow()
        val preview = previewForDate(settings, commands, today)
        val poolSize = settings.auto_category_id?.let { categoryId ->
            CommandOfDayResolver.buildPool(commands, categoryId).size
        }
        return CommandOfDayAdminResponse(
            settings = settings,
            preview_today = preview,
            pool_size = poolSize,
        )
    }
}
