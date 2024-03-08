package org.gulash.config.props

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Future

fun main() {
    val logs = LoggerFactory.getLogger("appl")

    val props = Properties()
    props.setProperty("bootstrap.servers", "localhost:9092")
    props["key.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"
    props["value.serializer"] = "org.apache.kafka.common.serialization.StringSerializer"

    // KafkaProducer<Key, Value>
    val producer = KafkaProducer<String, String>(props)

    producer.use {
        for (i in 0..20) {

            // Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback)
            // SEND - отправка, используем Future если нужно
            val recordMetadataFuture: Future<RecordMetadata> =
                producer.send(
                    // ProducerRecord - сообщение
                    ProducerRecord(
                        "mytopic",
                        0,
                        "key$i",
                        "value " + LocalDateTime.now(),
                        listOf<Header>(RecordHeader("key1", "some value $i".encodeToByteArray()))
                    )
                )
                // Callback по окончанию отправки
                { recordMetadata: RecordMetadata?, exception: Exception? ->
                    logs.info("Sent - ${Instant.now()}; Message timestamp - [${Instant.ofEpochMilli(recordMetadata?.timestamp() ?: 0L)}]")
                }
        }
    }

    Thread.sleep(1000)
    // Явно отправляем если нужно
    producer.flush()

    // Закрываем Producer-а
    producer.close()

    System.out.printf("Message send")
}

