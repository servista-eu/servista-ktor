package eu.servista.ktor.kafka

import eu.servista.ktor.KafkaConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.Properties
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val logger = KotlinLogging.logger {}

/**
 * Thin wrapper around [KafkaProducer] with HOCON-driven configuration and coroutine-friendly send.
 *
 * EventBuilder from servista-commons handles envelope construction and Avro encoding upstream.
 * This wrapper only concerns itself with producing raw byte arrays to Kafka topics.
 *
 * @param config Kafka configuration from HOCON (brokers, schema registry URL)
 */
class ServistaKafkaProducer(config: KafkaConfig) {

    private val producer: KafkaProducer<String, ByteArray>

    init {
        val props = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.brokers)
            put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer::class.java.name,
            )
            put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                ByteArraySerializer::class.java.name,
            )
            put(ProducerConfig.ACKS_CONFIG, "all")
            put("schema.registry.url", config.schemaRegistryUrl)
        }
        producer = KafkaProducer(props)
        logger.info { "ServistaKafkaProducer initialized: brokers=${config.brokers}" }
    }

    /**
     * Sends a record to the specified topic. Suspends the coroutine until the broker acknowledges.
     *
     * Uses [suspendCancellableCoroutine] with the Kafka callback API to avoid blocking a coroutine
     * thread on the Java Future.
     *
     * @param topic Kafka topic name
     * @param key Record key (used for partitioning)
     * @param value Record value (Avro-encoded bytes from EventBuilder)
     * @return Kafka [RecordMetadata] with offset and partition info
     */
    suspend fun send(topic: String, key: String, value: ByteArray): RecordMetadata {
        val record = ProducerRecord(topic, key, value)
        return suspendCancellableCoroutine { cont ->
            producer.send(record) { metadata, exception ->
                if (exception != null) {
                    cont.resumeWithException(exception)
                } else {
                    cont.resume(metadata)
                }
            }
        }
    }

    /** Closes the underlying Kafka producer, flushing any pending records. */
    fun close() {
        producer.close()
        logger.info { "ServistaKafkaProducer closed" }
    }
}
