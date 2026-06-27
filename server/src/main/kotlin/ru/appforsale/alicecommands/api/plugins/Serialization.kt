package ru.appforsale.alicecommands.api.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import ru.appforsale.alicecommands.api.application.BundleCodec

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(BundleCodec.json)
    }
}
