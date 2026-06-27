package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.domain.ContentBundle
import ru.appforsale.alicecommands.api.application.BundleCodec
import kotlin.io.path.Path

class BundleCodecTest {

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
        val schemaPath = listOf(
            Path("schema/content-bundle.schema.json"),
            Path("../schema/content-bundle.schema.json"),
        ).first { it.toFile().exists() }
        val seed = listOf(
            Path("seed/import-smart-home.json"),
            Path("../seed/import-smart-home.json"),
        ).first { it.toFile().exists() }.toFile().readText()
        val validator = ru.appforsale.alicecommands.api.infrastructure.validation.JsonSchemaValidator(
            schemaPath,
            BundleCodec.json,
        )
        validator.validateJson(seed)
    }
}
