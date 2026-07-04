package ru.appforsale.alicecommands.api

import kotlin.io.path.Path
import kotlin.io.path.exists

object TestResourcePaths {
    const val INTEGRATION_SEED = "seed/integration-smart-home.json"

    fun readText(relativePath: String): String =
        resolve(relativePath).toFile().readText()

    fun resolve(relativePath: String): java.nio.file.Path =
        listOf(Path(relativePath), Path("..", relativePath))
            .first { it.exists() }
}
