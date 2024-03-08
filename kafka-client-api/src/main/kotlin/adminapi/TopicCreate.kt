package adminapi

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.TopicConfig
import java.util.*
import kotlin.collections.listOf

fun main() {
    // Создание топиков для экспериментов
    CreateTopic().createTopicSimple()
}

class CreateTopic {
    fun createTopicSimple() {
        // Свойства для создания. Нужно много тут по минимуму.
        val properties = AdminUtils.getAdminConfig()

        // Создаём Admin с учётом свойств
        Admin.create(properties)
            // Оборачиваем в use, аналог try-with-resources
            .use { kafkaAdminClient ->

                val requestedTopics = listOf(
                    NewTopic(/*name*/"ex1-topic", /*num partitions*/1, /*replicationFactor*/1.toShort()),
                    NewTopic("ex3-topic-1", 1, 1.toShort())
                        .configs( /*накидываем опций*/
                            mapOf<String, String>(
                                TopicConfig.SEGMENT_MS_CONFIG to (1000 * 60 * 60).toString()
                            )
                        ),
                    NewTopic("ex3-topic-2", /*default-ы по топикому*/Optional.empty<Int>(), Optional.empty<Short>()),

                    // РУЧНАЯ раскладка партиций по Broker-ам.
                    // Шаблон - ПартицияНомер to listOf(ЛежитНаБрокереНомер, ЛежитНаБрокереНомер)[, ...]
                    // В этом примере НулеваяПартиция лежит на Брокерах 2 и 3, ПерваяПартиция лежит на Брокерах 4 и 5
                    //NewTopic("ex3-topic-3", mapOf<Int, List<Int>>(0 to listOf<Int>(2, 3), 1 to listOf<Int>(4, 5)))
                )
                // Создание топика. Будет создан не сразу!
                val topicResult = kafkaAdminClient.createTopics(requestedTopics)

                // не факт, что топик уже создан, пользоваться им нельзя

                // Ожидаем когда будут созданы топики
                topicResult.all().get()

                AdminUtils.sendMessages(0, 500, "ex1-topic", 0)
            }
    }
}