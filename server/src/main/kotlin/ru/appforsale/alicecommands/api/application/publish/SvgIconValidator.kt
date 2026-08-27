package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.ValidationException

object SvgIconValidator {
    private val slugRegex = Regex("^[a-z][a-z0-9_]*$")
    private val maxBytes = 4 * 1024
    private val eventHandlerRegex = Regex("""\bon[a-z]+\s*=""")

    fun validateAndNormalize(svg: String, slug: String): String {
        val trimmed = svg.trim()
        if (trimmed.isEmpty()) {
            throw ValidationException(listOf("svg: empty content"))
        }
        val bytes = trimmed.toByteArray(Charsets.UTF_8)
        if (bytes.size > maxBytes) {
            throw ValidationException(listOf("svg: exceeds 4 KB limit (${bytes.size} bytes)"))
        }
        if (!slugRegex.matches(slug)) {
            throw ValidationException(listOf("slug: must be snake_case latin (e.g. music_note)"))
        }
        val lower = trimmed.lowercase()
        if ("<script" in lower || "javascript:" in lower || eventHandlerRegex.containsMatchIn(lower)) {
            throw ValidationException(listOf("svg: script tags or event handlers are not allowed"))
        }
        if ("<foreignobject" in lower) {
            throw ValidationException(listOf("svg: foreignObject is not allowed"))
        }
        if (!lower.contains("<svg")) {
            throw ValidationException(listOf("svg: must be a valid SVG document"))
        }
        if (!lower.contains("viewbox=\"0 0 24 24\"") && !lower.contains("viewbox='0 0 24 24'")) {
            throw ValidationException(listOf("svg: viewBox must be 0 0 24 24"))
        }
        return trimmed
    }

    fun slugFromFilename(filename: String): String {
        val base = filename.substringBeforeLast('.').lowercase()
        return base.replace('-', '_')
    }
}
