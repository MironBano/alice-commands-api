package ru.appforsale.alicecommands.api.application.publish

import kotlinx.serialization.json.Json
import ru.appforsale.alicecommands.api.domain.IconCatalog
import ru.appforsale.alicecommands.api.domain.UploadIconRequest
import ru.appforsale.alicecommands.api.domain.UploadIconResponse
import ru.appforsale.alicecommands.api.domain.ValidationException
import ru.appforsale.alicecommands.api.domain.ports.IconStorage
import java.nio.file.Files
import java.nio.file.Path

class UploadIconUseCase(
    private val iconStorage: IconStorage,
    private val categoryVisualValidation: CategoryVisualValidationUseCase,
) {
    fun execute(request: UploadIconRequest): UploadIconResponse {
        val slug = request.slug?.trim()?.lowercase()
            ?: throw ValidationException(listOf("slug is required"))
        val normalizedSvg = SvgIconValidator.validateAndNormalize(request.svg, slug)
        val iconUrl = iconStorage.store(slug, normalizedSvg)
        categoryVisualValidation.validateIconUrl("icon_url", iconUrl).let { errors ->
            if (errors.isNotEmpty()) throw ValidationException(errors)
        }
        return UploadIconResponse(slug = slug, icon_url = iconUrl, icon_key = slug)
    }
}

class IconCatalogService(
    private val catalogPath: Path,
    private val iconStorage: IconStorage,
    private val json: Json,
) {
    fun loadCatalog(): IconCatalog {
        val fileCatalog = if (catalogPath.toFile().exists()) {
            json.decodeFromString<IconCatalog>(Files.readString(catalogPath))
        } else {
            IconCatalog()
        }
        val fileIcons = fileCatalog.icons.map { entry ->
            entry.copy(url = iconStorage.iconUrl(entry.slug))
        }
        val storedIcons = iconStorage.listSlugs().map { slug ->
            ru.appforsale.alicecommands.api.domain.IconCatalogEntry(
                slug = slug,
                label_ru = slug.replace('_', ' '),
                url = iconStorage.iconUrl(slug),
            )
        }
        val mergedIcons = (fileIcons + storedIcons)
            .distinctBy { it.slug }
            .sortedBy { it.slug }
        return fileCatalog.copy(
            icons = mergedIcons,
            public_base_url = iconStorage.basePublicUrl(),
        )
    }
}
