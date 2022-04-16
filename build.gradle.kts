plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.5.31"

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    // Use the Kotlin JDK 8 standard library.
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk11")
    implementation("org.apache.kafka:kafka-clients:2.8.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.2")
//    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.8")
//    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.10.2")
    implementation("info.picocli:picocli:4.6.3")

    implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    // Define the main class for the application.
    mainClass.set("kgitops.AppKt")
}
