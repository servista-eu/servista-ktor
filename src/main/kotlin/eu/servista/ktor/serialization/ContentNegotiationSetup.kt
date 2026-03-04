package eu.servista.ktor.serialization

import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

/** Shared JSON configuration used by ContentNegotiation and manual serialization. */
val servistaJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Install ContentNegotiation with kotlinx.serialization JSON.
 *
 * Registers for both `application/json` and `application/problem+json` content types so that RFC
 * 7807 error responses are properly serialized.
 */
fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        json(servistaJson)
        json(servistaJson, contentType = ContentType("application", "problem+json"))
    }
}
