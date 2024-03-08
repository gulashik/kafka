package org.gulash.kfk

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.gulash.kfk.model.User

fun main() {
    KafkaProducer<String, User>(
        mapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9091",
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java, // Для AVRO
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "http://localhost:8081" // URL Schema registry
            // KafkaAvroSerializerConfig смотрим тут много опций
        )
    ).use { producer ->
        producer.send(
            ProducerRecord<String, User>(
                "topic1",
                "1",
                User.newBuilder()
                    .setName("Ivan")
                    .setEmail("a@b.ru")
                    .setFavoriteColor("blue")
                    .setFavoriteNumber(42)
                    .build()
            )
        )
    }
}