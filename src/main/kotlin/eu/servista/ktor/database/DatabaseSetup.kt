package eu.servista.ktor.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.servista.ktor.DatabaseConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway

private val logger = KotlinLogging.logger {}

private const val DEFAULT_POOL_SIZE = 10

/**
 * Creates a [HikariDataSource] connection pool and runs Flyway database migrations.
 *
 * Flyway is configured with `cleanDisabled = true` to prevent accidental data loss in production.
 * Migrations are loaded from `classpath:db/migration`.
 *
 * @param config Database connection configuration (JDBC URL, user, password)
 * @return The configured [HikariDataSource] for jOOQ DSLContext creation
 */
fun migrateDatabase(config: DatabaseConfig): HikariDataSource {
    val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = DEFAULT_POOL_SIZE
        }

    val dataSource = HikariDataSource(hikariConfig)

    val result =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .cleanDisabled(true)
            .load()
            .migrate()

    logger.info { "Flyway migration complete: ${result.migrationsExecuted} migrations executed" }

    return dataSource
}
