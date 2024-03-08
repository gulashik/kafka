plugins {
    kotlin("jvm") version "1.9.22"
}

group = "org.gulash"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients
    implementation("org.apache.kafka:kafka-clients:3.6.1")

    // SLF4J API
    implementation ("org.slf4j:slf4j-api:2.0.11")
    // SLF4J Log4j Implementation
    //implementation ("org.slf4j:slf4j-log4j12:2.0.11")
    implementation ("ch.qos.logback:logback-classic:1.4.7")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}