package ru.appforsale.alicecommands.api.application.publish

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandOfDay
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ValidationException
import java.time.LocalDate

class CommandOfDayValidationTest {

    private val validator = CommandOfDayValidationUseCase()

    private fun baseCommand(id: String, categoryId: String = "music", sortOrder: Int = 1) = Command(
        id = id,
        category_id = categoryId,
        title_ru = "Title $id",
        phrases = listOf("Алиса, $id"),
        effect_description_ru = "Effect",
        requires_alice_word = true,
        source_url = "https://example.com",
        updated_at = "2026-07-01T00:00:00Z",
        sort_order = sortOrder,
    )

    private fun baseBundle(
        commands: List<Command> = listOf(baseCommand("cmd_a"), baseCommand("cmd_b", sortOrder = 2)),
        cod: CommandOfDay? = null,
    ) = ContentBundle(
        schema_version = 2,
        published_at = "2026-07-01T00:00:00Z",
        categories = listOf(
            Category(
                id = "music",
                title_ru = "Музыка",
                sort_order = 1,
                source_url = "https://example.com",
            ),
        ),
        commands = commands,
        command_of_day = cod,
    )

    @Test
    fun `rejects manual with auto_category_id set`() {
        val bundle = baseBundle(
            cod = CommandOfDay(
                mode = "manual",
                command_id = "cmd_a",
                auto_category_id = "music",
                resolved_date = "2026-07-01",
                updated_at = "2026-07-01T08:00:00Z",
            ),
        )
        val ex = assertThrows(ValidationException::class.java) { validator.validateForPublish(bundle) }
        assertTrue(ex.errors.any { it.contains("auto_category_id must be null") })
    }

    @Test
    fun `rejects auto when command_id mismatches resolver`() {
        val date = "2026-07-01"
        val pool = CommandOfDayResolver.buildPool(
            listOf(baseCommand("cmd_a"), baseCommand("cmd_b", sortOrder = 2)),
            "music",
        )
        val expected = CommandOfDayResolver.resolveCommandId(pool, LocalDate.parse(date))
        val wrongId = pool.first { it.id != expected }.id
        val bundle = baseBundle(
            cod = CommandOfDay(
                mode = "auto",
                command_id = wrongId,
                auto_category_id = "music",
                resolved_date = date,
                updated_at = "2026-07-01T08:00:00Z",
            ),
        )
        val ex = assertThrows(ValidationException::class.java) { validator.validateForPublish(bundle) }
        assertTrue(ex.errors.any { it.contains("does not match resolver") })
    }

    @Test
    fun `accepts valid auto snapshot`() {
        val date = "2026-07-01"
        val pool = CommandOfDayResolver.buildPool(
            listOf(baseCommand("cmd_a"), baseCommand("cmd_b", sortOrder = 2)),
            "music",
        )
        val expected = CommandOfDayResolver.resolveCommandId(pool, LocalDate.parse(date))
        val bundle = baseBundle(
            cod = CommandOfDay(
                mode = "auto",
                command_id = expected,
                auto_category_id = "music",
                resolved_date = date,
                updated_at = "2026-07-01T08:00:00Z",
            ),
        )
        validator.validateForPublish(bundle)
    }

    @Test
    fun `rejects missing command_id in catalog`() {
        val bundle = baseBundle(
            cod = CommandOfDay(
                mode = "manual",
                command_id = "missing",
                resolved_date = "2026-07-01",
                updated_at = "2026-07-01T08:00:00Z",
            ),
        )
        val ex = assertThrows(ValidationException::class.java) { validator.validateForPublish(bundle) }
        assertTrue(ex.errors.any { it.contains("not found in commands") })
    }

    @Test
    fun `bundle builder produces matching auto snapshot`() {
        val commands = listOf(baseCommand("cmd_a"), baseCommand("cmd_b", sortOrder = 2))
        val date = LocalDate.parse("2026-07-01")
        val settings = ru.appforsale.alicecommands.api.domain.CommandOfDaySettings(
            mode = "auto",
            command_id = "placeholder",
            auto_category_id = "music",
            auto_seed = 31,
            updated_at = "2026-07-01T08:00:00Z",
        )
        val built = CommandOfDayBundleBuilder.build(settings, commands, date)
        val expected = CommandOfDayResolver.resolveCommandId(
            CommandOfDayResolver.buildPool(commands, "music"),
            date,
        )
        assertEquals(expected, built.command_id)
        assertEquals("2026-07-01", built.resolved_date)
    }
}
