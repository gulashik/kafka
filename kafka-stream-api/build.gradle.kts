plugins {
    kotlin("jvm") version "1.9.22"
}

group = "org.gulash.kfk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // kafka-streams включает в себя kafka-clients
    implementation ("org.apache.kafka:kafka-streams:3.4.0")

    implementation ("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation ("ch.qos.logback:logback-classic:1.4.12")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}