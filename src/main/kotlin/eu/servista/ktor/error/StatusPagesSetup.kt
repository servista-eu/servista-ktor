package eu.servista.ktor.error

import eu.servista.commons.error.ProblemDetail
import eu.servista.commons.error.ServistaException
import eu.servista.ktor.serialization.servistaJson
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import kotlinx.serialization.encodeToString

private val logger = KotlinLogging.logger {}

private val problemJsonContentType = ContentType("application", "problem+json")

/**
 * Install StatusPages with exception handlers for Servista error types.
 *
 * Maps [ServistaException] to RFC 7807 ProblemDetail JSON responses with `application/problem+json`
 * content type. Unhandled exceptions produce a generic 500 response.
 */
fun Application.installStatusPages() {
    install(StatusPages) {
        exception<ServistaException> { call, cause ->
            val problemDetail = cause.toProblemDetail()
            call.respondText(
                text = servistaJson.encodeToString(problemDetail),
                contentType = problemJsonContentType,
                status = HttpStatusCode.fromValue(cause.statusCode),
            )
        }

        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unhandled exception" }
            val problemDetail =
                ProblemDetail(
                    type = "urn:servista:error:internal:unhandled",
                    title = "Internal Server Error",
                    status = 500,
                )
            call.respondText(
                text = servistaJson.encodeToString(problemDetail),
                contentType = problemJsonContentType,
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
}
