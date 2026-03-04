package eu.servista.ktor.context

import eu.servista.commons.context.ServistaContext
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.header
import java.util.UUID
import kotlinx.coroutines.withContext

/**
 * Install a pipeline interceptor that extracts gateway headers into a [ServistaContext] coroutine
 * context element.
 *
 * Extracts:
 * - `X-Correlation-Id` (generates UUID if missing)
 * - `X-Organization-Id` (toLongOrNull)
 * - `X-Account-Id` (toLongOrNull)
 *
 * The [ServistaContext] is a [CopyableThreadContextElement] that automatically bridges MDC across
 * coroutine boundaries.
 */
fun Application.installContextInterceptor() {
    intercept(ApplicationCallPipeline.Plugins) {
        val correlationId =
            call.request.header("X-Correlation-Id") ?: UUID.randomUUID().toString()
        val organizationId = call.request.header("X-Organization-Id")?.toLongOrNull()
        val accountId = call.request.header("X-Account-Id")?.toLongOrNull()

        val ctx =
            ServistaContext(
                correlationId = correlationId,
                organizationId = organizationId,
                accountId = accountId,
            )

        withContext(ctx) { proceed() }
    }
}
