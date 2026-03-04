package eu.servista.ktor.health

import eu.servista.commons.health.HealthRegistry
import eu.servista.commons.health.HealthResponse
import eu.servista.commons.health.HealthStatus
import eu.servista.ktor.serialization.servistaJson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.encodeToString

/**
 * Register Kubernetes probe health check endpoints.
 *
 * - `/health/startup` -- Startup probe (ready to accept traffic)
 * - `/health/live` -- Liveness probe (process is running correctly)
 * - `/health/ready` -- Readiness probe (ready to handle requests)
 *
 * Maps [HealthStatus.UP] and [HealthStatus.DEGRADED] to 200 OK, [HealthStatus.DOWN] to 503
 * Service Unavailable.
 */
fun Route.healthRoutes(healthRegistry: HealthRegistry) {
    route("/health") {
        get("/startup") {
            val response = healthRegistry.checkStartup()
            call.respondHealth(response)
        }

        get("/live") {
            val response = healthRegistry.checkLiveness()
            call.respondHealth(response)
        }

        get("/ready") {
            val response = healthRegistry.checkReadiness()
            call.respondHealth(response)
        }
    }
}

/**
 * Respond with a health check result.
 *
 * Since [HealthResponse] from servista-commons is not annotated with `@Serializable`, we manually
 * serialize using a serializable DTO to avoid requiring changes to the upstream library.
 */
private suspend fun io.ktor.server.routing.RoutingCall.respondHealth(response: HealthResponse) {
    val status =
        when (response.status) {
            HealthStatus.UP,
            HealthStatus.DEGRADED -> HttpStatusCode.OK
            HealthStatus.DOWN -> HttpStatusCode.ServiceUnavailable
        }

    val dto = HealthResponseDto.from(response)
    respondText(
        text = servistaJson.encodeToString(dto),
        contentType = ContentType.Application.Json,
        status = status,
    )
}
