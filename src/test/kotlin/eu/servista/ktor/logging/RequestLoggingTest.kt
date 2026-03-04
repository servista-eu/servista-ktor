package eu.servista.ktor.logging

import eu.servista.ktor.RequestLoggingConfig
import eu.servista.ktor.serialization.installContentNegotiation
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

class RequestLoggingTest {

    @Test
    fun `request with X-Correlation-Id header is processed without error`() = testApplication {
        application {
            installContentNegotiation()
            installRequestLogging(RequestLoggingConfig())
            routing { get("/api/test") { call.respondText("ok") } }
        }

        val response =
            client.get("/api/test") { header("X-Correlation-Id", "test-correlation-123") }

        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `request without X-Correlation-Id header still succeeds`() = testApplication {
        application {
            installContentNegotiation()
            installRequestLogging(RequestLoggingConfig())
            routing { get("/api/test") { call.respondText("ok") } }
        }

        val response = client.get("/api/test")
        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `request to health endpoint is excluded from logging but still responds`() =
        testApplication {
            application {
                installContentNegotiation()
                installRequestLogging(RequestLoggingConfig(excludePaths = listOf("/health")))
                routing {
                    get("/health/ready") { call.respondText("healthy") }
                    get("/api/data") { call.respondText("data") }
                }
            }

            // Health endpoint still responds normally (just excluded from logging)
            val healthResponse = client.get("/health/ready")
            healthResponse.status shouldBe HttpStatusCode.OK

            // Non-health endpoint also responds
            val apiResponse = client.get("/api/data")
            apiResponse.status shouldBe HttpStatusCode.OK
        }

    @Test
    fun `logging is not installed when disabled`() = testApplication {
        application {
            installContentNegotiation()
            installRequestLogging(RequestLoggingConfig(enabled = false))
            routing { get("/api/test") { call.respondText("ok") } }
        }

        // Should work fine even without logging installed
        val response = client.get("/api/test")
        response.status shouldBe HttpStatusCode.OK
    }
}
