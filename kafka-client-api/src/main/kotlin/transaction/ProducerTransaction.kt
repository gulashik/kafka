package org.gulash.transaction

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.Future

// Запускаем container-cmd.md
fun main() {
    val logs = LoggerFactory.getLogger("appl")

    // ProducerConfig - есть опции для включения транзакций у продюсера
    val producerConfig: Map<String, Any> = ProducerTransactionUtils.getProducerConfig()

    // KafkaProducer<Key, Value>
    val producer = KafkaProducer<String, String>(producerConfig)

    // Регистрация транзакционного продюсера на брокере
    producer.initTransactions()

    producer.use {
        for (i in 0..200_000) {
            // Начинаем транзакцию
            producer.beginTransaction()

            Thread.sleep(300)
            // Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback)
            // SEND - отправка, используем Future если нужно
            val recordMetadataFuture: Future<RecordMetadata> =
                it.send(
                    // ProducerRecord - сообщение
                    ProducerRecord(
                        "mytopic", // topic
                        //0, // patition опционально
                        "$i", // key опционально, но важно
                        "value " + LocalDateTime.now(), // value
                        //listOf<Header>(RecordHeader("key1", "some value $i".encodeToByteArray())) // заголовки опционально
                    )
                )
                // Callback по окончанию отправки
                { recordMetadata: RecordMetadata?, exception: Exception? ->
                    logs.info("Sent $i - ${Instant.now()}; Message timestamp - [${Instant.ofEpochMilli(recordMetadata?.timestamp() ?: 0L)}]")
                }

            // Завершение транзакции
            // Commit
            producer.commitTransaction()
            // Rollback - сообщений не получит
            //producer.abortTransaction()

        }

        // Явно отпарвляем если нужно
        producer.flush()
    }

    // т.к. используем USE - можно не закрывать. Закрываем Producer-а
    producer.close()

    System.out.printf("Message send")
}


// Для удобства конфигурирования
object ProducerTransactionUtils {
    const val HOST: String = "localhost:9092"

    // Основной блок настроек
    // Очень много параметров - см ProducerConfig
    private val producerConfig = mapOf<String, Any>(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to HOST,

        // Обязательные параметры для Транзакции
        // Нужно указать TRANSACTIONAL.ID
        ProducerConfig.TRANSACTIONAL_ID_CONFIG to "my_transaction_id",

        //  Имеет смысл явно ENABLE.IDEMPOTENCE = TRUE указать так как при изменении параметров отличных от указанных
        // БУДЕТ ВТИХАРЯ ВЫКЛЮЧЕН, А ТАК БУДЕТ ОШИБКА
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to "true", // default=true
        // Опции к ENABLE.IDEMPOTENCE
        // Опция acks default = all нужно all
        ProducerConfig.ACKS_CONFIG to "all",
        // Опция retries default = 2147483647 нужно > 0
        ProducerConfig.RETRIES_CONFIG to 2147483647,
        // Опция max.in.flight.request.per.connection default = 5 нужно <= 5
        ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 5,

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