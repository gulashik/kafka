package org.gulash.kfk.kstreams

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.*
import org.gulash.kfk.clientapi.producer.KafkaProducerCls
import org.gulash.kfk.clientapi.producer.KafkaProducerNonUniqueKeyCls
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Можно запускать сразу
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
    val log: Logger = LoggerFactory.getLogger("appl")

    // Объект типа Serde<T> - настройки Сериализации/Десериализации
    val stringSerde = Serdes.StringSerde()

    // Построение DAG-а(топологии действий) НАЧАЛОСЬ----------------------------------------------

    // StreamsBuilder - основа для построения DAG
    val streamsBuilder: StreamsBuilder = StreamsBuilder()
    // Consumed - Consumer-ы хранилка параметров
    val consumed: Consumed<String, String> =
        Consumed.with(/*keySerde*/ stringSerde, /*valueSerde*/ stringSerde)
            .withName("SourceProcessor") // Имя опционально

    // KStream - stream действий похож на обычный stream.
    val kStreamNoBranch: KStream<String, String> = streamsBuilder
        // ----
        // Метод stream - ВХОДНАЯ ТОЧКА
        .stream("intopic", consumed)
        // ----
        // Промежуточные методы пока БЕЗ ВЕТВЛЕНИЯ
        // Named - опционально. Можно и без него
        .mapValues({ value -> value.uppercase() }, Named.`as`("FirstProcessor"))
        // Тут обошлись без имени
        .filter { key, value -> value.isNotEmpty() }
        .peek { key, value -> log.info("Now is kStream = key-{}, value-{}", key, value) }

    // SINK Методы НЕ ОБЯЗАТЕЛЬНЫ!!!

    // Выводим Топологию
    log.info("Builder: {}", streamsBuilder.build().describe())
    // Построение DAG-а(топологии действий) ЗАКОНЧЕНО ----------------------------------------------

    // Создание и Запуск KafkaStreams --------------------------------------------------------------
    // Чтобы был запуск нужно СОЗДАТЬ STREAM-ы и передать туда DAG(топологии действий)
    // Опции для Stream в StreamsConfig - нужные конфиг опции для запуска
    val streamsConfigProps: Map<String, Any> = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to "myAppIdKStreamNoSink",
        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to "localhost:9092",
    )
    val streamsConfig: StreamsConfig = StreamsConfig(streamsConfigProps)

    // KafkaStreams - запуск экземпляра.
    // ВАЖНО!!! Stream работает ПОКА ВЫПОЛНЯЕТСЯ USE блок
    KafkaStreams(
        streamsBuilder.build(), // Builder - это DAG, который мы на создавали
        streamsConfig //
    ).use { // Используем USE т.к. нужно явно закрыть stream
        log.info("Application started")
        // KafkaStream фактически начинает после вызова метода start()
        it.start()

        // ВАЖНО!!! Stream работает ПОКА ВЫПОЛНЯЕТСЯ USE блок
        KafkaProducerCls().start()
        //Thread.sleep(500000)

        log.info("Application stopped")
        // Можно не закрывать т.к. мы в use{}
        //it.close()
    }
}