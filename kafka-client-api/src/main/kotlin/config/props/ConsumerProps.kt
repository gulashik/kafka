package org.gulash.config.props

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

// Запускаем container-cmd.md
fun main() {
    val props = Properties()
    props["bootstrap.servers"] = "localhost:9092"
    props["group.id"] = "mygroup"
    props["enable.auto.commit"] = "false"

    //props.put("auto.offset.reset","earliest");
    props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
    props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"

    val consumer = KafkaConsumer<String, String>(props)
    consumer.subscribe(listOf("mytopic"))


    // Ожидание примерно 10-30 секунд
    var records: ConsumerRecords<String?, String?>
    do {
        records = consumer.poll(Duration.ofMillis(100))

        for (record in records) {
            System.out.printf(
                "offset=%d, key=%s, value=%s, headers=%s%n",
                record.offset(),
                record.key(),
                record.value(),
                record.headers()
            )
        }
        // 1 Коммит на целый Batch
        // если используем props.put("enable.auto.commit","false");
        consumer.commitAsync() // без ожидания подтверждения

        //consumer.commitSync(); ждём подтверждения
        if (!records.isEmpty) println("One batch and return")
    } while (records.isEmpty)
}