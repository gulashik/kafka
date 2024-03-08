package adminapi

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.DescribeProducersResult
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.TopicPartition

fun main() {
    ProducerDescribe().producerDescribe()
}

class ProducerDescribe {
    fun producerDescribe() {
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
                    listOf(NewTopic("for_producer_describe", 1, 1.toShort()))
                )
                    .all().get() // Ждём завершения

                // Положили туда сообщения
                AdminUtils.sendMessages(0, 500, "for_producer_describe", 0)

                // Получаем описание Продюсера
                val info: MutableMap<TopicPartition, DescribeProducersResult.PartitionProducerState> =
                    kafkaAdminClient.describeProducers(listOf(TopicPartition("for_producer_describe", 0)))
                        .all().get() // Ждём завершения

                AdminUtils.log.info("Info\n{}", info)
                // {for_producer_describe-0=PartitionProducerState(activeProducers=[ProducerState(producerId=9, producerEpoch=0, lastSequence=499,
                // lastTimestamp=1712267679693, coordinatorEpoch=OptionalInt.empty, currentTransactionStartOffset=OptionalLong.empty)])}
            }
    }
}