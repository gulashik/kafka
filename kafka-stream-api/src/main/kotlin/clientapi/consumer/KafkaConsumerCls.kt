package org.gulash.kfk.clientapi.consumer

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

// Запускаем container-cmd.md

fun main() {
    val groupId = "app-group"
    val topics = listOf<String>("intopic")

    val consumerConfig: Map<String, Any> = ConsumerUtils.getConsumerConfig(
        mapOf(
            //Очень много параметров в ConsumerConfig
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            //ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
        )
    )

    //Создаём консюмера - KafkaConsumer<Key, Value>
    val consumer: KafkaConsumer<String, String> = KafkaConsumer<String, String>(consumerConfig)

    ConsumerUtils.log.warn("Subscribe to ${topics} with ${groupId}")

    // Подписываемся на topic - void subscribe(Collection<String> topics)
    consumer.subscribe(topics)

    // Для закрытия консюмера при ошибке
    try {
        while (true) {

            //Опрашиваем с ожиданием - ConsumerRecords<K, V> poll(final Duration timeout)
            val result: ConsumerRecords<String, String> = consumer.poll(Duration.ofSeconds(10))

            // Ставим на паузу. Если длинная обработка.
            // Игнорирование опции max.poll.interval.ms(default:300000 (5 minutes)
            //      - интервал, за который один Consumer должен запросить новую порцию сообщений.
            // Не вызывает rebalance, но если rebalance случился состояние паузы не сохранится
            // Пока полезность не понятна
            //consumer.pause( consumer.assignment() )

            ConsumerUtils.log.info("Read ${result.count()}")

            result.forEach {
                with(it) {
                    ConsumerUtils.log.warn("offset=${offset()}, key=${key()}, value=${value()}, headers=${headers()}")
                }
            }
            // 1 Коммит на целый Batch или 1 Коммит на offset - сложнее нужно оперировать офсетами
            // если используем props.put("enable.auto.commit","false");
            //consumer.commitAsync() // без ожидания подтверждения
            //consumer.commitSync(); ждём подтверждения

            // SEEK - поиск по офсетам
            // Особо не разбирался написал наугад
            //consumer.seek(TopicPartition(/*topic*/"mytopic", /*partition*/0), /*offset*/1)
            //consumer.seekToBeginning(consumer.assignment())
            //consumer.seekToEnd(consumer.assignment())

            // Ставим на паузу. Если длинная обработка.
            // Игнорирование опции max.poll.interval.ms(default:300000 (5 minutes)
            //      - интервал, за который один Consumer должен запросить новую порцию сообщений.
            // Не вызывает rebalance, но если rebalance случился состояние паузы не сохранится
            // Пока полезность не понятна
            //consumer.resume( consumer.assignment() )
        }
    } finally {
        // Если нужно закрыть/остановить консюмера.
        // Если enable.auto.commit=false, то нужно вызвать коммит
        consumer.close()
    }
}

// Для удобства конфигурирования
object ConsumerUtils {
    val log: Logger = LoggerFactory.getLogger("appl")

    const val HOST: String = "localhost:9092"

    private val consumerConfig = mutableMapOf<String, Any>(
        //Очень много параметров в ConsumerConfig
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to HOST,
        ConsumerConfig.GROUP_ID_CONFIG to "some-default-consumer-group",
        // Нужно указать серилизатор, десерилизатор
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
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