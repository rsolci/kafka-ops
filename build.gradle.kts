import io.gitlab.arturbosch.detekt.Detekt

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("io.gitlab.arturbosch.detekt").version("1.20.0")

    jacoco
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.6.21"))

    implementation("org.apache.kafka:kafka-clients:3.1.0")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")

    // Parsing command line arguments
    implementation("com.github.ajalt.clikt:clikt:3.4.2")

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("io.github.microutils:kotlin-logging:2.1.21")

    // Code quality
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.20.0")

    // Test
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("io.mockk:mockk:1.12.4")
    testImplementation("org.junit-pioneer:junit-pioneer:1.7.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    testImplementation("io.github.embeddedkafka:embedded-kafka_2.13:3.2.0")
}

application {
    // Define the main class for the application.
    mainClass.set("io.github.rsolci.kafkaops.AppKt")
}

val jar by tasks.getting(Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "io.github.rsolci.kafkaops.AppKt"
    }

    // Adding all dependencies into uber jar
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    allRules = false // activate all available (even unstable) rules.
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "11"
    reports {
        html.required.set(true) // observe findings in your browser with structure and code snippets
        xml.required.set(true) // checkstyle like format mainly for integrations like Jenkins
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("jacoco/reports/jacoco.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("jacoco/reports/html"))
    }
}
