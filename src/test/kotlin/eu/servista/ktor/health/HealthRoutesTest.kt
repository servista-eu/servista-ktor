package eu.servista.ktor.health

import eu.servista.commons.health.BuildInfo
import eu.servista.commons.health.HealthCheckResult
import eu.servista.commons.health.HealthRegistry
import eu.servista.commons.health.HealthStatus
import eu.servista.ktor.serialization.installContentNegotiation
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class HealthRoutesTest {

    private val testBuildInfo =
        BuildInfo(
            version = "0.1.0-test",
            gitSha = "abc1234",
            buildTimestamp = "2026-03-04T00:00:00Z",
        )

    @Test
    fun `startup returns 200 UP with empty checks and buildInfo`() = testApplication {
        val registry = HealthRegistry(testBuildInfo)

        application {
            installContentNegotiation()
            routing { healthRoutes(registry) }
        }

        val response = client.get("/health/startup")
        response.status shouldBe HttpStatusCode.OK

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["status"]?.jsonPrimitive?.content shouldBe "UP"
        body["checks"]?.jsonArray?.size shouldBe 0
        body["buildInfo"]?.jsonObject?.get("version")?.jsonPrimitive?.content shouldBe "0.1.0-test"
    }

    @Test
    fun `ready returns 503 when a DOWN check is registered`() = testApplication {
        val registry = HealthRegistry(testBuildInfo)
        registry.registerReadiness("database") {
            HealthCheckResult("database", HealthStatus.DOWN, error = "Connection refused")
        }

        application {
            installContentNegotiation()
            routing { healthRoutes(registry) }
        }

        val response = client.get("/health/ready")
        response.status shouldBe HttpStatusCode.ServiceUnavailable

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["status"]?.jsonPrimitive?.content shouldBe "DOWN"
        val checks = body["checks"]?.jsonArray
        checks?.size shouldBe 1
        checks?.get(0)?.jsonObject?.get("name")?.jsonPrimitive?.content shouldBe "database"
    }

    @Test
    fun `ready returns 200 when all checks are UP`() = testApplication {
        val registry = HealthRegistry(testBuildInfo)
        registry.registerReadiness("database") {
            HealthCheckResult("database", HealthStatus.UP, details = mapOf("type" to "postgresql"))
        }

        application {
            installContentNegotiation()
            routing { healthRoutes(registry) }
        }

        val response = client.get("/health/ready")
        response.status shouldBe HttpStatusCode.OK

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["status"]?.jsonPrimitive?.content shouldBe "UP"
    }

    @Test
    fun `live returns 200 with empty checks when no liveness checks registered`() =
        testApplication {
            val registry = HealthRegistry(testBuildInfo)

            application {
                installContentNegotiation()
                routing { healthRoutes(registry) }
            }

            val response = client.get("/health/live")
            response.status shouldBe HttpStatusCode.OK

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "UP"
            body["checks"]?.jsonArray?.size shouldBe 0
        }

    @Test
    fun `response body contains JSON with correct structure`() = testApplication {
        val registry = HealthRegistry(testBuildInfo)

        application {
            installContentNegotiation()
            routing { healthRoutes(registry) }
        }

        val response = client.get("/health/startup")
        val bodyText = response.bodyAsText()

        bodyText shouldContain "\"status\""
        bodyText shouldContain "\"checks\""
        bodyText shouldContain "\"buildInfo\""
        bodyText shouldContain "\"version\""
        bodyText shouldContain "\"gitSha\""
        bodyText shouldContain "\"buildTimestamp\""
    }
}
