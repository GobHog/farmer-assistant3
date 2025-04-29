package com.example

import ch.qos.logback.core.net.ssl.SSL
import configureAuthentication
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import io.ktor.server.engine.sslConnector
import java.io.FileInputStream
import java.security.KeyStore

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}
//fun main() {
//    // Используем JKS формат для загрузки keystore
//    val keyStore = KeyStore.getInstance("PKCS12")
//    keyStore.load(FileInputStream("my_keystore.jks"), "123456".toCharArray()) // Замените на ваш пароль
//
//    embeddedServer(Netty, environment = applicationEngineEnvironment {
//        sslConnector(
//            keyStore = keyStore,
//            keyAlias = "mykey", // Убедитесь, что alias правильный
//            keyStorePassword = { "123456".toCharArray() }, // Пароль к keystore
//            privateKeyPassword = { "123456".toCharArray() } // Пароль к приватному ключу
//        ) {
//            port = 8443
//            host = "0.0.0.0"
//        }
//
//        module {
//            // Ваши роуты и логика
//        }
//    }).start(wait = true)
//}
//
//fun loadKeyStore(filePath: String, password: String): KeyStore {
//    // Используем JKS формат
//    val keyStore = KeyStore.getInstance("JKS")
//    FileInputStream(filePath).use { fis ->
//        keyStore.load(fis, password.toCharArray())
//    }
//    return keyStore
//}

fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureDatabases()
    configureRouting()
    configureAuthentication()
}
