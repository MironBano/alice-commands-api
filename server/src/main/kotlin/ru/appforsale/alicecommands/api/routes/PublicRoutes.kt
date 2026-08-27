package ru.appforsale.alicecommands.api.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import ru.appforsale.alicecommands.api.deps
import ru.appforsale.alicecommands.api.domain.ApiError
import ru.appforsale.alicecommands.api.application.read.DeltaUnavailableException

fun Route.publicRoutes() {
    route("/v1/content") {
        get("/manifest") {
            val result = call.application.deps.manifestService.getManifestWithEtag()
            if (result == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "No published content yet"))
                return@get
            }
            val ifNoneMatch = call.request.headers[HttpHeaders.IfNoneMatch]
            if (ifNoneMatch == result.etag) {
                call.response.status(HttpStatusCode.NotModified)
                return@get
            }
            call.response.header(HttpHeaders.ETag, result.etag)
            call.response.header(HttpHeaders.CacheControl, "public, max-age=300")
            call.respond(result.manifest)
        }

        get("/bundle") {
            val bundle = call.application.deps.bundleService.getPublishedBundle()
            if (bundle == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "No published bundle"))
                return@get
            }
            val ifNoneMatch = call.request.headers[HttpHeaders.IfNoneMatch]
            if (ifNoneMatch == bundle.etag) {
                call.response.status(HttpStatusCode.NotModified)
                return@get
            }
            call.response.header(HttpHeaders.ETag, bundle.etag)
            call.response.header(HttpHeaders.CacheControl, "public, max-age=86400, immutable")
            call.response.header(HttpHeaders.ContentEncoding, "gzip")
            call.respondBytes(bundle.bytes, ContentType.Application.Json)
        }

        get("/bundle-backup/{filename}") {
            val filename = call.parameters["filename"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ApiError("validation_failed", "filename required"),
            )
            val bytes = call.application.deps.bundleService.getBackupBundle(filename)
            if (bytes == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "Bundle not found"))
            } else {
                call.response.header(HttpHeaders.ContentEncoding, "gzip")
                call.respondBytes(bytes, ContentType.Application.Json)
            }
        }

        get("/delta") {
            val fromParam = call.request.queryParameters["from"]
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("validation_failed", "from query parameter required"),
                )
            val fromVersion = fromParam.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("validation_failed", "from must be an integer"),
                )
            try {
                val delta = call.application.deps.contentDeltaService.getDelta(fromVersion)
                call.response.header(HttpHeaders.CacheControl, "public, max-age=60")
                call.respond(delta)
            } catch (e: DeltaUnavailableException) {
                call.respond(
                    HttpStatusCode.Conflict,
                    ApiError("delta_unavailable", e.message ?: "Use full bundle"),
                )
            }
        }
    }

    route("/v1/affiliate") {
        get("/blocks") {
            call.response.header("Deprecation", "true")
            call.response.header("Sunset", "Sat, 06 Dec 2026 00:00:00 GMT")
            call.response.header("Link", "</v1/smarthome/devices>; rel=\"successor-version\"")
            val blocks = call.application.deps.affiliateService.getPublishedBlocks()
            if (blocks == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "No published affiliate blocks"))
            } else {
                call.respond(blocks)
            }
        }
    }

    route("/v1/smarthome") {
        get("/devices") {
            val devices = call.application.deps.smartHomeDevicesService.getPublishedDevices()
            if (devices == null) {
                call.respond(HttpStatusCode.NotFound, ApiError("not_found", "No published smart home devices"))
            } else {
                call.response.header(HttpHeaders.CacheControl, "public, max-age=300")
                call.respond(devices)
            }
        }
    }
}

fun Route.healthRoutes() {
    get("/health") {
        call.respond(mapOf("status" to "ok"))
    }

    get("/ready") {
        val ready = call.application.deps.healthService.ready()
        call.respond(
            HttpStatusCode.fromValue(ready.httpStatus),
            mapOf(
                "status" to ready.status,
                "database" to ready.database,
                "storage" to ready.storage,
            ),
        )
    }
}
