package org.gulash.kfk.kstreams

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.Repartitioned
import org.apache.kafka.streams.processor.StreamPartitioner
import org.apache.kafka.streams.processor.api.FixedKeyProcessor
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext
import org.apache.kafka.streams.processor.api.FixedKeyRecord
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import org.gulash.kfk.clientapi.producer.KafkaProducerCls
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.abs
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
    val kStreamProc = streamsBuilder
        // ---- Хранилка
        .addStateStore( // Добавляем хранилку состояния
            Stores.keyValueStoreBuilder( // тип Хранилки
                // Имя хранилки.
                // Лежaт
                // 1 в топике с где в названии есть application.id + имя_персистентного_хранилища + changelog
                // например "myAppIdKStream-persistent-store-changelog"
                // 2 Лежат локально на машине файлами удалить можно
                //      в ручную rm -rf <state.dir>/<application.id> (e.g., rm -rf /var/lib/kafka-streams/my-streams-app)
                //      через метод cleanUp() перед запуском или после остановки KafkaStream
                // почитать тут https://www.confluent.io/blog/data-reprocessing-with-kafka-streams-resetting-a-streams-application/
                Stores.persistentKeyValueStore("persistent-store"),
                Serdes.IntegerSerde(),  // keySerde
                Serdes.StringSerde() // valueSerde
            )
                //.withLoggingDisabled() // Отключение логирования в Kafka топик
                //.withLoggingEnabled(mapOf(xxxx)) // Настройки Kafka топика для логирования если нужно
        )


    val kStreamRes: KStream<Int, String> = kStreamProc
        // ----
        // Метод stream - ВХОДНАЯ ТОЧКА
        .stream("intopic", consumed)
        // ----
        // Делаем ключ числовым от 1 до 3 смысла в этом нет. Просто для простоты в repartition
        .selectKey { key, value -> Random.nextInt(1/*inclusive*/, 4/*exclusive*/) }
        // Изменяем индекс партиции и следовательно обработчик куда будут приходить сообщения.
        // Видимо имеет смысл если нельзя изменить сам ключ.
        .repartition(
            Repartitioned
                .with(/*key*/Serdes.IntegerSerde(), /*value*/Serdes.StringSerde())
                .withStreamPartitioner(/*Нужная реализация*/MyRepartitioner())
        )
        .mapValues { readOnlyKey, value -> listOf("a", "b", "c", "d", "e").shuffled()[0] }
        .peek { key, value -> println("$key -> $value") } // чего до
        .processValues(
            ::MyFixedProcessor,
            "persistent-store"
        )
        .peek { key, value -> println("$key -> $value") } // чего после

    kStreamRes
        .to("state-topic", Produced.with(Serdes.IntegerSerde(), Serdes.StringSerde()))


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

        // Для очистки состояния нужно выполнить 2 пукта ОДНОВРЕМЕННО
        // 1 вызвать метод cleanUp() или удалить из папки в ручную rm -rf <state.dir>/<application.id> (e.g., rm -rf /var/lib/kafka-streams/my-streams-app)
        // 2 Удалить топик с где в названии есть application.id + имя_персистентного_хранилища + changelog например "myAppIdKStream-persistent-store-changelog"
        // почитать тут https://www.confluent.io/blog/data-reprocessing-with-kafka-streams-resetting-a-streams-application/
        it.cleanUp()

        // KafkaStream фактически начинает после вызова метода start()
        it.start()

        // ВАЖНО!!! Stream работает ПОКА ВЫПОЛНЯЕТСЯ USE блок
        KafkaProducerCls().start(10)

        Thread.sleep(50000)

        log.info("Application stopped")
        // Можно не закрывать т.к. мы в use{}
        //it.close()
    }
}

// FixedKeyProcessor - означает, что ключи не меняются, а меняются только значения
class MyFixedProcessor: FixedKeyProcessor</*key*/Int, /*value*/String, /*результат*/String> {

    private lateinit var context: FixedKeyProcessorContext</*key*/Int, /*результат*/String>
    private lateinit var store: KeyValueStore</*key*/Int, /*value*/String>

    // Вызывается при инициализации
    override fun init(context: FixedKeyProcessorContext</*key*/Int, /*результат*/String>) {
        //super.init(context)
        this.context = context
        this.store = context.getStateStore("persistent-store")
    }

    override fun process(record: FixedKeyRecord<Int, String>) {
        val currentState: String? = store[record.key()]

        val newState: String = (currentState?.let { "$it/" } ?: "") + record.value()

        // Сохраняем новое состояние в хранилке
        store.put(record.key(), newState)

        // Передача нового состояние Value дальше
        context.forward(record.withValue(newState))
    }

    override fun close() {
        super.close()
    }
}

class MyRepartitioner: StreamPartitioner<Int, String> {
    override fun partition(topic: String, key: Int, value: String, numPartitions: Int): Int {
        // Вычисляем НОВЫЙ индекс партиции топика если
        //  Партиций более одной
        //  Логическое разбиение не соответствует ключу сообщения
        // ЗАЧЕМ. Если нужно сохранение состояния то сообщения для обработки будут распределяться по ключу, т.е. не так как будем вычислять состояние.
        // ПРОБЛЕМА. Т.к. ОДИН обработчик создаётся на ОДНУ партицию + Логически НУЖНЫЕ В ОДНОМ обработчике сообщения приходят в РАЗНЫЕ обработчики.
        //  Получаем НЕСКОЛЬКО локальных состояний на одно логическое значение/ключ

        // Тут проблемы нет. Лень её делать :)
       return abs(key.hashCode()) % numPartitions
    }
}