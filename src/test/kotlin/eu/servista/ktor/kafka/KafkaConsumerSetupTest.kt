package eu.servista.ktor.kafka

import eu.servista.ktor.HealthConfig
import eu.servista.ktor.KafkaConfig
import eu.servista.ktor.KafkaConsumerConfig
import eu.servista.ktor.LoggingConfig
import eu.servista.ktor.RequestLoggingConfig
import eu.servista.ktor.ServistaConfig
import eu.servista.ktor.di.servistaModule
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication

class KafkaConsumerSetupTest {

    @Test
    fun `KafkaConsumerProperties is correctly populated from KafkaConfig`() {
        val kafkaConfig =
            KafkaConfig(
                brokers = "localhost:9092",
                schemaRegistryUrl = "http://localhost:8081",
                consumer = KafkaConsumerConfig(groupId = "test-group", autoOffsetReset = "earliest"),
            )

        val props = KafkaConsumerProperties.from(kafkaConfig)

        props.bootstrapServers shouldBe "localhost:9092"
        props.groupId shouldBe "test-group"
        props.autoOffsetReset shouldBe "earliest"
        props.schemaRegistryUrl shouldBe "http://localhost:8081"
    }

    @Test
    fun `KafkaConsumerProperties additionalProperties contains schema registry and auto offset`() {
        val kafkaConfig =
            KafkaConfig(
                brokers = "broker1:9092,broker2:9092",
                schemaRegistryUrl = "http://schema-registry:8081",
                consumer = KafkaConsumerConfig(groupId = "my-group", autoOffsetReset = "latest"),
            )

        val props = KafkaConsumerProperties.from(kafkaConfig)

        props.additionalProperties["schema.registry.url"] shouldBe "http://schema-registry:8081"
        props.additionalProperties["auto.offset.reset"] shouldBe "latest"
    }

    @Test
    fun `Koin module provides KafkaConsumerProperties when kafka consumer is configured`() {
        val config =
            ServistaConfig(
                database = null,
                kafka =
                    KafkaConfig(
                        brokers = "localhost:9092",
                        schemaRegistryUrl = "http://localhost:8081",
                        consumer = KafkaConsumerConfig(groupId = "test-group"),
                    ),
                logging = LoggingConfig(requests = RequestLoggingConfig()),
                health = HealthConfig(),
            )

        // Use isolated koinApplication (not global startKoin) to avoid conflicts with other tests
        val app = koinApplication { modules(servistaModule(config)) }

        val props = app.koin.getOrNull<KafkaConsumerProperties>()
        props shouldNotBe null
        props!!.groupId shouldBe "test-group"
        props.bootstrapServers shouldBe "localhost:9092"

        app.close()
    }

    @Test
    fun `Koin module does NOT provide KafkaConsumerProperties when kafka consumer is absent`() {
        val config =
            ServistaConfig(
                database = null,
                kafka =
                    KafkaConfig(
                        brokers = "localhost:9092",
                        schemaRegistryUrl = "http://localhost:8081",
                        consumer = null,
                    ),
                logging = LoggingConfig(requests = RequestLoggingConfig()),
                health = HealthConfig(),
            )

        val app = koinApplication { modules(servistaModule(config)) }

        val props = app.koin.getOrNull<KafkaConsumerProperties>()
        props shouldBe null

        app.close()
    }

    @Test
    fun `Koin module does NOT provide KafkaConsumerProperties when kafka is absent`() {
        val config =
            ServistaConfig(
                database = null,
                kafka = null,
                logging = LoggingConfig(requests = RequestLoggingConfig()),
                health = HealthConfig(),
            )

        val app = koinApplication { modules(servistaModule(config)) }

        val props = app.koin.getOrNull<KafkaConsumerProperties>()
        props shouldBe null

        app.close()
    }
}
