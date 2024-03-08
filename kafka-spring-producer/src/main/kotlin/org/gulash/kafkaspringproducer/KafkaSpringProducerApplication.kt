package org.gulash.kafkaspring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class KafkaSpringProducerApplication

fun main(args: Array<String>) {
	runApplication<KafkaSpringProducerApplication>(*args)
}
