package ru.appforsale.alicecommands.api.tools

import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.infrastructure.validation.JsonSchemaValidator
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.system.exitProcess

/**
 * CLI: validate content JSON against schema/content-bundle.schema.json
 * Usage: validateContent <path-to-json>
 */
fun main(args: Array<String>) {
    val contentFile = args.firstOrNull()
        ?: System.getProperty("contentFile")
        ?: "seed/catalog-audit-fixed.json"

    val path = Path(contentFile)
    if (!path.exists()) {
        System.err.println("Content file not found: $contentFile")
        exitProcess(1)
    }

    val schemaCandidates = listOf(
        Path("schema/content-bundle.schema.json"),
        Path("../schema/content-bundle.schema.json"),
    )
    val schemaPath = schemaCandidates.firstOrNull { it.exists() }
        ?: run {
            System.err.println("Schema not found")
            exitProcess(1)
        }

    val validator = JsonSchemaValidator(schemaPath, BundleCodec.json)
    val jsonText = path.readText()
    try {
        validator.validateJson(jsonText)
        val bundle = BundleCodec.json.decodeFromString<ru.appforsale.alicecommands.api.domain.ContentBundle>(jsonText)
        println(
            "OK: ${path.toAbsolutePath()} — " +
                "${bundle.categories.size} categories, ${bundle.commands.size} commands",
        )
    } catch (e: ru.appforsale.alicecommands.api.domain.ValidationException) {
        System.err.println("Validation failed:")
        e.errors.forEach { System.err.println("  - $it") }
        exitProcess(1)
    }
}
