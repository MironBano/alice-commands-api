package ru.appforsale.alicecommands.api

import io.ktor.server.application.Application
import ru.appforsale.alicecommands.api.application.publish.ImportJsonUseCase
import ru.appforsale.alicecommands.api.application.publish.PreviewBundleUseCase
import ru.appforsale.alicecommands.api.application.publish.PublishContentUseCase
import ru.appforsale.alicecommands.api.application.publish.RollbackPublishUseCase
import ru.appforsale.alicecommands.api.application.read.AffiliateService
import ru.appforsale.alicecommands.api.application.read.BundleService
import ru.appforsale.alicecommands.api.application.read.ContentDiffService
import ru.appforsale.alicecommands.api.application.read.DraftPublishStatusService
import ru.appforsale.alicecommands.api.application.read.HealthService
import ru.appforsale.alicecommands.api.application.read.ManifestService
import ru.appforsale.alicecommands.api.config.AppConfig
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.HealthProbe
import ru.appforsale.alicecommands.api.domain.ports.LoginRateLimiter
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository
import ru.appforsale.alicecommands.api.domain.ports.SchemaValidator
import ru.appforsale.alicecommands.api.domain.ports.SessionRepository
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedDraftRepository
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedHealthProbe
import ru.appforsale.alicecommands.api.infrastructure.security.ExposedLoginRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.security.NoOpLoginRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedManifestRepository
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedSessionRepository
import ru.appforsale.alicecommands.api.infrastructure.persistence.initDatabase
import ru.appforsale.alicecommands.api.infrastructure.security.SessionSigner
import ru.appforsale.alicecommands.api.infrastructure.storage.FilesystemBundleStorage
import ru.appforsale.alicecommands.api.infrastructure.validation.JsonSchemaValidator
import org.jetbrains.exposed.sql.Database
import java.nio.file.Path
import kotlin.io.path.Path

data class AppDependencies(
    val config: AppConfig,
    val database: Database,
    val draftRepository: DraftRepository,
    val manifestRepository: ManifestRepository,
    val bundleStorage: BundleStorage,
    val schemaValidator: SchemaValidator,
    val sessionRepository: SessionRepository,
    val sessionSigner: SessionSigner,
    val loginRateLimiter: LoginRateLimiter,
    val healthProbe: HealthProbe,
    val manifestService: ManifestService,
    val bundleService: BundleService,
    val affiliateService: AffiliateService,
    val healthService: HealthService,
    val draftPublishStatusService: DraftPublishStatusService,
    val publishContentUseCase: PublishContentUseCase,
    val rollbackPublishUseCase: RollbackPublishUseCase,
    val importJsonUseCase: ImportJsonUseCase,
    val previewBundleUseCase: PreviewBundleUseCase,
    val contentDiffService: ContentDiffService,
)

val Application.deps: AppDependencies
    get() = attributes[AppAttributesKey]

private val AppAttributesKey = io.ktor.util.AttributeKey<AppDependencies>("deps")

fun Application.initDependencies(config: AppConfig = AppConfig.load()): AppDependencies {
    val database = initDatabase(config)
    val draftRepository = ExposedDraftRepository(database)
    val manifestRepository = ExposedManifestRepository(database)
    val bundleStorage = FilesystemBundleStorage(
        bundlePath = config.bundleStoragePath,
        manifestPath = config.manifestStoragePath,
        json = BundleCodec.json,
    )
    val schemaPath = resolveSchemaPath()
    val schemaValidator = JsonSchemaValidator(schemaPath, BundleCodec.json)
    val sessionRepository = ExposedSessionRepository(database)
    sessionRepository.cleanupExpired()
    val sessionSigner = SessionSigner(config.sessionSecret)
    val loginRateLimiter = if (config.env == "local") {
        NoOpLoginRateLimiter()
    } else {
        ExposedLoginRateLimiter(database, config.adminLoginRateLimit)
    }
    val healthProbe = ExposedHealthProbe(database)

    val deps = AppDependencies(
        config = config,
        database = database,
        draftRepository = draftRepository,
        manifestRepository = manifestRepository,
        bundleStorage = bundleStorage,
        schemaValidator = schemaValidator,
        sessionRepository = sessionRepository,
        sessionSigner = sessionSigner,
        loginRateLimiter = loginRateLimiter,
        healthProbe = healthProbe,
        manifestService = ManifestService(manifestRepository, config),
        bundleService = BundleService(manifestRepository, bundleStorage),
        affiliateService = AffiliateService(bundleStorage),
        healthService = HealthService(healthProbe, bundleStorage),
        draftPublishStatusService = DraftPublishStatusService(draftRepository, manifestRepository, bundleStorage),
        publishContentUseCase = PublishContentUseCase(
            draftRepository, manifestRepository, bundleStorage, schemaValidator, config.bundleRetentionCount,
        ),
        rollbackPublishUseCase = RollbackPublishUseCase(manifestRepository, bundleStorage),
        importJsonUseCase = ImportJsonUseCase(draftRepository, schemaValidator),
        previewBundleUseCase = PreviewBundleUseCase(draftRepository),
        contentDiffService = ContentDiffService(manifestRepository, bundleStorage, schemaValidator),
    )
    attributes.put(AppAttributesKey, deps)
    return deps
}

private fun resolveSchemaPath(): Path {
    val candidates = listOf(
        Path("schema/content-bundle.schema.json"),
        Path("../schema/content-bundle.schema.json"),
    )
    return candidates.first { it.toFile().exists() }
}
