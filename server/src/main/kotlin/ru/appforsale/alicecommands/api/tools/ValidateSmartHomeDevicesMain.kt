package ru.appforsale.alicecommands.api.tools

import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.domain.SmartHomeDevicesResponse
import ru.appforsale.alicecommands.api.infrastructure.validation.JsonSmartHomeDevicesSchemaValidator
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val contentFile = args.firstOrNull()
        ?: System.getProperty("contentFile")
        ?: "seed/smarthome-devices-example.json"

    val path = Path(contentFile)
    if (!path.exists()) {
        System.err.println("Content file not found: $contentFile")
        exitProcess(1)
    }

    val schemaCandidates = listOf(
        Path("schema/smarthome-devices.schema.json"),
        Path("../schema/smarthome-devices.schema.json"),
    )
    val schemaPath = schemaCandidates.firstOrNull { it.exists() }
        ?: run {
            System.err.println("Schema not found")
            exitProcess(1)
        }

    val validator = JsonSmartHomeDevicesSchemaValidator(schemaPath, BundleCodec.json)
    val jsonText = path.readText()
    try {
        validator.validateJson(jsonText)
        val response = BundleCodec.json.decodeFromString<SmartHomeDevicesResponse>(jsonText)
        println(
            "OK: ${path.toAbsolutePath()} — " +
                "${response.guides.size} guides, ${response.picks.size} picks",
        )
    } catch (e: ru.appforsale.alicecommands.api.domain.ValidationException) {
        System.err.println("Validation failed:")
        e.errors.forEach { System.err.println("  - $it") }
        exitProcess(1)
    }
}
