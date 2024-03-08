package adminapi

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.NewPartitions
import org.apache.kafka.clients.admin.NewTopic
import kotlin.collections.listOf
import kotlin.collections.mapOf

fun main() {
    AddPartition().addPartition()
}
class AddPartition {
    fun addPartition() {
        // Процедура удаления топиков
        DeleteTopic().deleteAllTopics().also(::println)

        // Свойства для создния. Нужно много тут по минимуму.
        val properties = AdminUtils.getAdminConfig()

        // Создаём Admin с учётом свойств
        Admin.create(properties)
            // Оборачиваем в use, аналог try-with-resources
            .use { kafkaAdminClient ->

                // Создали топик с одной партицией
                kafkaAdminClient.createTopics(
                    listOf(NewTopic("ex6", 1, 1.toShort()))
                )
                    .all().get() // Ждём завершения

                // Добавляем партиции по указанного колисества
                kafkaAdminClient.createPartitions(
                    // Пара ИмяПартиции-КоличествоПартиций
                    mapOf("ex6" to  NewPartitions.increaseTo(3))
                )
                    .all().get() // Ждём завершения
            }
    }
}