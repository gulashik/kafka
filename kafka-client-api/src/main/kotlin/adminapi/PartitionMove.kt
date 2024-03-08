package adminapi

import org.apache.kafka.clients.admin.*
import org.apache.kafka.common.TopicPartition
import java.util.*
import java.util.Map
import kotlin.collections.List
import kotlin.collections.listOf


fun main() {
    MovePartition().movePartition()
}
class MovePartition {
    fun movePartition() {
        // Процедура удаления топиков
        DeleteTopic().deleteAllTopics().also(::println)

        // Свойства для создния. Нужно много тут по минимуму.
        val properties = AdminUtils.getAdminConfig()

        // Создаём Admin с учётом свойств
        Admin.create(properties)
            // Оборачиваем в use, аналог try-with-resources
            .use { kafkaAdminClient ->

                kafkaAdminClient.createTopics(
                    java.util.List.of(
                        NewTopic(
                            "ex7-1",
                            Map.of<Int, List<Int>>(
                                0, listOf<Int>(1, 2),
                                1, listOf<Int>(2, 3)
                            )
                        )
                    )
                ).all().get()

                // пошлем немного сообщений, чтобы переброс занял некоторое время
                AdminUtils.sendMessages(0, 500, "ex7-1", 0)

                // переместим партиции
                kafkaAdminClient.alterPartitionReassignments(
                    Map.of(
                        TopicPartition("ex7-1", 0),
                        Optional.of(NewPartitionReassignment(listOf<Int>(4, 5)))
                    )
                ).all().get()

                // процесс все еще идет
                var current: MutableMap<TopicPartition, PartitionReassignment> = kafkaAdminClient.listPartitionReassignments().reassignments().get()
                AdminUtils.log.info("Assigments after reassign {}", current)

                Thread.sleep(1000)

                // скорее всего перемещение закончилось
                current = kafkaAdminClient.listPartitionReassignments().reassignments().get()
                AdminUtils.log.info("Assigments after pause, {}", current)
            }
    }
}