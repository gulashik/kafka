package org.gulash.kfk

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.gulash.kfk.model.User
import java.time.Duration
import java.util.function.Consumer

fun main() {
    KafkaConsumer<String, User>(
        mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9091",
            ConsumerConfig.GROUP_ID_CONFIG to "consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java, // Для AVRO
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://localhost:8081" // URL Schema registry
            // KafkaAvroSerializerConfig смотрим тут много опций
        )
    ).use { consumer ->
        consumer.subscribe( listOf("topic1") )
        // Прерываем как получим
        while (true) {
            val records: ConsumerRecords<String, User> =
                consumer.poll(Duration.ofSeconds(10))

            records.forEach(
                Consumer<ConsumerRecord<String, User>> { it: ConsumerRecord<String, User> ->
                    println(it.key() + ": " + it.value())
                }
            )
        }
    }
}