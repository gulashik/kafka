package adminapi

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.DescribeTopicsOptions
import org.apache.kafka.clients.admin.TopicDescription

fun main() {
    // Пересоздаём топики =====
    // Процедура удаления топиков
    DeleteTopic().deleteAllTopics().also(::println)

    // Создание топиков для экспериментов
    CreateTopic().createTopicSimple()

    // Свойства для создания. Нужно много тут по минимуму.
    val adminConfig: Map<String, Any> = AdminUtils.getAdminConfig()

    // Создаём KafkaAdminClient с учётом свойств
    val kafkaAdminClient: Admin = Admin.create(adminConfig)

    val mutableMap: MutableMap<String, TopicDescription> = kafkaAdminClient.describeTopics(
        listOf(
            "ex3-topic-1",
            "ex3-topic-2",
        ),
        DescribeTopicsOptions().includeAuthorizedOperations(true)
    )
        .allTopicNames()
        .get() // Нужен метод get т.к. KafkaFuture

    mutableMap.forEach{
        entry: Map.Entry<String, TopicDescription> ->
        val (name, description) = entry

        AdminUtils.log.info("{}: {}", name, description)
    }
}
