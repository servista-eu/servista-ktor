package eu.servista.ktor

import io.ktor.server.config.ApplicationConfig
import org.slf4j.event.Level

/**
 * Typed configuration data classes mirroring the `servista {}` HOCON block.
 *
 * Services define their `application.conf` with a `servista {}` section. This class loads and
 * validates the configuration using Ktor's [ApplicationConfig] API with environment variable
 * substitution support.
 *
 * Database and Kafka sections are nullable -- not all services need both.
 */
data class ServistaConfig(
    val database: DatabaseConfig?,
    val kafka: KafkaConfig?,
    val logging: LoggingConfig,
    val health: HealthConfig,
) {
    companion object {
        fun load(config: ApplicationConfig): ServistaConfig {
            val servista = config.config("servista")
            return ServistaConfig(
                database = servista.configOrNull("database")?.let { DatabaseConfig.from(it) },
                kafka = servista.configOrNull("kafka")?.let { KafkaConfig.from(it) },
                logging = LoggingConfig.from(servista.configOrNull("logging")),
                health = HealthConfig.from(servista.configOrNull("health")),
            )
        }
    }
}

/** Database connection configuration. */
data class DatabaseConfig(val url: String, val user: String, val password: String) {
    companion object {
        fun from(config: ApplicationConfig) =
            DatabaseConfig(
                url = config.property("url").getString(),
                user = config.property("user").getString(),
                password = config.property("password").getString(),
            )
    }
}

/**
 * Kafka broker and schema registry configuration.
 *
 * The [consumer] sub-config is nullable -- only services that consume events need it.
 */
data class KafkaConfig(
    val brokers: String,
    val schemaRegistryUrl: String,
    val consumer: KafkaConsumerConfig?,
) {
    companion object {
        fun from(config: ApplicationConfig) =
            KafkaConfig(
                brokers = config.property("brokers").getString(),
                schemaRegistryUrl = config.property("schema-registry-url").getString(),
                consumer = config.configOrNull("consumer")?.let { KafkaConsumerConfig.from(it) },
            )
    }
}

/** Kafka consumer configuration for services that consume events. */
data class KafkaConsumerConfig(val groupId: String, val autoOffsetReset: String = "earliest") {
    companion object {
        fun from(config: ApplicationConfig) =
            KafkaConsumerConfig(
                groupId = config.property("group-id").getString(),
                autoOffsetReset =
                    config.propertyOrNull("auto-offset-reset")?.getString() ?: "earliest",
            )
    }
}

/** Logging configuration including request logging sub-config. */
data class LoggingConfig(val requests: RequestLoggingConfig) {
    companion object {
        fun from(config: ApplicationConfig?) =
            LoggingConfig(requests = RequestLoggingConfig.from(config?.configOrNull("requests")))
    }
}

/** Request logging configuration for the Ktor CallLogging plugin. */
data class RequestLoggingConfig(
    val enabled: Boolean = true,
    val logLevel: Level = Level.INFO,
    val excludePaths: List<String> = listOf("/health"),
) {
    companion object {
        fun from(config: ApplicationConfig?) =
            if (config == null) {
                RequestLoggingConfig()
            } else {
                RequestLoggingConfig(
                    enabled = config.propertyOrNull("enabled")?.getString()?.toBoolean() ?: true,
                    logLevel =
                        config.propertyOrNull("level")?.getString()?.let {
                            Level.valueOf(it.uppercase())
                        } ?: Level.INFO,
                    excludePaths =
                        config.propertyOrNull("exclude-paths")?.getList() ?: listOf("/health"),
                )
            }
    }
}

/** Health endpoint configuration. */
data class HealthConfig(val enabled: Boolean = true) {
    companion object {
        fun from(config: ApplicationConfig?) =
            if (config == null) {
                HealthConfig()
            } else {
                HealthConfig(
                    enabled = config.propertyOrNull("enabled")?.getString()?.toBoolean() ?: true
                )
            }
    }
}

/**
 * Extension to get a sub-config or null if the section doesn't exist. Ktor's ApplicationConfig
 * throws on missing sections, so we catch and return null.
 */
private fun ApplicationConfig.configOrNull(path: String): ApplicationConfig? =
    try {
        config(path)
    } catch (_: Exception) {
        null
    }
