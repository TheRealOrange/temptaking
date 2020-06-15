plugins {
    application
    id("com.github.johnrengelman.shadow") version "5.1.0"
    kotlin("jvm") version "1.3.70"
    kotlin("plugin.serialization") version "1.3.70"
}

application {
    mainClassName = "MainKt"
}

rootProject.name

group="therealorange"
version="1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    val selenium_version = "4.0.0-alpha-6"
    val logback_version = "1.2.3"

    implementation(kotlin("stdlib", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
    testImplementation("junit:junit:4.12")

    implementation("com.jessecorbett:diskord:1.6.2")

    implementation("org.seleniumhq.selenium:selenium-java:$selenium_version")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:$selenium_version")

    implementation("org.slf4j:slf4j-api:1.7.26")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("ch.qos.logback:logback-core:$logback_version")

    implementation("com.google.cloud:google-cloud-logging-logback:0.117.0-alpha")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
}

fun Configuration.isDeprecated(): Boolean =
    if (this is org.gradle.internal.deprecation.DeprecatableConfiguration) {
        resolutionAlternatives != null
    } else {
        false
    }

tasks.register("downloadDependencies") {
    doLast {
        val allDeps = configurations.names
            .map { configurations[it] }
            .filter { it.isCanBeResolved && !it.isDeprecated() }
            .map { it.resolve().size }
            .sum()
        println("Downloaded all dependencies: $allDeps")
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>() {
    manifest {
        attributes["Main-Class"] = "HelloKt"
    }
}
