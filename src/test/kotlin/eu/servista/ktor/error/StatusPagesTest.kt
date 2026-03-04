package eu.servista.ktor.error

import eu.servista.commons.error.ConflictException
import eu.servista.commons.error.NotFoundException
import eu.servista.ktor.serialization.installContentNegotiation
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

class StatusPagesTest {

    @Test
    fun `ServistaException NotFound returns 404 with application-problem+json`() = testApplication {
        application {
            installContentNegotiation()
            installStatusPages()
            routing {
                get("/test-not-found") { throw NotFoundException("user", "User 42 not found") }
            }
        }

        val response = client.get("/test-not-found")

        response.status shouldBe HttpStatusCode.NotFound
        response.headers["Content-Type"] shouldContain "application/problem+json"

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["type"]?.jsonPrimitive?.content shouldBe "urn:servista:error:user:not-found"
        body["title"]?.jsonPrimitive?.content shouldBe "Not Found"
        body["status"]?.jsonPrimitive?.content shouldBe "404"
    }

    @Test
    fun `ServistaException Conflict returns 409 with correct ProblemDetail`() = testApplication {
        application {
            installContentNegotiation()
            installStatusPages()
            routing {
                get("/test-conflict") { throw ConflictException("order", "Order already exists") }
            }
        }

        val response = client.get("/test-conflict")

        response.status shouldBe HttpStatusCode.Conflict
        response.headers["Content-Type"] shouldContain "application/problem+json"

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["type"]?.jsonPrimitive?.content shouldBe "urn:servista:error:order:conflict"
        body["title"]?.jsonPrimitive?.content shouldBe "Conflict"
        body["status"]?.jsonPrimitive?.content shouldBe "409"
    }

    @Test
    fun `generic RuntimeException returns 500 with unhandled ProblemDetail`() = testApplication {
        application {
            installContentNegotiation()
            installStatusPages()
            routing {
                get("/test-unhandled") {
                    @Suppress("TooGenericExceptionThrown") throw RuntimeException("Something broke")
                }
            }
        }

        val response = client.get("/test-unhandled")

        response.status shouldBe HttpStatusCode.InternalServerError
        response.headers["Content-Type"] shouldContain "application/problem+json"

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        body["type"]?.jsonPrimitive?.content shouldBe "urn:servista:error:internal:unhandled"
        body["title"]?.jsonPrimitive?.content shouldBe "Internal Server Error"
        body["status"]?.jsonPrimitive?.content shouldBe "500"
    }
}
