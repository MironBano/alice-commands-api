package ru.appforsale.alicecommands.api.application.publish

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ValidationException
import ru.appforsale.alicecommands.api.application.publish.SvgIconValidator

class CategoryVisualValidationTest {

    private val allowedHosts = setOf("cdn.alicecommands.ru", "localhost", "127.0.0.1")
    private val validator = CategoryVisualValidationUseCase(allowedHosts)

    private fun baseBundle(categories: List<Category> = emptyList(), groups: List<CommandGroup> = emptyList()) =
        ContentBundle(
            schema_version = 2,
            published_at = "2026-07-01T00:00:00Z",
            categories = categories,
            command_groups = groups,
        )

    @Test
    fun `rejects invalid accent hex`() {
        val bundle = baseBundle(
            categories = listOf(
                Category(
                    id = "music",
                    title_ru = "Музыка",
                    sort_order = 1,
                    source_url = "https://example.com",
                    accent_color = "purple",
                ),
            ),
        )
        val ex = assertThrows(ValidationException::class.java) { validator.validateForPublish(bundle) }
        assertTrue(ex.errors.any { it.contains("accent_color") })
    }

    @Test
    fun `rejects icon_url wrong host`() {
        val bundle = baseBundle(
            categories = listOf(
                Category(
                    id = "music",
                    title_ru = "Музыка",
                    sort_order = 1,
                    source_url = "https://example.com",
                    icon_url = "https://evil.example/icons/v1/music_note.svg",
                ),
            ),
        )
        val ex = assertThrows(ValidationException::class.java) { validator.validateForPublish(bundle) }
        assertTrue(ex.errors.any { it.contains("allowlist") })
    }

    @Test
    fun `accepts valid visual fields`() {
        val bundle = baseBundle(
            categories = listOf(
                Category(
                    id = "music",
                    title_ru = "Музыка",
                    sort_order = 1,
                    source_url = "https://example.com",
                    icon_key = "music_note",
                    icon_url = "https://cdn.alicecommands.ru/icons/v1/music_note.svg",
                    accent_color = "#7B4BB7",
                    accent_color_dark = "#C9A8F0",
                ),
            ),
            groups = listOf(
                CommandGroup(
                    id = "g1",
                    category_id = "music",
                    title_ru = "G",
                    sort_order = 1,
                    icon_url = "https://cdn.alicecommands.ru/icons/v1/lightbulb.svg",
                    icon_key = "lightbulb",
                ),
            ),
        )
        validator.validateForPublish(bundle)
    }

    @Test
    fun `rejects visual fields on commands in raw json`() {
        val json = """
            {
              "schema_version": 2,
              "content_version": 0,
              "published_at": "2026-07-01T00:00:00Z",
              "min_app_version": "1.0",
              "categories": [],
              "commands": [
                {
                  "id": "c1",
                  "category_id": "music",
                  "title_ru": "T",
                  "phrases": ["Алиса"],
                  "effect_description_ru": "E",
                  "requires_alice_word": true,
                  "source_url": "https://example.com",
                  "updated_at": "2026-07-01T00:00:00Z",
                  "icon_url": "https://cdn.alicecommands.ru/icons/v1/music_note.svg"
                }
              ],
              "scenario_templates": [],
              "checklist_items": []
            }
        """.trimIndent()
        val ex = assertThrows(ValidationException::class.java) {
            validator.validateJsonForForbiddenCommandFields(json)
        }
        assertTrue(ex.errors.any { it.contains("icon_url") })
    }

    @Test
    fun `svg validator rejects script tags`() {
        val ex = assertThrows(ValidationException::class.java) {
            SvgIconValidator.validateAndNormalize(
                """<svg viewBox="0 0 24 24"><script>alert(1)</script></svg>""",
                "bad",
            )
        }
        assertTrue(ex.errors.any { it.contains("script") })
    }

    @Test
    fun `svg validator rejects oversize content`() {
        val huge = "<svg viewBox=\"0 0 24 24\">" + "x".repeat(5000) + "</svg>"
        val ex = assertThrows(ValidationException::class.java) {
            SvgIconValidator.validateAndNormalize(huge, "big")
        }
        assertTrue(ex.errors.any { it.contains("4 KB") })
    }

    @Test
    fun `validateIconUrl accepts localhost http`() {
        val errors = validator.validateIconUrl(
            "test",
            "http://localhost:8080/icons/v1/music_note.svg",
        )
        assertEquals(emptyList<String>(), errors)
    }
}
