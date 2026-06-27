package ru.appforsale.alicecommands.api.infrastructure.validation

import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.domain.ValidationException
import ru.appforsale.alicecommands.api.domain.ports.SchemaValidator
import java.nio.file.Path
import kotlin.io.path.readText

class JsonSchemaValidator(
    schemaPath: Path,
    private val json: Json,
) : SchemaValidator {

    private val schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        .getSchema(schemaPath.toUri())

    override fun validate(bundle: ContentBundle) {
        validateJson(json.encodeToString(bundle))
    }

    override fun validateJson(jsonText: String) {
        val node = com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonText)
        val errors = schema.validate(node)
        if (errors.isNotEmpty()) {
            throw ValidationException(errors.map { "${it.instanceLocation}: ${it.message}" })
        }
    }
}
