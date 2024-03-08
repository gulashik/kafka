package org.gulash.kafkaspring.job

import org.gulash.kafkaspring.service.ProducerService
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.SendResult
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@Component
class ProducerJob(
    // Привязываем Producer-а
    val producer: ProducerService,
) {

    // нужно рядом с аннотацией @SpringBootApplication(SpringBoot Main class) добавить @EnableScheduling
    @Scheduled(fixedDelay = 1_000)
    fun send() {
        log.info("Producer job send")
        // Отправляем
        val resultCompletableFuture: CompletableFuture<SendResult<String, String>> =
            producer.sendMessage(topic = "input-data", key = "Some key", value = "Value-${LocalDateTime.now()}")

        // Ждём отправки
        val sendResult: SendResult<String, String> = resultCompletableFuture.get()

        // Логируем
        with(sendResult.recordMetadata){
            log.info("topic-${topic()} offset-${offset()} partition-${partition()} timestamp-${timestamp()} " +
                    "serializedKeySize-${serializedKeySize()} serializedValueSize-${serializedValueSize()}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProducerJob::class.java)
    }
}


