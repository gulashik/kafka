package ktable

import mu.KotlinLogging
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.gulash.kfk.clientapi.producer.KafkaProducerNonUniqueKeyCls
import org.slf4j.Logger
import kotlin.concurrent.thread

fun main() {
    /*
    Концептульно создание и использование KafkaStreams состоит
    Построение DAG-а(топологии действий)
        Создание Билдера
        Обогощаем билдер. Source node - метод stream
        Обогощаем билдер. Stream nodes - методы mapValues, selectKey, proceesValues ...
        Обогощаем билдер. Sink nodes - to, foreach, print ...

    Экземпляр KafkaStreams
        обогащаем DAG-ом
        запускаем поток
        закрываем поток
*/
    val log: Logger = KotlinLogging.logger{}

    // Построение DAG-а(топологии действий) ----------------------------------------------
    val streamsBuilder: StreamsBuilder = StreamsBuilder()

    // Создание сущности KTable через StreamsBuilder.table
    // Input records with null key will be dropped.
    val kTable: KTable<String, String> =streamsBuilder.table(
        "intopic-ktable", // входящий топик
        Consumed.with(Serdes.StringSerde(), Serdes.StringSerde()), // сериализатор/десериализатор
        // материализуется в локальное персистентное хранилище
        //Materialized.`as`<String, String, KeyValueStore<Bytes, ByteArray>>("ktable-store").withLoggingDisabled() /*withLoggingDisabled нет backup-а в топик*/
        Materialized.`as`("ktable-store")
    )

    val kStream: KStream<String, String> = streamsBuilder.stream(
        "intopic-kstream",
        Consumed.with(Serdes.StringSerde(), Serdes.StringSerde())
    )

    // Выводим KTable - ВЫВОД БУДЕТ НЕ СРАЗУ через какое-то время после завершения посылки сообщений
    kTable.toStream()
        .foreach { key, value -> println("KTable $key = $value") }
    kStream
        .foreach { key, value -> println("KStream $key = $value") }

    // Создание и Запуск KafkaStreams --------------------------------------------------------------
    // Чтобы был запуск нужно СОЗДАТЬ STREAM-ы и передать туда DAG(топологии действий)
    // Опции для Stream в StreamsConfig - нужные конфиг опции для запуска
    val streamsConfigProps: Map<String, Any> = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to "myAppIdKTable",
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
        // Опции управляющие частотой отправки состояния
        // StreamsConfig.COMMIT_INTERVAL_MS_CONFIG to 10000, // The frequency in milliseconds with which to commit processing progress.
        //StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG to 50, // "Maximum number of memory bytes to be used for buffering across all threads"
    )
    val streamsConfig: StreamsConfig = StreamsConfig(streamsConfigProps)
    // KafkaStreams - запуск экземпляра.
    // ВАЖНО!!! Stream работает ПОКА ВЫПОЛНЯЕТСЯ USE блок
    KafkaStreams(
        streamsBuilder.build(), // Builder - это DAG, который мы на создавали
        streamsConfig //
    ).use { // Используем USE т.к. нужно явно закрыть stream
        log.info("Application started")

        // Для очистки состояния нужно выполнить 2 пукта ОДНОВРЕМЕННО
        // 1 вызвать метод cleanUp() или удалить из папки в ручную rm -rf <state.dir>/<application.id> (e.g., rm -rf /var/lib/kafka-streams/my-streams-app)
        // 2 Удалить топик с где в названии есть application.id + имя_персистентного_хранилища + changelog например "myAppIdKStream-persistent-store-changelog"
        // почитать тут https://www.confluent.io/blog/data-reprocessing-with-kafka-streams-resetting-a-streams-application/
        //it.cleanUp()

        // KafkaStream фактически начинает после вызова метода start()
        it.start()

        // ВАЖНО!!! Stream работает ПОКА ВЫПОЛНЯЕТСЯ USE блок
        thread {
            KafkaProducerNonUniqueKeyCls("intopic-ktable").start(10)
        }

        thread {
            KafkaProducerNonUniqueKeyCls("intopic-kstream").start(10)
        }

        Thread.sleep(50000)

        log.info("Application stopped")
        // Можно не закрывать т.к. мы в use{}
        //it.close()
    }
}
