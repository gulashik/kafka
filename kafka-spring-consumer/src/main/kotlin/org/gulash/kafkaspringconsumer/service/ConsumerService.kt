package org.gulash.kafkaspringconsumer.service

import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class ConsumerService {
    @KafkaListener(topics = ["input-data"])
    fun consume(
        @Payload message: String?,
        @Header(KafkaHeaders.OFFSET) offset: Int,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String?,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String?,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) ts: Long,
    ) {
        log.info(
            """
            |Consumer------------------------
            |MESSAGE = ${message}
            |OFFSET = ${offset}
            |RECEIVED_KEY = ${key}
            |RECEIVED_PARTITION = ${partition}
            |RECEIVED_TOPIC = ${topic}
            |RECEIVED_TIMESTAMP = ${ts}
            """.trimMargin()
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(ConsumerService::class.java)
    }
}