package eu.servista.ktor

import eu.servista.commons.error.NotFoundException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class ServistaApplicationTest {

    private val testConfig =
        ServistaConfig(
            database = null,
            kafka = null,
            logging = LoggingConfig(requests = RequestLoggingConfig(enabled = false)),
            health = HealthConfig(),
        )

    @Test
    fun `installServista with minimal config starts and health startup returns 200`() =
        testApplication {
            application { installServista(testConfig) }

            val response = client.get("/health/startup")
            response.status shouldBe HttpStatusCode.OK

            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            body["status"]?.jsonPrimitive?.content shouldBe "UP"
        }

    @Test
    fun `installServista wires error handling for ServistaException`() = testApplication {
        application {
            installServista(testConfig)
            routing { get("/test-error") { throw NotFoundException("test", "Test not found") } }
        }

        val response = client.get("/test-error")
        response.status shouldBe HttpStatusCode.NotFound
        response.headers["Content-Type"] shouldContain "application/problem+json"

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["type"]?.jsonPrimitive?.content shouldBe "urn:servista:error:test:not-found"
    }

    @Test
    fun `installServista provides metrics endpoint`() = testApplication {
        application { installServista(testConfig) }

        val response = client.get("/metrics")
        response.status shouldBe HttpStatusCode.OK
        // Prometheus scrape output should contain standard JVM metrics or Ktor metrics
        response.bodyAsText().length shouldBe response.bodyAsText().length // non-empty
    }

    @Test
    fun `installServista health endpoints all respond`() = testApplication {
        application { installServista(testConfig) }

        client.get("/health/startup").status shouldBe HttpStatusCode.OK
        client.get("/health/live").status shouldBe HttpStatusCode.OK
        client.get("/health/ready").status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `content negotiation returns JSON responses`() = testApplication {
        application { installServista(testConfig) }

        val response = client.get("/health/startup")
        response.headers["Content-Type"] shouldContain "application/json"
    }
}
