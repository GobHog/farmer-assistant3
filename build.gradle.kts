
plugins {

    kotlin("jvm") version "1.8.21" // замените на актуальную стабильную версию Kotlin
    id("io.ktor.plugin") version "2.3.12"
    alias(libs.plugins.kotlin.plugin.serialization)

}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "com.example.ApplicationKt"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        maven { url = uri("https://repo.maven.apache.org/maven2")  }
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap") // Для плагинов Ktor
        maven("https://plugins.gradle.org/m2/")
//        maven { url = uri("https://jcenter.bintray.com") }
    }

dependencies {
    implementation("org.postgresql:postgresql:42.7.1") // версия зависит от времени, можно самую свежую
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("io.ktor:ktor-server-status-pages:2.3.12")
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.time)
    implementation(libs.h2)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")

}
kotlin {
    jvmToolchain(8)
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8 // Используем JDK 8 для компиляции
    targetCompatibility = JavaVersion.VERSION_1_8 // Используем JDK 8 для выполнения тестов
}
tasks.withType<Test> {
    useJUnitPlatform() // Убедитесь, что тесты выполняются с правильной версией JDK
}
