package ru.appforsale.alicecommands.api.application.publish

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ValidationException

class CommandGroupValidationTest {

    private val validator = CommandGroupValidationUseCase()

    private fun baseBundle(
        groups: List<CommandGroup> = emptyList(),
        commands: List<Command> = emptyList(),
    ) = ContentBundle(
        schema_version = 2,
        published_at = "2026-06-29T00:00:00Z",
        categories = listOf(
            Category(
                id = "smart_home",
                title_ru = "Умный дом",
                sort_order = 1,
                source_url = "https://example.com",
            ),
        ),
        command_groups = groups,
        commands = commands,
    )

    private fun command(
        id: String,
        groupId: String? = null,
        sortOrder: Int? = null,
        primary: Boolean = false,
        aliases: List<String> = emptyList(),
    ) = Command(
        id = id,
        category_id = "smart_home",
        title_ru = "Test $id",
        phrases = listOf("Алиса, $id"),
        effect_description_ru = "Effect",
        requires_alice_word = true,
        source_url = "https://example.com",
        updated_at = "2026-06-29T00:00:00Z",
        group_id = groupId,
        sort_order = sortOrder,
        is_primary_in_group = primary,
        search_aliases = aliases,
    )

    @Test
    fun `rejects unknown group category`() {
        val bundle = baseBundle(
            groups = listOf(
                CommandGroup(
                    id = "g1",
                    category_id = "missing",
                    title_ru = "G",
                    sort_order = 10,
                ),
            ),
        )
        val ex = assertThrows(ValidationException::class.java) { validator.validateForPublish(bundle) }
        assertTrue(ex.errors.any { it.contains("unknown category_id") })
    }

    @Test
    fun `rejects empty group`() {
        val bundle = baseBundle(
            groups = listOf(
                CommandGroup(
                    id = "smart_home_light",
                    category_id = "smart_home",
                    title_ru = "Свет",
                    sort_order = 10,
                ),
            ),
        )
        val ex = assertThrows(ValidationException::class.java) { validator.validateForPublish(bundle) }
        assertTrue(ex.errors.any { it.contains("no commands") })
    }

    @Test
    fun `rejects duplicate primary`() {
        val bundle = baseBundle(
            groups = listOf(
                CommandGroup(
                    id = "smart_home_light",
                    category_id = "smart_home",
                    title_ru = "Свет",
                    sort_order = 10,
                ),
            ),
            commands = listOf(
                command("a", "smart_home_light", 10, primary = true),
                command("b", "smart_home_light", 20, primary = true),
            ),
        )
        val ex = assertThrows(ValidationException::class.java) { validator.validateForPublish(bundle) }
        assertTrue(ex.errors.any { it.contains("more than one is_primary_in_group") })
    }

    @Test
    fun `duplicate aliases with title are warning only`() {
        val bundle = baseBundle(
            groups = listOf(
                CommandGroup(
                    id = "smart_home_light",
                    category_id = "smart_home",
                    title_ru = "Свет",
                    sort_order = 10,
                ),
            ),
            commands = listOf(
                command("a", "smart_home_light", 10, primary = true, aliases = listOf("test a")),
            ),
        )
        validator.validateForPublish(bundle)
        assertTrue(validator.collectWarnings(bundle).duplicate_alias_commands.contains("a"))
    }

    @Test
    fun `valid grouped bundle passes`() {
        val bundle = baseBundle(
            groups = listOf(
                CommandGroup(
                    id = "smart_home_light",
                    category_id = "smart_home",
                    title_ru = "Свет",
                    sort_order = 10,
                    preview_command_ids = listOf("a"),
                ),
            ),
            commands = listOf(
                command("a", "smart_home_light", 10, primary = true, aliases = listOf("лампа")),
            ),
        )
        validator.validateForPublish(bundle)
        assertEquals(emptyList<String>(), validator.collectWarnings(bundle).orphan_commands)
        assertEquals(emptyList<String>(), validator.collectWarnings(bundle).empty_groups)
    }
}
