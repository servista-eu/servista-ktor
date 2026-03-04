package eu.servista.ktor.metrics

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/**
 * Install MicrometerMetrics with a Prometheus registry.
 *
 * Returns the [PrometheusMeterRegistry] for use in the `/metrics` scrape endpoint and Koin DI
 * registration.
 */
fun Application.installMetrics(): PrometheusMeterRegistry {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) { this.registry = registry }
    return registry
}
