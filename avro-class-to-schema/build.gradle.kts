plugins {
    kotlin("jvm") version "1.9.23"
}

group = "org.gulash.kfk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // The versions of the Avro and Avro-tools libraries should be equal to prevent conflicts from arising.
    // https://mvnrepository.com/artifact/org.apache.avro/avro
    implementation("org.apache.avro:avro:1.11.3")

    // SLF4J API
    implementation ("org.slf4j:slf4j-api:2.0.11")
    // SLF4J Log4j Implementation
    //implementation ("org.slf4j:slf4j-log4j12:2.0.11")
    implementation ("ch.qos.logback:logback-classic:1.4.12")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}