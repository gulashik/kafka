package adminapi

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.admin.RecordsToDelete
import org.apache.kafka.common.TopicPartition

fun main() {
    RecordDelete().recordDelete()
}
class RecordDelete {
    fun recordDelete() {
        // Процедура удаления топиков
        DeleteTopic().deleteAllTopics().also(::println)

        // Свойства для создания. Нужно много тут по минимуму.
        val properties = AdminUtils.getAdminConfig()

        // Создаём Admin с учётом свойств
        Admin.create(properties)
            // Оборачиваем в use, аналог try-with-resources
            .use { kafkaAdminClient ->

                // Создали топик
                kafkaAdminClient.createTopics(
                    listOf(NewTopic("for_delete_recors", 1, 1.toShort()))
                )
                    .all().get() // Ждём завершения

                // Положили туда сообщения
                AdminUtils.sendMessages(0, 500, "for_delete_recors", 0)

                // Очищаем сообщения
                kafkaAdminClient.deleteRecords(
                    mapOf(
                        TopicPartition("for_delete_recors", 0) to RecordsToDelete.beforeOffset(250)
                    )
                )
                    .all().get()// Ждём завершения
            }
    }
}