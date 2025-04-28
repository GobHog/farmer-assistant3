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
    val keyStore = loadKeyStore("my_keystore.p12", "123456") // путь к твоему файлу и пароль

    embeddedServer(Netty, environment = applicationEngineEnvironment {
        sslConnector(
            keyStore = keyStore,
            keyAlias = "myAlias",
            keyStorePassword = { "123456".toCharArray() },
            privateKeyPassword = { "123456".toCharArray() }
        ) {
            port = 8443
            host = "0.0.0.0"
        }

        module {
            // Твои роуты и логика
        }
    }).start(wait = true)
}

fun loadKeyStore(filePath: String, password: String): KeyStore {
    val keyStore = KeyStore.getInstance("JKS")
    FileInputStream(filePath).use { fis ->
        keyStore.load(fis, password.toCharArray())
    }
    return keyStore
}

fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureDatabases()
    configureRouting()
    configureAuthentication()
}
