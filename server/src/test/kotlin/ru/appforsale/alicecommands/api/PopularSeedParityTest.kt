package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.domain.popular.PopularCommandsSeedCanon

class PopularSeedParityTest {

    @Test
    fun seedIds_matchAppOrganicPopularCommandsIds() {
        assertEquals(APP_ORGANIC_POPULAR_COMMANDS_IDS, PopularCommandsSeedCanon.SEED_IDS)
    }

    @Test
    fun denylistSeed_matchesAppContaminatedIds() {
        assertEquals(APP_CONTAMINATED_IDS, PopularCommandsSeedCanon.DENYLIST_IDS)
    }

    @Test
    fun flywayV11DenylistSeed_matchesCanon() {
        val migration = this::class.java.classLoader
            .getResource("db/migration/V11__popular_commands.sql")
            ?.readText()
            ?: error("Missing V11__popular_commands.sql")
        val seeded = migration
            .substringAfter("INSERT INTO popular_command_denylist")
            .substringBefore("ON CONFLICT")
            .lineSequence()
            .mapNotNull { line ->
                Regex("""\('([^']+)',\s*'""").find(line)?.groupValues?.get(1)
            }
            .toSet()
        assertEquals(PopularCommandsSeedCanon.DENYLIST_IDS, seeded)
    }

    companion object {
        /** Mirror of app OrganicPopularCommands / PopularCommandsSeedCanon. */
        private val APP_ORGANIC_POPULAR_COMMANDS_IDS = listOf(
            "music_luchshie_kompozitsii",
            "music_muzyka",
            "smart_home_vkliuchi_girliandu",
            "sh_light_dim",
            "sh_light_off",
            "calls_pozvoni",
            "music_muzyku_gromche",
            "music_avtoradio",
            "music_bitlz",
        )

        private val APP_CONTAMINATED_IDS = setOf(
            "music_vkliuchi_muzyku",
            "general_gromche",
            "quick_commands_dalshe",
            "timers_postav_taimer_na_5_minut",
        )
    }
}
