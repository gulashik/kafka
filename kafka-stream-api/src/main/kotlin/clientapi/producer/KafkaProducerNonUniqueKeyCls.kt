package org.gulash.kfk.clientapi.producer

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.Future
import kotlin.random.Random

// Запускаем вначале container-cmd.md
fun main() {
    KafkaProducerNonUniqueKeyCls().start()
}

class KafkaProducerNonUniqueKeyCls(
    val topicName: String = "intopic",
) {
    fun start(count: Int = 200_000) {
        val logs = LoggerFactory.getLogger("appl")

        // ProducerConfig - опции для продюсера
        val producerConfig: Map<String, Any> = ProducerNonUniqueUtils.getProducerConfig()

        // KafkaProducer<Key, Value>
        val producer = KafkaProducer<String, String>(producerConfig)

        producer.use {
            for (i in 1..count) {
                Thread.sleep(1000)
                // Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback)
                // SEND - отправка, используем Future если нужно
                val recordMetadataFuture: Future<RecordMetadata> =
                    it.send(
                        // ProducerRecord - сообщение
                        ProducerRecord(
                            topicName, // topic
                            //0, // patition опционально
                            "key-${Random.nextInt(1,4)}", // key опционально, но важно
                            "value " + LocalDateTime.now(), // value
                            //listOf<Header>(RecordHeader("key1", "some value $i".encodeToByteArray())) // заголовки опционально
                        )
                    )
                    // Callback по окончанию отправки
                    { recordMetadata: RecordMetadata?, exception: Exception? ->
                        logs.info("Sent $i - ${Instant.now()}; Message timestamp - [${Instant.ofEpochMilli(recordMetadata?.timestamp() ?: 0L)}]")
                    }
            }

            // Явно отправляем если нужно
            producer.flush()
        }

        // т.к. используем USE - можно не закрывать. Закрываем Producer-а
        producer.close()

        System.out.println("Message send")
    }
}

// Для удобства конфигурирования
object ProducerNonUniqueUtils {
    const val HOST: String = "localhost:9092"

    // Основной блок настроек
    // Очень много параметров - см ProducerConfig
    private val producerConfig = mapOf<String, Any>(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to HOST,
        ProducerConfig.ACKS_CONFIG to "all",
        // Нужно указать серилизатор, десерилизатор
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
    )

    // Если нужно, то можно что-то переопределить
    fun getProducerConfig(addConfigs: Map<String, Any>? = null): Map<String, Any> {
        val resultConfig = HashMap<String, Any>(producerConfig)

        addConfigs?.let {
            resultConfig += addConfigs
        }

        return resultConfig
    }
}
