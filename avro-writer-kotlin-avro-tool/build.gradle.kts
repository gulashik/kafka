import org.apache.avro.tool.SpecificCompilerTool
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

buildscript {
    dependencies {
        // Добавляем avro-tools jar в classpath через build script
        // In the Avro schema code generation context, the custom Gradle task responsible for this task needs access
        //      to the Avro-tools library early in the build process, i.e., before general dependencies are loaded

        // The versions of the Avro and Avro-tools libraries should be equal to prevent conflicts from arising.
        classpath("org.apache.avro:avro-tools:1.11.3")
    }
}

plugins {
    kotlin("jvm") version "1.9.23"
}

group = "org.gulash.kfk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    // Добавляем confluent репозиторий
    maven {
        url = URI("https://packages.confluent.io/maven")
    }
}

dependencies {
    // The versions of the Avro and Avro-tools libraries should be equal to prevent conflicts from arising.
    // https://mvnrepository.com/artifact/org.apache.avro/avro
    implementation("org.apache.avro:avro:1.11.3")
    // The versions of the Avro and Avro-tools libraries should be equal to prevent conflicts from arising.
    // https://mvnrepository.com/artifact/org.apache.avro/avro-tools
    implementation("org.apache.avro:avro-tools:1.11.3") { exclude ("org.slf4j") }

    // SLF4J API
    implementation ("org.slf4j:slf4j-api:2.0.11")
    // SLF4J Log4j Implementation
    //implementation ("org.slf4j:slf4j-log4j12:2.0.11")
    implementation ("ch.qos.logback:logback-classic:1.4.12")

    testImplementation(kotlin("test"))
}

// где лежат схемы
val avroSchemasDir = "src/main/avro"

// куда хотим положить
//val avroCodeGenerationDir = "build/generated-main-avro-custom-java"
val avroCodeGenerationDir = "src/main/java"

// Add the generated Avro Java code to the Gradle source files.
//sourceSets.main.java.srcDirs += [avroCodeGenerationDir]
sourceSets{
    main{
        java.srcDirs.add(File(avroCodeGenerationDir))
        kotlin.srcDirs.add(File(avroCodeGenerationDir))
        //java.srcDirs.add(File(avroCodeGenerationDir))
        //kotlin.srcDirs.add(File(avroSchemasDir))
    }
}

// Регистрируем задачу на генерацию
tasks.register("customAvroCodeGeneration") {
    // Define the task inputs and outputs for the Gradle up-to-date checks.
    inputs.dir(avroSchemasDir)
    outputs.dir(avroCodeGenerationDir)
    // The Avro code generation logs to the standard streams. Redirect the standard streams to the Gradle log.
    logging.captureStandardOutput(LogLevel.INFO);
    logging.captureStandardError(LogLevel.ERROR)

    doLast {
        // Run the Avro code generation.
        // https://avro.apache.org/docs/1.11.1/api/java/org/apache/avro/tool/SpecificCompilerTool.html
        SpecificCompilerTool().run(System.`in`, System.out, System.err, listOf(
            "-encoding", "UTF-8",
            "-string",
            "-fieldVisibility", "private",
            "-noSetters",
            "schema", "$projectDir/$avroSchemasDir", "$projectDir/$avroCodeGenerationDir"
        )
        )
    }
}

// добавляем в зависимости
tasks.withType(KotlinCompile::class.java).configureEach {
    // Make Java compilation tasks depend on the Avro code generation task.
    dependsOn("customAvroCodeGeneration")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}