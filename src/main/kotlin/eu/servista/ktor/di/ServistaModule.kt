package eu.servista.ktor.di

import eu.servista.commons.health.BuildInfo
import eu.servista.commons.health.HealthCheckResult
import eu.servista.commons.health.HealthRegistry
import eu.servista.commons.health.HealthStatus
import eu.servista.commons.rls.RlsSessionManager
import eu.servista.ktor.ServistaConfig
import eu.servista.ktor.database.migrateDatabase
import eu.servista.ktor.kafka.KafkaConsumerProperties
import eu.servista.ktor.kafka.ServistaKafkaProducer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.koin.dsl.module

private val logger = KotlinLogging.logger {}

/**
 * Creates the Koin DI module for Servista infrastructure services.
 *
 * Always provides:
 * - [HealthRegistry] for probe endpoints
 *
 * Conditionally provides when database is configured:
 * - [javax.sql.DataSource] (HikariDataSource) with Flyway auto-migration
 * - [org.jooq.DSLContext] backed by the HikariDataSource
 * - [RlsSessionManager] for tenant isolation
 * - Database readiness health check
 *
 * Conditionally provides when Kafka is configured:
 * - [ServistaKafkaProducer] for producing events
 *
 * Conditionally provides when Kafka consumer is configured:
 * - [KafkaConsumerProperties] for services to create their own ServistaKafkaConsumer instances
 */
fun servistaModule(config: ServistaConfig) = module {
    // Always provided
    single { BuildInfo.fromClasspath() }
    single { HealthRegistry(get()) }

    // Database (conditional)
    if (config.database != null) {
        single { migrateDatabase(config.database) }
        single { DSL.using(get<com.zaxxer.hikari.HikariDataSource>(), SQLDialect.POSTGRES) }
        single { RlsSessionManager() }
    }

    // Kafka producer (conditional)
    if (config.kafka != null) {
        single { ServistaKafkaProducer(config.kafka) }
    }

    // Kafka consumer properties (conditional)
    if (config.kafka?.consumer != null) {
        single { KafkaConsumerProperties.from(config.kafka) }
    }
}

/**
 * Registers a database readiness health check that executes `SELECT 1` against the database.
 *
 * Called after Koin initialization when database is configured.
 */
fun registerDatabaseHealthCheck(healthRegistry: HealthRegistry, dsl: org.jooq.DSLContext) {
    healthRegistry.registerReadiness("database") {
        try {
            dsl.selectOne().execute()
            HealthCheckResult(
                name = "database",
                status = HealthStatus.UP,
                details = mapOf("type" to "postgresql"),
            )
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.warn(e) { "Database health check failed" }
            HealthCheckResult(name = "database", status = HealthStatus.DOWN, error = e.message)
        }
    }
}
