package adminapi

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.CreateTopicsOptions
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.admin.TopicListing
import org.apache.kafka.common.errors.TopicExistsException
import java.util.*
import java.util.concurrent.ExecutionException

fun main() {

    // Процедура удаления топиков
    DeleteTopic().deleteAllTopics()
        .also(::println)
}

class DeleteTopic {
    // Свойства для создния. Нужно много тут по минимуму.
    val adminConfig: Map<String, Any> = AdminUtils.getAdminConfig()

    // Создаём KafkaAdminClient с учётом свойств
    val kafkaAdminClient: Admin = Admin.create(adminConfig)


    // Функция удаления для удобства вызова из вне
    fun deleteAllTopics() = deleteAllTopics(kafkaAdminClient)

    // Функция удаления
    fun deleteAllTopics(kafkaAdminClient: Admin): List<String> {
        // Список топиков
        val topics = kafkaAdminClient.listTopics()
            .listings()
            .get() // Нужен get т.к. KafkaFuture. Тут получим коллекцию
            .asSequence()
            .map { obj: TopicListing -> obj.name() }
            .toList()

        AdminUtils.log.info("External topics: {}", topics)

        kafkaAdminClient.deleteTopics(topics).all()
            .get() // Нужен get т.к. KafkaFuture. Ждём результат удаления.

        // Список топиков, которые будем проверять уже удалились или нет
        val checkedTopicList: List<NewTopic> = topics
            .asSequence()
            .map { s: String -> NewTopic(s, Optional.empty(), Optional.empty()) }
            .toList()

        // validateOnly(true) - только проверят возможность создания топика
        val topicsOptions: CreateTopicsOptions = CreateTopicsOptions().validateOnly(true)

        while (true) {
            try {
                // Если есть возможность создания топиков с такими же именами - Значит всё удалено
                kafkaAdminClient.createTopics(checkedTopicList, topicsOptions).all().get()
                // Прерываемся
                break
            } catch (ex: ExecutionException ) {
                // Если ошибка не про существование топика - пробрасываем
                if (ex.cause == null || ex.cause!!.javaClass != TopicExistsException::class.java) throw ex

                // Если топики не удалились - ждём
                AdminUtils.log.info("Topic not yet deleted - ${ex.cause}")
                // 21:40:02.170 [main] INFO  appl - Topic not yet deleted - org.apache.kafka.common.errors.TopicExistsException: Topic 'ex3-topic-1' is marked for deletion.

                Thread.sleep(100)
            }
        }

        AdminUtils.log.info("SUCCESS")

        return topics
    }
}