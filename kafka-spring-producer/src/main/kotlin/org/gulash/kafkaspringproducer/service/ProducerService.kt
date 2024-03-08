package org.gulash.kafkaspring.service

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class ProducerService(
    // Нужен Kafka template
    val kafkaTemplate: KafkaTemplate<String, String>
) {
    fun sendMessage(topic: String, key: String, value: String): CompletableFuture<SendResult<String, String>> =
        kafkaTemplate.send(topic, key, value)
            .also { log.debug("Sending message to topic {}", topic) }

    companion object {
        private val log = LoggerFactory.getLogger(ProducerService::class.java)
    }
}