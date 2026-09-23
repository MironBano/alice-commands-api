package ru.appforsale.alicecommands.api

import ru.appforsale.alicecommands.api.config.AppConfig
import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.appforsale.alicecommands.api.plugins.configureSerialization
import ru.appforsale.alicecommands.api.plugins.configureStatusPages
import ru.appforsale.alicecommands.api.routes.adminRoutes
import ru.appforsale.alicecommands.api.routes.analyticsRoutes
import ru.appforsale.alicecommands.api.routes.feedbackRoutes
import ru.appforsale.alicecommands.api.routes.pushRoutes
import ru.appforsale.alicecommands.api.routes.healthRoutes
import ru.appforsale.alicecommands.api.routes.publicRoutes
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun main() {
    val config = AppConfig.load()
    embeddedServer(CIO, port = config.port, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig.load()) {
    val deps = initDependencies(config)
    install(CallLogging)
    install(Compression) { gzip() }
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
    }
    configureSerialization()
    configureStatusPages()

    launch {
        // Initial purge on startup; then daily.
        runCatching {
            deps.purgeAnalyticsEventsUseCase.execute()
        }.onFailure { error ->
            org.slf4j.LoggerFactory.getLogger("AnalyticsRetentionTicker")
                .warn("Initial analytics retention purge failed: {}", error.message)
        }
        while (isActive) {
            delay(24.hours)
            runCatching {
                deps.purgeAnalyticsEventsUseCase.execute()
            }.onFailure { error ->
                org.slf4j.LoggerFactory.getLogger("AnalyticsRetentionTicker")
                    .warn("Analytics retention purge failed: {}", error.message)
            }
        }
    }

    launch {
        // Initial compute if snapshot empty; then every 6 hours.
        runCatching {
            if (deps.popularCommandsRepository.getSnapshot().isEmpty()) {
                deps.rankPopularCommandsUseCase.execute(trigger = "ticker")
            }
        }.onFailure { error ->
            org.slf4j.LoggerFactory.getLogger("PopularCommandsTicker")
                .warn("Initial popular rank failed: {}", error.message)
        }
        while (isActive) {
            delay(6.hours)
            runCatching {
                deps.rankPopularCommandsUseCase.execute(trigger = "ticker")
            }.onFailure { error ->
                org.slf4j.LoggerFactory.getLogger("PopularCommandsTicker")
                    .warn("Popular rank ticker failed: {}", error.message)
            }
        }
    }

    if (config.pushCampaignEnabled) {
        launch {
            // Align to ~15 min cadence for COD window (±15m) and quiet-hours defer.
            delay(30.seconds)
            while (isActive) {
                runCatching {
                    deps.runPushCampaignsUseCase.execute()
                }.onFailure { error ->
                    org.slf4j.LoggerFactory.getLogger("PushCampaignTicker")
                        .warn("Push campaign tick failed: {}", error.message)
                }
                delay(15.minutes)
            }
        }
    }

    if (config.marketAffiliateRefreshEnabled) {
        launch {
            delay(45.seconds)
            while (isActive) {
                runCatching {
                    val result = deps.refreshAffiliatePicksUseCase.execute()
                    if (!result.skipped) {
                        org.slf4j.LoggerFactory.getLogger("AffiliateRefreshTicker")
                            .info(
                                "Affiliate refresh refreshed={} failedOver={} deactivated={}",
                                result.refreshed,
                                result.failedOver,
                                result.deactivated,
                            )
                    }
                }.onFailure { error ->
                    org.slf4j.LoggerFactory.getLogger("AffiliateRefreshTicker")
                        .warn("Affiliate refresh tick failed: {}", error.message)
                }
                delay(6.hours)
            }
        }
    }

    routing {
        publicRoutes()
        feedbackRoutes()
        pushRoutes()
        analyticsRoutes()
        healthRoutes()
        adminRoutes()
        staticFiles("/icons", config.iconStoragePath.toFile()) {
            cacheControl { listOf(CacheControl.MaxAge(maxAgeSeconds = 86400)) }
        }
        staticFiles("/devices", config.deviceImageStoragePath.toFile()) {
            cacheControl { listOf(CacheControl.MaxAge(maxAgeSeconds = 86400)) }
        }
        staticFiles("/announcements", config.announcementImageStoragePath.toFile()) {
            cacheControl { listOf(CacheControl.MaxAge(maxAgeSeconds = 86400)) }
        }
        val adminDir = when (config.env) {
            "local" -> Path("admin-web").toFile().takeIf { it.exists() }
            else -> Path("server/build/resources/main/admin").toFile().takeIf { it.exists() }
                ?: Path("admin-web").toFile().takeIf { it.exists() }
        }
        if (adminDir != null) {
            staticFiles("/admin", adminDir) {
                default("index.html")
                cacheControl {
                    if (config.env == "local") emptyList()
                    else listOf(CacheControl.MaxAge(maxAgeSeconds = 0))
                }
            }
        } else {
            staticResources("/admin", "admin") {
                default("index.html")
                cacheControl { listOf(CacheControl.MaxAge(maxAgeSeconds = 0)) }
            }
        }
    }
}
