package org.gulash.kafkaspringconsumer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KafkaSpringConsumerApplication

fun main(args: Array<String>) {
    runApplication<KafkaSpringConsumerApplication>(*args)
}
