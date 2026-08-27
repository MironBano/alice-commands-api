package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.CommandOfDay
import ru.appforsale.alicecommands.api.domain.CommandOfDaySettings
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ContentValidationWarnings
import ru.appforsale.alicecommands.api.domain.ValidationException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

class CommandOfDayValidationUseCase {

    fun validateForPublish(bundle: ContentBundle) {
        val cod = bundle.command_of_day ?: return
        val errors = collectPublishErrors(bundle, cod)
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }

    fun collectWarnings(bundle: ContentBundle, settings: CommandOfDaySettings?): ContentValidationWarnings {
        val existing = bundle.command_of_day
        if (existing == null || settings == null) {
            return ContentValidationWarnings()
        }
        val autoPoolSmall = existing.mode == MODE_AUTO && run {
            val pool = CommandOfDayResolver.buildPool(bundle.commands, existing.auto_category_id!!)
            pool.size in 1..2
        }
        val manualPinnedLong = existing.mode == MODE_MANUAL && run {
            val updatedAt = runCatching { Instant.parse(settings.updated_at) }.getOrNull() ?: return@run false
            Duration.between(updatedAt, Instant.now()).toDays() > 14
        }
        return ContentValidationWarnings(
            command_of_day_auto_pool_small = autoPoolSmall,
            command_of_day_manual_pinned_long = manualPinnedLong,
        )
    }

    fun mergeWarnings(
        base: ContentValidationWarnings,
        codWarnings: ContentValidationWarnings,
    ): ContentValidationWarnings = base.copy(
        command_of_day_auto_pool_small = codWarnings.command_of_day_auto_pool_small,
        command_of_day_manual_pinned_long = codWarnings.command_of_day_manual_pinned_long,
    )

    private fun collectPublishErrors(bundle: ContentBundle, cod: CommandOfDay): List<String> {
        val errors = mutableListOf<String>()
        val commandIds = bundle.commands.map { it.id }.toSet()
        val categoryIds = bundle.categories.map { it.id }.toSet()

        if (cod.mode !in MODES) {
            errors += "command_of_day.mode must be manual or auto"
        }
        if (cod.command_id.isBlank()) {
            errors += "command_of_day.command_id must not be empty"
        } else if (cod.command_id !in commandIds) {
            errors += "command_of_day.command_id '${cod.command_id}' not found in commands"
        }
        if (!DATE_PATTERN.matches(cod.resolved_date)) {
            errors += "command_of_day.resolved_date must be YYYY-MM-DD"
        } else {
            runCatching { LocalDate.parse(cod.resolved_date) }
                .onFailure { errors += "command_of_day.resolved_date is not a valid date" }
        }
        runCatching { Instant.parse(cod.updated_at) }
            .onFailure { errors += "command_of_day.updated_at must be valid ISO-8601" }
        if (cod.auto_seed < 1) {
            errors += "command_of_day.auto_seed must be >= 1"
        }

        when (cod.mode) {
            MODE_MANUAL -> {
                if (cod.auto_category_id != null) {
                    errors += "command_of_day.auto_category_id must be null for manual mode"
                }
            }
            MODE_AUTO -> {
                val categoryId = cod.auto_category_id
                if (categoryId.isNullOrBlank()) {
                    errors += "command_of_day.auto_category_id is required for auto mode"
                } else if (categoryId !in categoryIds) {
                    errors += "command_of_day.auto_category_id '$categoryId' not found in categories"
                } else {
                    val pool = CommandOfDayResolver.buildPool(bundle.commands, categoryId)
                    if (pool.isEmpty()) {
                        errors += "command_of_day auto pool for category '$categoryId' is empty"
                    } else {
                        val resolvedDate = LocalDate.parse(cod.resolved_date)
                        val expected = CommandOfDayResolver.resolveCommandId(pool, resolvedDate, cod.auto_seed)
                        if (cod.command_id != expected) {
                            errors += "command_of_day.command_id '${
                                cod.command_id
                            }' does not match resolver result '$expected' for ${cod.resolved_date}"
                        }
                    }
                }
            }
        }
        return errors
    }

    companion object {
        const val MODE_MANUAL = "manual"
        const val MODE_AUTO = "auto"
        private val MODES = setOf(MODE_MANUAL, MODE_AUTO)
        private val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}

object CommandOfDayPolicy {
    fun matches(settings: ru.appforsale.alicecommands.api.domain.CommandOfDaySettings, published: CommandOfDay?): Boolean {
        if (published == null) return false
        return policyKey(settings.mode, settings.auto_category_id, settings.auto_seed, settings.command_id) ==
            policyKey(published.mode, published.auto_category_id, published.auto_seed, published.command_id)
    }

    private fun policyKey(mode: String, autoCategoryId: String?, autoSeed: Int, commandId: String): String =
        listOf(
            mode,
            autoCategoryId.orEmpty(),
            autoSeed.toString(),
            if (mode == CommandOfDayValidationUseCase.MODE_MANUAL) commandId else "",
        ).joinToString("|")
}

object CommandOfDayBundleBuilder {

    fun build(
        settings: CommandOfDaySettings,
        commands: List<ru.appforsale.alicecommands.api.domain.Command>,
        resolvedDate: LocalDate = CommandOfDayResolver.todayMoscow(),
    ): CommandOfDay {
        val resolvedDateStr = resolvedDate.toString()
        val commandId = when (settings.mode) {
            CommandOfDayValidationUseCase.MODE_AUTO -> {
                val categoryId = settings.auto_category_id
                    ?: throw IllegalStateException("auto_category_id required for auto mode")
                val pool = CommandOfDayResolver.buildPool(commands, categoryId)
                CommandOfDayResolver.resolveCommandId(pool, resolvedDate, settings.auto_seed)
            }
            else -> settings.command_id
        }
        return CommandOfDay(
            mode = settings.mode,
            command_id = commandId,
            auto_category_id = settings.auto_category_id,
            auto_seed = settings.auto_seed,
            resolved_date = resolvedDateStr,
            updated_at = settings.updated_at,
        )
    }
}
