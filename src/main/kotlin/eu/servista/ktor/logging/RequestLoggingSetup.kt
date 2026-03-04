package eu.servista.ktor.logging

import eu.servista.ktor.RequestLoggingConfig
import io.ktor.server.application.Application
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.application.install
import io.ktor.server.request.header
import io.ktor.server.request.path

/**
 * Install Ktor CallLogging plugin with MDC correlation data and health endpoint filtering.
 *
 * Only installs if [config.enabled] is true. Filters out requests matching [config.excludePaths]
 * patterns using prefix matching. Adds MDC entries for correlation_id, organization_id, and
 * account_id extracted from request headers.
 */
fun Application.installRequestLogging(config: RequestLoggingConfig) {
    if (!config.enabled) return

    install(CallLogging) {
        level = config.logLevel

        filter { call ->
            config.excludePaths.none { pattern ->
                call.request.path().startsWith(pattern.removeSuffix("*"))
            }
        }

        mdc("correlation_id") { call -> call.request.header("X-Correlation-Id") }
        mdc("organization_id") { call -> call.request.header("X-Organization-Id") }
        mdc("account_id") { call -> call.request.header("X-Account-Id") }
    }
}
