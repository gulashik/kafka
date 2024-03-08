package adminapi

import org.apache.kafka.clients.admin.Admin
import org.apache.kafka.clients.admin.ConsumerGroupDescription
import org.apache.kafka.clients.admin.ConsumerGroupListing
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import kotlin.collections.MutableCollection
import kotlin.collections.MutableMap
import kotlin.collections.listOf

fun main() {
    // Запустить вручную
    //  kotlin/config/cls/ProducerMaps.kt
    //  kotlin/config/cls/ConsumerMaps.kt

    ConsumerGroupsActions().consumerGroupExample()
}

class ConsumerGroupsActions {
    fun consumerGroupExample() {

        // Свойства для создния. Нужно много тут по минимуму.
        val properties = AdminUtils.getAdminConfig()

        // Создаём Admin с учётом свойств
        Admin.create(properties)
            // Оборачиваем в use, аналог try-with-resources
            .use { kafkaAdminClient ->

                // Список ConsumerGroups
                val groups: MutableCollection<ConsumerGroupListing> = kafkaAdminClient.listConsumerGroups()
                    .all().get() // Ждём получения

                AdminUtils.log.info("Groups\n{}", groups)
                // [(groupId='app-group', isSimpleConsumerGroup=false, state=Optional[Stable])]

                // Описание ConsumerGroups
                val consumerGroupDescription: MutableMap<String, ConsumerGroupDescription> =
                    kafkaAdminClient.describeConsumerGroups(listOf("app-group"))
                        .all().get() // Ждём получения

                AdminUtils.log.info("ConsumerGroupDescription\n{}", consumerGroupDescription)
                // {app-group=(groupId=app-group, isSimpleConsumerGroup=false, members=(memberId=consumer-app-group-1-429db170-042a-4164-87a8-f03be99d940b,
                //  groupInstanceId=null, clientId=consumer-app-group-1, host=/192.168.127.1, assignment=(topicPartitions=mytopic-0)), partitionAssignor=range,
                //  state=Stable, coordinator=127.0.0.1:9092 (id: 1 rack: null), authorizedOperations=null)}

                // Текущие Offset-ы по ConsumerGroups
                val offsets: MutableMap<String, MutableMap<TopicPartition, OffsetAndMetadata>> =
                    kafkaAdminClient.listConsumerGroupOffsets("app-group")
                        .all().get() // Ждём получения

                AdminUtils.log.info("ListConsumerGroupOffsets\n{}", offsets)
                // {app-group={mytopic-0=OffsetAndMetadata{offset=4479, leaderEpoch=0, metadata=''}}}

                // Изменение Offset по ConsumerGroups
                // Чтобы изменить offset по Consumer Group она должна быть пустая т.е. НЕТ АКТИВНЫХ КОНСЮМЕРОВ
                // Т.е. ОСТАНАВЛИВАЕМ kotlin/config/cls/ConsumerMaps.kt и ждём пока отвалится от группы
                kafkaAdminClient.alterConsumerGroupOffsets("app-group",mapOf(TopicPartition("mytopic", 0) to OffsetAndMetadata(5)))
                    .all().get()// Ждём получения
                // Offset изменился на нужный
                AdminUtils.log.info("ListConsumerGroupOffsets\n{}", offsets)
                // {app-group={mytopic-0=OffsetAndMetadata{offset=5, leaderEpoch=null, metadata=''}}}
            }
    }
}