plugins {
    kotlin("jvm") version "1.8.22"
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("com.example.ApplicationKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.djl.ai/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("net.dankito.readability4j:readability4j:1.0.8")
    implementation(libs.kotlinx.coroutines.core)
    // Ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.test.host)

    // Exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.time)

    // H2 and logging
    implementation(libs.h2)
    implementation(libs.logback.classic)

    implementation("com.microsoft.onnxruntime:onnxruntime:1.17.0")
    implementation("ai.djl.huggingface:tokenizers:0.31.1") // DJL токенизатор
    implementation("ai.djl:api:0.31.1")
    implementation("ai.djl.huggingface:tokenizers:0.31.1")
    implementation("ai.djl.onnxruntime:onnxruntime-engine:0.31.1")

    // Ktor client
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Kotlin stdlib and tests
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(libs.kotlin.test.junit)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.8.22")
        }
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-coroutines")) {
            useVersion("1.6.4")
        }
    }
}
kotlin {
    jvmToolchain(8)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Test> {
    useJUnitPlatform()
}
// вот сюда — в самый конец файла (после dependencies и kotlin {...}, java {...} и т.п.)
tasks {
    shadowJar {
        archiveBaseName.set("farmer-assistant3")
        archiveClassifier.set("all")
        archiveVersion.set("") // Без версии в имени jar
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar) // чтобы ./gradlew build делал fat-jar
    }
}