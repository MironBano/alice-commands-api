package ru.appforsale.alicecommands.api

import io.ktor.server.application.Application
import ru.appforsale.alicecommands.api.application.analytics.AnalyticsBreakdownUseCase
import ru.appforsale.alicecommands.api.application.analytics.AnalyticsDashboardUseCase
import ru.appforsale.alicecommands.api.application.analytics.AnalyticsFunnelUseCase
import ru.appforsale.alicecommands.api.application.analytics.ListAnalyticsEventsUseCase
import ru.appforsale.alicecommands.api.application.analytics.SubmitAnalyticsBatchUseCase
import ru.appforsale.alicecommands.api.application.feedback.DismissCommandReportUseCase
import ru.appforsale.alicecommands.api.application.feedback.DismissFeedbackUseCase
import ru.appforsale.alicecommands.api.application.feedback.FeedbackInboxCountsUseCase
import ru.appforsale.alicecommands.api.application.feedback.ListCommandReportsUseCase
import ru.appforsale.alicecommands.api.application.feedback.ListFeedbackUseCase
import ru.appforsale.alicecommands.api.application.feedback.PublishedBundleLookup
import ru.appforsale.alicecommands.api.application.feedback.ReportCommandIssueUseCase
import ru.appforsale.alicecommands.api.application.feedback.ResolveCommandReportUseCase
import ru.appforsale.alicecommands.api.application.feedback.ResolveFeedbackUseCase
import ru.appforsale.alicecommands.api.application.feedback.SubmitFeedbackUseCase
import ru.appforsale.alicecommands.api.application.publish.ApproveQueueItemUseCase
import ru.appforsale.alicecommands.api.application.publish.DismissQueueItemUseCase
import ru.appforsale.alicecommands.api.application.publish.ImportEditorialReviewUseCase
import ru.appforsale.alicecommands.api.application.publish.ImportJsonUseCase
import ru.appforsale.alicecommands.api.application.publish.SaveEditorialBatchUseCase
import ru.appforsale.alicecommands.api.application.publish.PreviewBundleUseCase
import ru.appforsale.alicecommands.api.application.publish.PublishAffiliateUseCase
import ru.appforsale.alicecommands.api.application.publish.PublishSmartHomeDevicesUseCase
import ru.appforsale.alicecommands.api.application.publish.SmartHomeDevicesValidationUseCase
import ru.appforsale.alicecommands.api.application.publish.UploadDeviceImageUseCase
import ru.appforsale.alicecommands.api.application.publish.PublishContentUseCase
import ru.appforsale.alicecommands.api.application.publish.PublishCommandOfDayUseCase
import ru.appforsale.alicecommands.api.application.publish.RollbackPublishUseCase
import ru.appforsale.alicecommands.api.application.publish.RebuildDraftFromPipelineUseCase
import ru.appforsale.alicecommands.api.application.publish.SyncPipelineUseCase
import ru.appforsale.alicecommands.api.application.read.AffiliateService
import ru.appforsale.alicecommands.api.application.read.SmartHomeDevicesService
import ru.appforsale.alicecommands.api.application.read.BundleService
import ru.appforsale.alicecommands.api.application.publish.CategoryVisualValidationUseCase
import ru.appforsale.alicecommands.api.application.publish.CommandOfDayAdminUseCase
import ru.appforsale.alicecommands.api.application.publish.CommandOfDayValidationUseCase
import ru.appforsale.alicecommands.api.application.publish.CommandGroupValidationUseCase
import ru.appforsale.alicecommands.api.application.publish.IconCatalogService
import ru.appforsale.alicecommands.api.application.publish.UploadIconUseCase
import ru.appforsale.alicecommands.api.application.read.ContentDeltaService
import ru.appforsale.alicecommands.api.application.read.ContentDiffService
import ru.appforsale.alicecommands.api.application.read.DraftPublishStatusService
import ru.appforsale.alicecommands.api.application.read.EditorialReviewService
import ru.appforsale.alicecommands.api.application.read.HealthService
import ru.appforsale.alicecommands.api.application.read.ManifestService
import ru.appforsale.alicecommands.api.config.AppConfig
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsEventRepository
import ru.appforsale.alicecommands.api.domain.ports.AnalyticsRateLimiter
import ru.appforsale.alicecommands.api.domain.ports.BundleStorage
import ru.appforsale.alicecommands.api.domain.ports.ContentPipelineRepository
import ru.appforsale.alicecommands.api.domain.ports.DraftRepository
import ru.appforsale.alicecommands.api.domain.ports.HealthProbe
import ru.appforsale.alicecommands.api.domain.ports.LoginRateLimiter
import ru.appforsale.alicecommands.api.domain.ports.ManifestRepository
import ru.appforsale.alicecommands.api.domain.ports.PublicSubmissionRateLimiter
import ru.appforsale.alicecommands.api.domain.ports.UserFeedbackRepository
import ru.appforsale.alicecommands.api.domain.ports.SchemaValidator
import ru.appforsale.alicecommands.api.domain.ports.SessionRepository
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedAnalyticsEventRepository
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedContentPipelineRepository
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedDraftRepository
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedHealthProbe
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedUserFeedbackRepository
import ru.appforsale.alicecommands.api.infrastructure.security.ExposedAnalyticsRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.security.ExposedLoginRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.security.ExposedPublicSubmissionRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.security.NoOpAnalyticsRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.security.NoOpPublicSubmissionRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.security.NoOpLoginRateLimiter
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedManifestRepository
import ru.appforsale.alicecommands.api.infrastructure.persistence.ExposedSessionRepository
import ru.appforsale.alicecommands.api.infrastructure.persistence.initDatabase
import ru.appforsale.alicecommands.api.infrastructure.security.SessionSigner
import ru.appforsale.alicecommands.api.infrastructure.storage.FilesystemBundleStorage
import ru.appforsale.alicecommands.api.infrastructure.storage.FilesystemIconStorage
import ru.appforsale.alicecommands.api.domain.ports.SmartHomeDevicesSchemaValidator
import ru.appforsale.alicecommands.api.infrastructure.storage.FilesystemDeviceImageStorage
import ru.appforsale.alicecommands.api.infrastructure.validation.JsonSchemaValidator
import ru.appforsale.alicecommands.api.infrastructure.validation.JsonSmartHomeDevicesSchemaValidator
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
    val smartHomeDevicesService: SmartHomeDevicesService,
    val healthService: HealthService,
    val draftPublishStatusService: DraftPublishStatusService,
    val publishAffiliateUseCase: PublishAffiliateUseCase,
    val publishSmartHomeDevicesUseCase: PublishSmartHomeDevicesUseCase,
    val smartHomeDevicesValidationUseCase: SmartHomeDevicesValidationUseCase,
    val uploadDeviceImageUseCase: UploadDeviceImageUseCase,
    val publishContentUseCase: PublishContentUseCase,
    val publishCommandOfDayUseCase: PublishCommandOfDayUseCase,
    val rollbackPublishUseCase: RollbackPublishUseCase,
    val importJsonUseCase: ImportJsonUseCase,
    val previewBundleUseCase: PreviewBundleUseCase,
    val contentDiffService: ContentDiffService,
    val contentDeltaService: ContentDeltaService,
    val commandGroupValidationUseCase: CommandGroupValidationUseCase,
    val categoryVisualValidationUseCase: CategoryVisualValidationUseCase,
    val commandOfDayValidationUseCase: CommandOfDayValidationUseCase,
    val commandOfDayAdminUseCase: CommandOfDayAdminUseCase,
    val uploadIconUseCase: UploadIconUseCase,
    val iconCatalogService: IconCatalogService,
    val editorialReviewService: EditorialReviewService,
    val contentPipelineRepository: ContentPipelineRepository,
    val syncPipelineUseCase: SyncPipelineUseCase,
    val approveQueueItemUseCase: ApproveQueueItemUseCase,
    val dismissQueueItemUseCase: DismissQueueItemUseCase,
    val rebuildDraftFromPipelineUseCase: RebuildDraftFromPipelineUseCase,
    val importEditorialReviewUseCase: ImportEditorialReviewUseCase,
    val saveEditorialBatchUseCase: SaveEditorialBatchUseCase,
    val userFeedbackRepository: UserFeedbackRepository,
    val publicSubmissionRateLimiter: PublicSubmissionRateLimiter,
    val publishedBundleLookup: PublishedBundleLookup,
    val submitFeedbackUseCase: SubmitFeedbackUseCase,
    val reportCommandIssueUseCase: ReportCommandIssueUseCase,
    val listFeedbackUseCase: ListFeedbackUseCase,
    val listCommandReportsUseCase: ListCommandReportsUseCase,
    val resolveFeedbackUseCase: ResolveFeedbackUseCase,
    val dismissFeedbackUseCase: DismissFeedbackUseCase,
    val resolveCommandReportUseCase: ResolveCommandReportUseCase,
    val dismissCommandReportUseCase: DismissCommandReportUseCase,
    val feedbackInboxCountsUseCase: FeedbackInboxCountsUseCase,
    val analyticsEventRepository: AnalyticsEventRepository,
    val analyticsRateLimiter: AnalyticsRateLimiter,
    val submitAnalyticsBatchUseCase: SubmitAnalyticsBatchUseCase,
    val analyticsDashboardUseCase: AnalyticsDashboardUseCase,
    val listAnalyticsEventsUseCase: ListAnalyticsEventsUseCase,
    val analyticsFunnelUseCase: AnalyticsFunnelUseCase,
    val analyticsBreakdownUseCase: AnalyticsBreakdownUseCase,
)

val Application.deps: AppDependencies
    get() = attributes[AppAttributesKey]

private val AppAttributesKey = io.ktor.util.AttributeKey<AppDependencies>("deps")

fun Application.initDependencies(config: AppConfig = AppConfig.load()): AppDependencies {
    val database = initDatabase(config)
    val draftRepository = ExposedDraftRepository(database)
    val contentPipelineRepository = ExposedContentPipelineRepository(database)
    val manifestRepository = ExposedManifestRepository(database)
    val bundleStorage = FilesystemBundleStorage(
        bundlePath = config.bundleStoragePath,
        manifestPath = config.manifestStoragePath,
        json = BundleCodec.json,
    )
    val iconStorage = FilesystemIconStorage(
        rootPath = config.iconStoragePath,
        publicBaseUrl = config.iconPublicBaseUrl,
    )
    val deviceImageStorage = FilesystemDeviceImageStorage(
        rootPath = config.deviceImageStoragePath,
        publicBaseUrl = config.iconPublicBaseUrl,
    )
    seedDefaultIconsIfNeeded(config, iconStorage)
    val schemaPath = resolveSchemaPath()
    val schemaValidator = JsonSchemaValidator(schemaPath, BundleCodec.json)
    val smartHomeDevicesSchemaPath = resolveSmartHomeDevicesSchemaPath()
    val smartHomeDevicesSchemaValidator = JsonSmartHomeDevicesSchemaValidator(smartHomeDevicesSchemaPath, BundleCodec.json)
    val sessionRepository = ExposedSessionRepository(database)
    sessionRepository.cleanupExpired()
    val sessionSigner = SessionSigner(config.sessionSecret)
    val loginRateLimiter = if (config.env == "local") {
        NoOpLoginRateLimiter()
    } else {
        ExposedLoginRateLimiter(database, config.adminLoginRateLimit)
    }
    val publicSubmissionRateLimiter = if (config.env == "local") {
        NoOpPublicSubmissionRateLimiter()
    } else {
        ExposedPublicSubmissionRateLimiter(database, config.publicSubmissionRateLimit)
    }
    val userFeedbackRepository = ExposedUserFeedbackRepository(database)
    val analyticsEventRepository = ExposedAnalyticsEventRepository(database)
    val analyticsRateLimiter = if (config.env == "local") {
        NoOpAnalyticsRateLimiter()
    } else {
        ExposedAnalyticsRateLimiter(
            database = database,
            maxRequestsPerWindow = config.analyticsRateLimitPerIp,
            maxEventsPerDay = config.analyticsEventsPerIpPerDay,
            eventRepository = analyticsEventRepository,
        )
    }
    val publishedBundleLookup = PublishedBundleLookup(manifestRepository, bundleStorage)
    val healthProbe = ExposedHealthProbe(database)

    val rebuildDraftFromPipelineUseCase = RebuildDraftFromPipelineUseCase(contentPipelineRepository, draftRepository)
    val importEditorialReviewUseCase = ImportEditorialReviewUseCase(contentPipelineRepository, rebuildDraftFromPipelineUseCase)
    val saveEditorialBatchUseCase = SaveEditorialBatchUseCase(importEditorialReviewUseCase)

    val commandGroupValidationUseCase = CommandGroupValidationUseCase()
    val categoryVisualValidationUseCase = CategoryVisualValidationUseCase(config.iconUrlAllowedHosts)
    val commandOfDayValidationUseCase = CommandOfDayValidationUseCase()
    val commandOfDayAdminUseCase = CommandOfDayAdminUseCase(draftRepository)
    val smartHomeDevicesValidationUseCase = SmartHomeDevicesValidationUseCase(config.iconUrlAllowedHosts)
    val uploadDeviceImageUseCase = UploadDeviceImageUseCase(deviceImageStorage, smartHomeDevicesValidationUseCase)
    val publishSmartHomeDevicesUseCase = PublishSmartHomeDevicesUseCase(
        draftRepository,
        bundleStorage,
        smartHomeDevicesValidationUseCase,
        smartHomeDevicesSchemaValidator,
    )
    val uploadIconUseCase = UploadIconUseCase(iconStorage, categoryVisualValidationUseCase)
    val iconCatalogService = IconCatalogService(
        catalogPath = config.iconCatalogPath,
        iconStorage = iconStorage,
        json = BundleCodec.json,
    )
    val contentDeltaService = ContentDeltaService(manifestRepository, bundleStorage)

    val draftPublishStatusService = DraftPublishStatusService(draftRepository, manifestRepository, bundleStorage)

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
        smartHomeDevicesService = SmartHomeDevicesService(bundleStorage),
        healthService = HealthService(healthProbe, bundleStorage),
        draftPublishStatusService = draftPublishStatusService,
        publishAffiliateUseCase = PublishAffiliateUseCase(draftRepository, bundleStorage),
        publishSmartHomeDevicesUseCase = publishSmartHomeDevicesUseCase,
        smartHomeDevicesValidationUseCase = smartHomeDevicesValidationUseCase,
        uploadDeviceImageUseCase = uploadDeviceImageUseCase,
        publishContentUseCase = PublishContentUseCase(
            draftRepository, manifestRepository, bundleStorage, schemaValidator,
            commandGroupValidationUseCase, categoryVisualValidationUseCase,
            commandOfDayValidationUseCase, config.bundleRetentionCount,
        ),
        publishCommandOfDayUseCase = PublishCommandOfDayUseCase(
            draftRepository,
            manifestRepository,
            bundleStorage,
            schemaValidator,
            commandOfDayValidationUseCase,
            draftPublishStatusService,
            config.bundleRetentionCount,
        ),
        rollbackPublishUseCase = RollbackPublishUseCase(manifestRepository, bundleStorage),
        importJsonUseCase = ImportJsonUseCase(
            draftRepository, contentPipelineRepository, schemaValidator, categoryVisualValidationUseCase,
        ),
        previewBundleUseCase = PreviewBundleUseCase(draftRepository),
        contentDiffService = ContentDiffService(draftRepository, manifestRepository, bundleStorage, schemaValidator),
        contentDeltaService = contentDeltaService,
        commandGroupValidationUseCase = commandGroupValidationUseCase,
        categoryVisualValidationUseCase = categoryVisualValidationUseCase,
        commandOfDayValidationUseCase = commandOfDayValidationUseCase,
        commandOfDayAdminUseCase = commandOfDayAdminUseCase,
        uploadIconUseCase = uploadIconUseCase,
        iconCatalogService = iconCatalogService,
        editorialReviewService = EditorialReviewService(
            draftRepository, manifestRepository, bundleStorage, contentPipelineRepository,
        ),
        contentPipelineRepository = contentPipelineRepository,
        syncPipelineUseCase = SyncPipelineUseCase(contentPipelineRepository),
        approveQueueItemUseCase = ApproveQueueItemUseCase(contentPipelineRepository, draftRepository),
        dismissQueueItemUseCase = DismissQueueItemUseCase(contentPipelineRepository),
        rebuildDraftFromPipelineUseCase = rebuildDraftFromPipelineUseCase,
        importEditorialReviewUseCase = importEditorialReviewUseCase,
        saveEditorialBatchUseCase = saveEditorialBatchUseCase,
        userFeedbackRepository = userFeedbackRepository,
        publicSubmissionRateLimiter = publicSubmissionRateLimiter,
        publishedBundleLookup = publishedBundleLookup,
        submitFeedbackUseCase = SubmitFeedbackUseCase(userFeedbackRepository, publicSubmissionRateLimiter),
        reportCommandIssueUseCase = ReportCommandIssueUseCase(
            userFeedbackRepository,
            publicSubmissionRateLimiter,
            publishedBundleLookup,
        ),
        listFeedbackUseCase = ListFeedbackUseCase(userFeedbackRepository),
        listCommandReportsUseCase = ListCommandReportsUseCase(userFeedbackRepository),
        resolveFeedbackUseCase = ResolveFeedbackUseCase(userFeedbackRepository),
        dismissFeedbackUseCase = DismissFeedbackUseCase(userFeedbackRepository),
        resolveCommandReportUseCase = ResolveCommandReportUseCase(userFeedbackRepository),
        dismissCommandReportUseCase = DismissCommandReportUseCase(userFeedbackRepository),
        feedbackInboxCountsUseCase = FeedbackInboxCountsUseCase(userFeedbackRepository),
        analyticsEventRepository = analyticsEventRepository,
        analyticsRateLimiter = analyticsRateLimiter,
        submitAnalyticsBatchUseCase = SubmitAnalyticsBatchUseCase(analyticsEventRepository, analyticsRateLimiter),
        analyticsDashboardUseCase = AnalyticsDashboardUseCase(
            analyticsEventRepository,
            config.analyticsRawRetentionDays,
        ),
        listAnalyticsEventsUseCase = ListAnalyticsEventsUseCase(
            analyticsEventRepository,
            config.analyticsRawRetentionDays,
        ),
        analyticsFunnelUseCase = AnalyticsFunnelUseCase(
            analyticsEventRepository,
            config.analyticsRawRetentionDays,
        ),
        analyticsBreakdownUseCase = AnalyticsBreakdownUseCase(
            analyticsEventRepository,
            config.analyticsRawRetentionDays,
        ),
    )
    if (bundleStorage.readSmartHomeDevices() == null &&
        (draftRepository.listDeviceGuides().isNotEmpty() || draftRepository.listDevicePicks().isNotEmpty())
    ) {
        runCatching { publishSmartHomeDevicesUseCase.execute() }
            .onFailure { error ->
                org.slf4j.LoggerFactory.getLogger("AppDependencies")
                    .warn("Smart home devices auto-publish failed: {}", error.message)
            }
    }
    attributes.put(AppAttributesKey, deps)
    return deps
}

private fun resolveSmartHomeDevicesSchemaPath(): Path {
    val candidates = listOf(
        Path("schema/smarthome-devices.schema.json"),
        Path("../schema/smarthome-devices.schema.json"),
    )
    return candidates.first { it.toFile().exists() }
}

private fun resolveSchemaPath(): Path {
    val candidates = listOf(
        Path("schema/content-bundle.schema.json"),
        Path("../schema/content-bundle.schema.json"),
    )
    return candidates.first { it.toFile().exists() }
}

private fun seedDefaultIconsIfNeeded(
    config: AppConfig,
    iconStorage: FilesystemIconStorage,
) {
    val sourceDirs = listOf(
        Path("content/icons/v1"),
        Path("../content/icons/v1"),
    )
    val sourceDir = sourceDirs.firstOrNull { it.toFile().isDirectory } ?: return
    sourceDir.toFile().listFiles { f -> f.isFile && f.extension.equals("svg", ignoreCase = true) }
        ?.forEach { file ->
            val slug = file.nameWithoutExtension
            if (!iconStorage.exists(slug)) {
                iconStorage.store(slug, file.readText())
            }
        }
}

