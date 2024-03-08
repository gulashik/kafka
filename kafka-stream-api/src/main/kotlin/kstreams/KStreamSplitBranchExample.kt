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
import kotlin.random.Random

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
    var number = 0

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
        .selectKey { key, value -> "${++number}-${key}" }

    // Разделяем ветку по какому-то условию
    val splitIntoBranches: MutableMap<String, KStream<String, String>> = kStreamNoBranch
        // Разделяем stream в разные branch-и
        //  Split this stream into different branches.
        //  The returned BranchedKStream instance can be used for routing the records to different branches depending on evaluation against the supplied predicates.
        .split(Named.`as`("split-branch-")) // возвращается BranchedKStream<K, V>
        .branch(Predicate { key, value -> Random.nextBoolean() }, Branched.`as`("random1-true")/*дособираем имя, чтобы удобней ссылаться*/)
        .branch(Predicate { key, value -> Random.nextBoolean() }, Branched.`as`("random2-true")/*дособираем имя, чтобы удобней ссылаться*/)
        //.noDefaultBranch() // Branch-а по умолчанию не будет ЗНАЧЕНИЯ НЕ ПОПАВШИЕ В ВЕТКИ ВЫШЕ - ТЕРЯЮТСЯ!
        .defaultBranch(Branched.`as`("default-branch")/*дособираем имя, чтобы удобней ссылаться*/) // Branch если ничего не нашли

    // Работаем с "нарезанными" ветками
    // Produced - Producer-ы хранилка параметров
    splitIntoBranches
        .get("split-branch-random1-true")
        ?.to("random1-true", Produced.with(stringSerde, stringSerde).withName("SinkProcessor-split-branch-random1-true"))

    splitIntoBranches
        .get("split-branch-random2-true")
        ?.to("random2-true", Produced.with(stringSerde, stringSerde).withName("SinkProcessor-split-branch-random2-true"))

    splitIntoBranches
        .get("split-branch-default-branch")
        // foreach - терминальная операция. Можно положить например в БД
        ?.foreach { key, value -> println("FOREACH terminal operation key-${key} value-${value}") }

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
        KafkaProducerCls().start(100)

        Thread.sleep(50000)

        log.info("Application stopped")
        // Можно не закрывать т.к. мы в use{}
        //it.close()
    }
}