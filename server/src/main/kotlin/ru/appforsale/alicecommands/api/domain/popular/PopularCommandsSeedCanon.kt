package ru.appforsale.alicecommands.api.domain.popular

/**
 * Canonical seed/denylist ids shared with the Android app emergency fallback.
 * Keep in sync with `OrganicPopularCommands` / Flyway `V11__popular_commands.sql` denylist seed.
 */
object PopularCommandsSeedCanon {
    val SEED_IDS: List<String> = listOf(
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

    val DENYLIST_IDS: Set<String> = setOf(
        "music_vkliuchi_muzyku",
        "general_gromche",
        "quick_commands_dalshe",
        "timers_postav_taimer_na_5_minut",
    )
}
