package ru.appforsale.alicecommands.api.infrastructure.validation

import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.appforsale.alicecommands.api.domain.SmartHomeDevicesResponse
import ru.appforsale.alicecommands.api.domain.ValidationException
import ru.appforsale.alicecommands.api.domain.ports.SmartHomeDevicesSchemaValidator
import java.nio.file.Path
import kotlin.io.path.readText

class JsonSmartHomeDevicesSchemaValidator(
    schemaPath: Path,
    private val json: Json,
) : SmartHomeDevicesSchemaValidator {

    private val schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        .getSchema(schemaPath.toUri())
    private val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()

    override fun validate(response: SmartHomeDevicesResponse) {
        validateJson(json.encodeToString(response))
    }

    override fun validateJson(jsonText: String) {
        val node = objectMapper.readTree(jsonText)
        val errors = schema.validate(node)
        if (errors.isNotEmpty()) {
            throw ValidationException(errors.map { "${it.instanceLocation}: ${it.message}" })
        }
    }
}
