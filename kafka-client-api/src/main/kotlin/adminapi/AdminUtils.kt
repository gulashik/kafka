package adminapi

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

// Для удобства конфигурирования
object AdminUtils {
    val log: Logger = LoggerFactory.getLogger("appl")

    const val HOST: String = "localhost:9092"

    private val adminConfig = mutableMapOf<String, Any>(
        //Очень много параметров в ConsumerConfig
        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to HOST,
    )

    // Если нужно, то можно что-то переопределить
    fun getAdminConfig(addConfigs: Map<String, Any>? = null): Map<String, Any> {
        val resultConfig = HashMap<String, Any>(adminConfig)

        addConfigs?.let {
            resultConfig += addConfigs
        }

        return resultConfig
    }

    fun sendMessages(keyFrom: Int, keyTo: Int, topic: String?, partition: Int?) {
        KafkaProducer<String, String>(producerConfig).use {
            producer ->
            for (i in keyFrom until keyTo) {
                producer.send(
                    ProducerRecord<String, String>(
                        topic, partition, i.toString(),
                        "aaaaaaaaaaaaaaaaaaaaaaaaa"
                    )
                )
            }
            producer.flush()
        }
    }

    val producerConfig: Map<String, Any> = java.util.Map.of<String, Any>(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, HOST,
        ProducerConfig.ACKS_CONFIG, "all",
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java
    )

    fun createProducerConfig(builder: Consumer<Map<String, Any>?>): Map<String, Any> {
        val map = java.util.HashMap(producerConfig)
        builder.accept(map)
        return map
    }
}