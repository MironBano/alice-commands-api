package ru.appforsale.alicecommands.api.application.popular

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.domain.Command
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ports.PublishedCatalogProvider

class PublishedBundleCatalogReaderTest {

    @Test
    fun loadLiveCatalog_mapsIdsAndTitlesFromSingleBundleLoad() {
        var loads = 0
        val provider = object : PublishedCatalogProvider {
            override fun loadCurrentBundle(): ContentBundle? {
                loads++
                return sampleBundle()
            }
        }
        val reader = PublishedBundleCatalogReader(provider)

        val snapshot = reader.loadLiveCatalog()

        assertEquals(1, loads)
        assertEquals(setOf("music_muzyka", "calls_pozvoni"), snapshot.commandIds)
        assertEquals(
            mapOf(
                "music_muzyka" to "Музыка",
                "calls_pozvoni" to "Позвони",
            ),
            snapshot.titlesById,
        )
        assertEquals(
            mapOf("music_muzyka" to "Музыка"),
            snapshot.titlesFor(setOf("music_muzyka")),
        )
    }

    @Test
    fun loadLiveCatalog_emptyWhenNoPublishedBundle() {
        val reader = PublishedBundleCatalogReader(object : PublishedCatalogProvider {
            override fun loadCurrentBundle(): ContentBundle? = null
        })

        val snapshot = reader.loadLiveCatalog()

        assertTrue(snapshot.commandIds.isEmpty())
        assertTrue(snapshot.titlesById.isEmpty())
    }

    private fun sampleBundle() = ContentBundle(
        published_at = "2026-08-27T00:00:00Z",
        commands = listOf(
            Command(
                id = "music_muzyka",
                category_id = "music",
                title_ru = "Музыка",
                phrases = listOf("музыка"),
                effect_description_ru = "effect",
                requires_alice_word = true,
                source_url = "https://example.com",
                updated_at = "2026-08-27T00:00:00Z",
            ),
            Command(
                id = "calls_pozvoni",
                category_id = "calls",
                title_ru = "Позвони",
                phrases = listOf("позвони"),
                effect_description_ru = "effect",
                requires_alice_word = true,
                source_url = "https://example.com",
                updated_at = "2026-08-27T00:00:00Z",
            ),
        ),
    )
}
