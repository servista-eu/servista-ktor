package eu.servista.ktor

import eu.servista.commons.health.HealthRegistry
import eu.servista.ktor.context.installContextInterceptor
import eu.servista.ktor.di.registerDatabaseHealthCheck
import eu.servista.ktor.di.servistaModule
import eu.servista.ktor.error.installStatusPages
import eu.servista.ktor.health.healthRoutes
import eu.servista.ktor.logging.installRequestLogging
import eu.servista.ktor.metrics.installMetrics
import eu.servista.ktor.serialization.installContentNegotiation
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.jooq.DSLContext
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin

/**
 * Single entry point for all Ktor plugin installation.
 *
 * Loads configuration from HOCON `application.conf` and installs all Servista runtime plugins in
 * the correct dependency order. Services call this in their Application module to get consistent
 * runtime behavior.
 *
 * Installation order is critical:
 * 1. ContentNegotiation (JSON serialization -- must be before StatusPages for JSON error responses)
 * 2. StatusPages (error handling, needs serializer)
 * 3. CallLogging (request logging with MDC)
 * 4. MicrometerMetrics (metrics)
 * 5. Context interceptor (header extraction)
 * 6. Koin (DI -- provides HealthRegistry, DataSource, DSLContext, Kafka, etc.)
 * 7. Health routes (via routing)
 * 8. Metrics route (/metrics)
 */
fun Application.installServista() {
    val config = ServistaConfig.load(environment.config)
    installServista(config)
}

/**
 * Overload accepting a pre-built [ServistaConfig] for testing scenarios where HOCON config is not
 * available.
 */
fun Application.installServista(config: ServistaConfig) {
    // 1. Content negotiation (JSON serialization)
    installContentNegotiation()

    // 2. Status pages (error handling)
    installStatusPages()

    // 3. Request logging with MDC
    installRequestLogging(config.logging.requests)

    // 4. Metrics (returns the registry for DI and /metrics route)
    val prometheusRegistry = installMetrics()

    // 5. Context interceptor (extract gateway headers into ServistaContext)
    installContextInterceptor()

    // 6. Koin DI with servistaModule
    install(Koin) {
        modules(
            servistaModule(config),
            org.koin.dsl.module {
                single { prometheusRegistry }
                single { config }
            },
        )
    }

    // Register database health check if database is configured
    if (config.database != null) {
        val koin = getKoin()
        val healthRegistry = koin.get<HealthRegistry>()
        val dslContext = koin.get<DSLContext>()
        registerDatabaseHealthCheck(healthRegistry, dslContext)
    }

    // 7. Health routes + 8. Metrics route
    val healthRegistry = getKoin().get<HealthRegistry>()
    routing {
        healthRoutes(healthRegistry)
        metricsRoute(prometheusRegistry)
    }
}

/** Metrics scrape endpoint at `/metrics` for Prometheus. */
private fun Route.metricsRoute(registry: PrometheusMeterRegistry) {
    get("/metrics") { call.respondText(registry.scrape()) }
}
