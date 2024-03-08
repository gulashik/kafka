package clientapi.processorapi

import clientapi.processorapi.Utils.INTOPIC
import clientapi.processorapi.Utils.log
import mu.KotlinLogging
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.gulash.kfk.clientapi.producer.KafkaProducerNonUniqueKeyCls
import kotlin.concurrent.thread


fun main() {
    /*
     Концептульно создание и использование KafkaStreams состоит
     Построение DAG-а(топологии действий)
         Создание объект Topology
         Добавляем Source node -
         Добавляем Stream nodes
         Добавляем Sink nodes

     Экземпляр KafkaStreams
         обогащаем DAG-ом
         запускаем поток
         закрываем поток
 */
    // объект Topology
    val topology: Topology = Topology()

    // Добавляем Source node
    // ВАЖНО! генерация DAG-а не через порядок записи, а через ссылки в "parentNames"
    topology
        .addSource(
            /* name = */ "my-source-processor",
            /* keyDeserializer = */Serdes.StringSerde().deserializer(),
            /* valueDeserializer = */Serdes.StringSerde().deserializer(),
            /* ...topics = */INTOPIC
        )
        // Добавляем Stream nodes
        .addProcessor(
            /* name = */ "my-trim-processor",
            /* supplier = */ ::MyTrimProcessor,
            /* ...parentNames = */ "my-source-processor" // имя от кого ждём record
        )
        .addProcessor(
            /* name = */ "my-log-processor",
            /* supplier = */ ::MyLoggerProcessor,
            /* ...parentNames = */ "my-trim-processor", // имя от кого ждём record
        )
        //Добавляем Sink nodes
        .addSink(
            /* name = */ "my-sink-processor",
            /* topic = */ "outtopic",
            /* keySerializer = */ Serdes.StringSerde().serializer(),
            /* valueSerializer = */ Serdes.StringSerde().serializer(),
            /* ...parentNames = */ "my-log-processor" // имя от кого ждём record
        )

    // Печать топологии
    log.info("{}", topology.describe())

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
        topology, // Topology - содержит DAG, который мы на создавали
        streamsConfig // Опции для Stream
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
            KafkaProducerNonUniqueKeyCls(INTOPIC).start(10)
        }

        Thread.sleep(50000)

        log.info("Application stopped")
        // Можно не закрывать т.к. мы в use{}
        //it.close()
    }
}

// Классы обработчики-----------------------------------------
class MyTrimProcessor : /*Наследуемся*/Processor</*keyIn*/String, /*valueIn*/String, /*keyOut*/String, /*valueOut*/String> {
    private lateinit var context: ProcessorContext<String, String>

    override fun init(context: ProcessorContext<String, String>) {
    // Получаем контекст. Нужен для возможности дальше отправить запись по потоку
        this.context = context
    }

    override fun process(record: Record<String, String>) {

        context.forward(// Отправляем record дальше
            Record(// Resulting record
                record.key(),/*new key*/
                record.value()[1].toString(), /*new value*/
                record.timestamp() /*timestamp*/
            )
        )
    }
}

class MyLoggerProcessor : /*Наследуемся*/Processor</*keyIn*/String, /*valueIn*/String, /*keyOut*/String, /*valueOut*/String> {
    private lateinit var context: ProcessorContext<String, String>

    override fun init(context: ProcessorContext<String, String>) {
        // Получаем контекст. Нужен для возможности дальше отправить запись по потоку
        this.context = context
    }

    override fun process(record: Record<String, String>) {
        log.info("LOGGER PROCESSOR-----> ${record}")
        // Отправляем record дальше
        context.forward(record)
    }
}

object Utils {
    val log = KotlinLogging.logger {}
    const val INTOPIC = "intopic"
}