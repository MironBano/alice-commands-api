package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.domain.Category
import ru.appforsale.alicecommands.api.domain.CommandGroup
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.application.BundleCodec

class BundleCodecTest {

    @Test
    fun `serializes category visual fields`() {
        val bundle = ContentBundle(
            published_at = "2026-07-01T00:00:00Z",
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
            command_groups = listOf(
                CommandGroup(
                    id = "g1",
                    category_id = "music",
                    title_ru = "G",
                    sort_order = 1,
                    icon_url = "https://cdn.alicecommands.ru/icons/v1/lightbulb.svg",
                ),
            ),
        )
        val json = BundleCodec.toJson(bundle)
        assertTrue(json.contains("\"icon_url\""))
        assertTrue(json.contains("#7B4BB7"))
        val roundTrip = BundleCodec.json.decodeFromString<ContentBundle>(json)
        assertEquals("#C9A8F0", roundTrip.categories.single().accent_color_dark)
        assertEquals("https://cdn.alicecommands.ru/icons/v1/lightbulb.svg", roundTrip.command_groups.single().icon_url)
    }

    @Test
    fun `gzip and sha256 are stable`() {
        val bundle = ContentBundle(
            published_at = "2026-06-26T12:00:00Z",
            categories = emptyList(),
            commands = emptyList(),
        )
        val json = BundleCodec.toJson(bundle)
        val gzip = BundleCodec.gzip(json)
        assertTrue(gzip.isNotEmpty())
        val sha = BundleCodec.sha256(gzip)
        assertEquals(64, sha.length)
        assertEquals(sha, BundleCodec.sha256(gzip))
    }
}

class JsonSchemaValidatorTest {

    @Test
    fun `seed import passes schema validation`() {
        val seed = TestResourcePaths.readText(TestResourcePaths.INTEGRATION_SEED)
        val validator = ru.appforsale.alicecommands.api.infrastructure.validation.JsonSchemaValidator(
            TestResourcePaths.resolve("schema/content-bundle.schema.json"),
            BundleCodec.json,
        )
        validator.validateJson(seed)
    }
}
