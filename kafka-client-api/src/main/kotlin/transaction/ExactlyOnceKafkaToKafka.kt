package  org.gulash.transaction

import org.apache.kafka.clients.consumer.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.IsolationLevel
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Future

fun main() {
    /*
     * Для работоспосбности запускаем ProducerTransaction.kt, а потом только этот main
     */
    // Особенность в том что
    //      Вначале читаем из Входящего топика - Consumer
    //      Потом записываем в Исходящий топик - Produce
    //      ВАЖНО!!! Коммит оффсета делает PRODUCER, не Consumer как обычно
    //          Идея в том что за одну транзакцию отправляем в исходящий топик и коммитим оффсет по входящему топику

    //=====================Consumer=====================
    // Consumer входной топик
    val inTopics = listOf<String>("mytopic")

    // Consumer группа
    val groupId = "exactly-once-group"

    // Consumer конфиги
    val consumerConfig: Map<String, Any> = ConsumerExactlyOnceUtils.getConsumerConfig(
        mapOf(ConsumerConfig.GROUP_ID_CONFIG to groupId)
    )

    //Создаём Consumer - KafkaConsumer<Key, Value>
    val consumer: KafkaConsumer<String, String> = KafkaConsumer<String, String>(consumerConfig)

    // Подписываемся на topic - void subscribe(Collection<String> topics)
    ConsumerExactlyOnceUtils.log.warn("Subscribe to ${inTopics} with ${groupId}")
    consumer.subscribe(inTopics)

    //=====================Producer=====================
    val outTopic = "othertopic"

    // ProducerConfig - есть опции для включения транзакций у продюсера
    val producerConfig: Map<String, Any> = ProducerExactlyOnceUtils.getProducerConfig()

    // KafkaProducer<Key, Value>
    val producer = KafkaProducer<String, String>(producerConfig)

    // Регистрация транзакционного продюсера на брокере
    producer.initTransactions()

    //=====================Process=====================
    // Для закрытия консюмера при ошибке
    try {
        while (true) {

            val result: ConsumerRecords<String, String> = consumer.poll(Duration.ofSeconds(10))

            ConsumerExactlyOnceUtils.log.info("Read ${result.count()}")

            result.forEach { consumerRecord ->

                // Начинаем транзакцию
                producer.beginTransaction()

                // Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback)
                // SEND - отправка, используем Future если нужно
                val recordMetadataFuture: Future<RecordMetadata> =
                    producer.send(
                        ProducerRecord(
                            outTopic, // topic
                            consumerRecord.partition(), // patition опционально
                            consumerRecord.key(), // key опционально, но важно
                            consumerRecord.value() + "_processed", // value
                            consumerRecord.headers() + listOf<Header>(
                                RecordHeader("some-header", "some value".encodeToByteArray())
                            ) // заголовки опционально
                        )
                    )
                    // Callback по окончанию отправки
                    { recordMetadata: RecordMetadata?, exception: Exception? ->
                        ProducerExactlyOnceUtils.log.info("Sent ${consumerRecord.key()} - ${Instant.now()}; Message timestamp - [${Instant.ofEpochMilli(recordMetadata?.timestamp() ?: 0L)}]")
                    }

                // ВАЖНО!!! Коммит оффсета делает PRODUCER, не Consumer как обычно
                // Идея в том что за одну транзакцию отправляем в исходящий топик и коммитим оффсет по входящему топику
                producer.sendOffsetsToTransaction(
                    mapOf(
                        TopicPartition(consumerRecord.topic(), consumerRecord.partition())
                                to OffsetAndMetadata(consumerRecord.offset())
                    ),
                    ConsumerGroupMetadata(groupId)
                )

                // Завершение транзакции
                producer.commitTransaction() //producer.abortTransaction()
            }

        }
    } finally {
        // Если нужно закрыть/остановить консюмера.
        // Если enable.auto.commit=false, то нужно вызвать коммит
        consumer.close()
        producer.close()
    }
}


// Для удобства конфигурирования
object ProducerExactlyOnceUtils {
    val log: Logger = LoggerFactory.getLogger("appl")

    const val HOST: String = "localhost:9092"

    // Основной блок настроек
    // Очень много параметров - см ProducerConfig
    private val producerConfig = mapOf<String, Any>(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to HOST,

        // Обязательные параметры для Транзакции
        // Нужно указать TRANSACTIONAL.ID
        ProducerConfig.TRANSACTIONAL_ID_CONFIG to "my_transaction_id_exactly_once",

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
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
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

// Для удобства конфигурирования
object ConsumerExactlyOnceUtils {
    val log: Logger = LoggerFactory.getLogger("appl")

    const val HOST: String = "localhost:9092"

    private val consumerConfig = mutableMapOf<String, Any>(
        //Очень много параметров в ConsumerConfig
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to HOST,

        ConsumerConfig.GROUP_ID_CONFIG to "some-default-consumer-group",

        // Опция isolation.level - указание учитывать транзакции
        // нужно "read_committed" default = "read_uncommitted"
        ConsumerConfig.ISOLATION_LEVEL_CONFIG to IsolationLevel.READ_COMMITTED.toString().lowercase(),

        // Нужно указать серилизатор, десерилизатор
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,

        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",

        //ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
    )

    // Если нужно, то можно что-то переопределить
    fun getConsumerConfig(addConfigs: Map<String, Any>? = null): Map<String, Any> {
        val resultConfig = HashMap<String, Any>(consumerConfig)

        addConfigs?.let {
            resultConfig += addConfigs
        }

        return resultConfig
    }
}