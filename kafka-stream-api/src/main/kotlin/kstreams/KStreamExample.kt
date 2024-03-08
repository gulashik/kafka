package org.gulash.kfk.kstreams

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.*
import mu.KotlinLogging

// Запускаем вначале org.gulash.kfk.clientapi.producer.KafkaProducerKt.main
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
    val log = KotlinLogging.logger {}

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
        .peek { key, value -> log.info("Now is = key-{}, value-{}", key, value) }
        // Named - опционально. Можно и без него
        .mapValues({ value -> value.uppercase()}, Named.`as`("FirstProcessor"))
        // Тут обошлись без имени
        .filter { key, value -> value.isNotEmpty() }
        .selectKey { key, value -> "New value + Old key($key)" } // Меняем ключ

    // Делаем Branch1-----------------------------------------------------------------
    // to - Терминальный метод
    // Produced - Producer-ы хранилка параметров
    val kStreamBranch: KStream<String, String> = kStreamNoBranch.mapValues { value -> value.repeat(2) }
    kStreamBranch.to("outopic-branch1", Produced.with(stringSerde, stringSerde).withName("SinkProcessor-Branch1"))

    // Делаем Branch2-----------------------------------------------------------------
    // to - Терминальный метод
    // Produced - Producer-ы хранилка параметров
    kStreamNoBranch
        .mapValues { value -> value.repeat(3) }
        .to("outopic-branch2", Produced.with(stringSerde, stringSerde).withName("SinkProcessor-Branch2"))

    // Делаем Branch3-----------------------------------------------------------------
    // Ответвляемся и печатаем тоже что и уходит в другой топик
    kStreamNoBranch.print(Printed.toSysOut<String?, String?>().withLabel("App debug info"))

    // Выводим Топологию
    log.info("Builder: {}", streamsBuilder.build().describe())
    // Построение DAG-а(топологии действий) ЗАКОНЧЕНО ----------------------------------------------

    // Создание и Запуск KafkaStreams --------------------------------------------------------------
    // Чтобы был запуск нужно СОЗДАТЬ STREAM-ы и передать туда DAG(топологии действий)
    // Опции для Stream в StreamsConfig - нужные конфиг опции для запуска
    val streamsConfigProps: Map<String, Any> = mapOf(
        StreamsConfig.APPLICATION_ID_CONFIG to "myAppIdKStream",
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
        Thread.sleep(500000)

        log.info("Application stopped")
        // Можно не закрывать т.к. мы в use{}
        //it.close()
    }
}