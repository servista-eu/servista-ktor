package eu.servista.ktor.kafka

import eu.servista.ktor.KafkaConfig

/**
 * Pre-configured Kafka consumer properties from HOCON configuration.
 *
 * Services use this to construct their own [eu.servista.commons.kafka.ServistaKafkaConsumer]
 * instances with service-specific [eu.servista.commons.event.withEventContext] handlers and
 * [eu.servista.commons.kafka.DeadLetterRouter] implementations. The scaffold pre-configures the
 * common properties (bootstrap servers, group ID, auto offset reset, schema registry URL) so
 * services don't have to repeat the boilerplate.
 *
 * Usage in a service:
 * ```kotlin
 * val props = get<KafkaConsumerProperties>()
 * val consumerConfig = ServistaConsumerConfig(
 *     bootstrapServers = props.bootstrapServers,
 *     groupId = props.groupId,
 *     topics = listOf("my-topic"),
 *     consumerProperties = props.additionalProperties,
 * )
 * val consumer = ServistaKafkaConsumer(consumerConfig, myDeadLetterRouter, myHandler)
 * ```
 */
data class KafkaConsumerProperties(
    val bootstrapServers: String,
    val groupId: String,
    val autoOffsetReset: String,
    val schemaRegistryUrl: String,
) {
    /**
     * Additional Kafka consumer properties derived from this configuration, ready to merge into
     * [eu.servista.commons.kafka.ServistaConsumerConfig.consumerProperties].
     */
    val additionalProperties: Map<String, Any>
        get() = mapOf(
            "schema.registry.url" to schemaRegistryUrl,
            "auto.offset.reset" to autoOffsetReset,
        )

    companion object {
        /**
         * Creates [KafkaConsumerProperties] from HOCON [KafkaConfig].
         *
         * Requires [KafkaConfig.consumer] to be non-null (caller should check before invoking).
         */
        fun from(kafkaConfig: KafkaConfig): KafkaConsumerProperties {
            val consumer = requireNotNull(kafkaConfig.consumer) {
                "KafkaConfig.consumer must be non-null to create KafkaConsumerProperties"
            }
            return KafkaConsumerProperties(
                bootstrapServers = kafkaConfig.brokers,
                groupId = consumer.groupId,
                autoOffsetReset = consumer.autoOffsetReset,
                schemaRegistryUrl = kafkaConfig.schemaRegistryUrl,
            )
        }
    }
}
