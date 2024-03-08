package ktable

import mu.KotlinLogging
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.state.KeyValueStore
import org.gulash.kfk.clientapi.producer.KafkaProducerNonUniqueKeyCls
import org.slf4j.Logger
import kotlin.concurrent.thread
import kotlin.random.Random

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
    val log: Logger = KotlinLogging.logger {}

    // Построение DAG-а(топологии действий) ----------------------------------------------
    val streamsBuilder: StreamsBuilder = StreamsBuilder()

    // Создание сущности KTable через StreamsBuilder.table
    // Input records with null key will be dropped.
    val kStream: KStream<String, String> = streamsBuilder
        .stream(
            "intopic-ktable",
            Consumed.with(Serdes.StringSerde(), Serdes.StringSerde())
        )

    // reduce---------------------------------------------------------------------------
    kStream
        .selectKey(KeyValueMapper { key, value -> listOf("a","b","c").shuffled()[0] })
        // тут для удобства заменили value на 1
        .mapValues { value: String -> 1 }
        .peek(ForeachAction { key, value -> println("reduce key-$key value-$value") })
        // Действие над ключом если нужно
        .groupBy({ key, _ -> key[0].toString() }, Grouped.with(Serdes.StringSerde(), Serdes.IntegerSerde()))
        // Группировка по ключу если доп действий не нужно
        //.groupByKey()
        .reduce(
            Reducer { acc, curValue -> log.info("get key-value $acc, $curValue"); acc + curValue },
            // материализуется в локальное персистентное хранилище
            //Materialized.`as`<String, String, KeyValueStore<Bytes, ByteArray>>("ktable-store").withLoggingDisabled()
            Materialized.`as`<String, Int, KeyValueStore<Bytes, ByteArray>?>("reduce-store")
                .withLoggingDisabled()  /*withLoggingDisabled нет backup-а в топик*/
            //Materialized.`as`("reduce-store")
        )
        .toStream()
        //.to("reduce-topic")
        .foreach { key, value -> println("KTable Reduce $key = $value") }

    // count---------------------------------------------------------------------------
    kStream
        .selectKey(KeyValueMapper { key, value -> listOf("a","b","c").shuffled()[0] })
        // тут для удобства заменили value на 1
        .mapValues { value: String -> 1 }
        .peek(ForeachAction { key, value -> println("count key-$key value-$value") })
        // Действие над ключом если нужно
        .groupBy({ key, _ -> key[0].toString() }, Grouped.with(Serdes.StringSerde(), Serdes.IntegerSerde()))
        // Группировка по ключу если доп действий не нужно
        //.count()
        .count(
            // материализуется в локальное персистентное хранилище
            //Materialized.`as`<String, String, KeyValueStore<Bytes, ByteArray>>("ktable-store").withLoggingDisabled()
            Materialized.`as`<String, Long, KeyValueStore<Bytes, ByteArray>?>("count-store")
                .withLoggingDisabled()  /*withLoggingDisabled нет backup-а в топик*/
            //Materialized.`as`("count-store")
        )
        .toStream()
        //.to("count-topic")
        .foreach { key, value -> println("KTable Count $key = $value") }

    // aggregate---------------------------------------------------------------------------
    kStream
        .selectKey(KeyValueMapper { key, value -> listOf("a","b","c").shuffled()[0] })
        // тут для удобства заменили value на 1
        .mapValues { value: String -> 1 }
        .peek(ForeachAction { key, value -> println("aggregate key-$key value-$value") })
        // Действие над ключом если нужно
        .groupBy({ key, _ -> key[0].toString() }, Grouped.with(Serdes.StringSerde(), Serdes.IntegerSerde()))
        //
        .aggregate(
            // начальное значение
            { 0 },
            // агрегация
            { key, value, aggregate -> aggregate + value },

            // материализуется в локальное персистентное хранилище
            //Materialized.`as`<String, String, KeyValueStore<Bytes, ByteArray>>("ktable-store").withLoggingDisabled()
            Materialized.`as`<String, Long, KeyValueStore<Bytes, ByteArray>?>("aggregate-store")
                .withLoggingDisabled()  //withLoggingDisabled нет backup-а в топик

                // Нужно указать сериализаторы/десериализаторы
                .withKeySerde(Serdes.StringSerde())
                .withValueSerde(Serdes.LongSerde())
            //Materialized.`as`("aggregate-store")
        )
        .toStream()
        //.to("aggregate-topic")
        .foreach { key, value -> println("KTable Aggregate $key = $value") }

    // Создание и Запуск KafkaStreams --------------------------------------------------------------
    // Чтобы был запуск нужно СОЗДАТЬ STREAM-ы и передать туда DAG(топологии действий)
    // Опции для Stream в StreamsConfig - нужные конфиг опции для запуска
    val streamsConfigProps: Map<String, Any> = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to "myAppIdKTable",
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
        // Опции управляющие частотой отправки состояния
        // StreamsConfig.COMMIT_INTERVAL_MS_CONFIG to 1000, // The frequency in milliseconds with which to commit processing progress.
        // StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG to 50, // "Maximum number of memory bytes to be used for buffering across all threads"
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
        it.cleanUp()

        // KafkaStream фактически начинает после вызова метода start()
        it.start()

        // ВАЖНО!!! Stream работает ПОКА ВЫПОЛНЯЕТСЯ USE блок
        thread {
            KafkaProducerNonUniqueKeyCls("intopic-ktable").start(10)
        }

        Thread.sleep(50000)

        log.info("Application stopped")
        // Можно не закрывать т.к. мы в use{}
        //it.close()
    }
}
