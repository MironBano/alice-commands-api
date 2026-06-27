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
import ru.appforsale.alicecommands.api.plugins.configureSerialization
import ru.appforsale.alicecommands.api.plugins.configureStatusPages
import ru.appforsale.alicecommands.api.routes.adminRoutes
import ru.appforsale.alicecommands.api.routes.healthRoutes
import ru.appforsale.alicecommands.api.routes.publicRoutes
import kotlin.io.path.Path

fun main() {
    val config = AppConfig.load()
    embeddedServer(CIO, port = config.port, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig.load()) {
    initDependencies(config)
    install(CallLogging)
    install(Compression) { gzip() }
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
    }
    configureSerialization()
    configureStatusPages()

    routing {
        publicRoutes()
        healthRoutes()
        adminRoutes()
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
                    else listOf(CacheControl.MaxAge(maxAgeSeconds = 3600))
                }
            }
        } else {
            staticResources("/admin", "admin") {
                default("index.html")
                cacheControl { listOf(CacheControl.MaxAge(maxAgeSeconds = 3600)) }
            }
        }
    }
}
