package com.example

import ExposedNewsArticle
import NewsArticle
import NewsArticleService
import OllamaRequest
import OllamaResponse
import OnnxRubertPredictor
import RubertPredictor
import TfidfModel
import TranslationRequest
import TranslationResponse
import ch.qos.logback.core.net.ssl.SSL
import configureAuthentication
import configureGroupRoutes
import configureGroupTaskRoutes
import configureNewsArticleRoutes
import configureRoleRoutes
import dbQuery
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import io.ktor.server.engine.sslConnector
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import java.io.FileInputStream
import java.security.KeyStore
import java.time.LocalDateTime
import org.jsoup.Jsoup
import net.dankito.readability4j.Readability4J
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}
fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureAuthentication()
    configureRouting()
    configureDatabases() // Передаём нужные сервисы в конкретные функции
    configureGroupRoutes()
    configureGroupTaskRoutes()
    configureNewsArticleRoutes()
    configureRoleRoutes()
    // Запуск фоновой задачи после старта приложения
    environment.monitor.subscribe(ApplicationStarted) {
        println("Запуск после старта приложения")

        try {
            val modelPath = Paths.get("src/main/resources/ru_bert.onnx")
            val absPath = modelPath.toAbsolutePath()
            println("Абсолютный путь к модели: $absPath")

            if (Files.exists(modelPath)) {
                println("✅ Модель найдена по пути: $modelPath")
            } else {
                println("❌ Модель не найдена по пути: $modelPath")
            }

            val predictor = try {
                OnnxRubertPredictor(absPath)
            } catch (t: Throwable) {
                println("❌ Ошибка при создании OnnxRubertPredictor: ${t.message}")
                t.printStackTrace()
                throw t
            }


            println("✅ ONNX модель успешно загружена.")


            val database = Database.connect(
                url = "jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}",
                driver = "org.postgresql.Driver",
                user = System.getenv("DB_USER"),
                password = System.getenv("DB_PASSWORD")
            )

            val newsArticleService = NewsArticleService(database)

            launch {
                while (true) {
                    try {
                        println("🕒 Запуск фоновой задачи: получение новостей")

                        val latestNews = fetchLatestNews()
                        var addedAtLeastOne = false

                        for ((index, article) in latestNews.withIndex()) {
                            try {
                                if (newsArticleService.existsByLink(article.url)) {
                                    println("⚠️ Новость уже существует в базе: ${article.url}")
                                    continue
                                }

                                val articleText = fetchArticleText(article.url)
                                val score = predictor.predict(articleText)

                                if (score >= 0.5) {
                                    println("\n✅ [${"%.2f".format(score * 100)}%] Новость ${index + 1}: ${article.content}")
                                    println("🔗 URL: ${article.url}\n")

                                    val newsArticle = ExposedNewsArticle(
                                        content = articleText,
                                        creation_date = LocalDate.now().toString(),
                                        link = article.url
                                    )
                                    newsArticleService.create(newsArticle)
                                    addedAtLeastOne = true
                                    println("✅ Новость сохранена в базу данных.")
                                }
                            } catch (e: Exception) {
                                println("❌ Ошибка при обработке статьи: ${e.message}")
                            }
                        }

                        if (addedAtLeastOne || SummaryStorage.lastSummary.isBlank()) {
                            println("📥 Извлекаем 5 последних сохранённых новостей из БД...")
                            val recentNews = newsArticleService.readLatest5()

                            recentNews.forEachIndexed { i, news ->
                                println("📰 ${i + 1}) ${news.content.take(100)}...")
                            }

                            val combinedText = recentNews.joinToString("\n\n") { "• ${it.content}" }
                            val summary = getSummaryFromOllama(combinedText.take(3000))

                            println("\n📝 Выжимка по последним новостям:\n$summary")
                        } else {
                            println("ℹ️ Новостей не добавлено, и выжимка уже существует.")
                        }

                        println("✅ Завершение итерации фоновой задачи")
                    } catch (e: Exception) {
                        println("❌ Ошибка в фоновой задаче: ${e.message}")
                    }

                    delay(3 * 60 * 1000L) // 10 минут
                }
            }

        } catch (e: Exception) {
            println("❌ Ошибка при инициализации модели: ${e.message}")
        }

        println("Завершение инициализации")
    }




}
// --- Обновлённая функция ---
suspend fun getSummaryFromOllama(text: String): String {
    val prompt = """
    Проанализируй следующие новости и выдели только те факты, которые оказывают реальное влияние на сельское хозяйство. 
    Используй до 4 лаконичных предложений без вводных слов и оценочных выражений. 
    Не упоминай новости, которые не связаны с аграрным сектором, даже если они есть в тексте. 
    Ответ должен быть только на русском языке и содержать исключительно релевантные факты:

    $text
""".trimIndent()



    val request = OllamaRequest(
        model = "llama3:latest",
        prompt = prompt,
        stream = false
    )

    return try {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }.use { client ->
            val rawResponse = client.post("http://31.128.51.62:11434/api/generate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.bodyAsText()

            println("Raw Ollama response: $rawResponse")

            val response = Json { ignoreUnknownKeys = true }.decodeFromString<OllamaResponse>(rawResponse)
            val cleanedSummary = response.response.trim()

            // Сохраняем результат
            SummaryStorage.lastSummary = cleanedSummary

            cleanedSummary
        }
    } catch (e: Exception) {
        println("❌ Ошибка при обращении к Ollama: ${e.message}")
        "⚠️ Не удалось получить выжимку."
    }
}



suspend fun fetchLatestNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
    val doc = Jsoup.connect("https://iz.ru/tag/selskoe-khoziaistvo").get()

    val newsBoxes = doc.select("div.tag-materials-item__box").take(5)

    newsBoxes.mapNotNull { box ->
        val linkElement = box.selectFirst("a.tag-materials-item") ?: return@mapNotNull null
        val url = linkElement.absUrl("href")

        return@mapNotNull try {
            val html = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get()
                .html()

            val readability = Readability4J(url, html)
            val article = readability.parse()

            val title = article.title?.trim()
            if (!title.isNullOrBlank() && url.isNotBlank()) {
                NewsArticle(
                    url = url,
                    creation_date = LocalDate.now().toString(),
                    content = title
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("❌ Ошибка при обработке $url: ${e.message}")
            null
        }
    }
}

suspend fun fetchArticleText(url: String): String = withContext(Dispatchers.IO) {
    val doc = Jsoup.connect(url)
        .userAgent("Mozilla/5.0")
        .get()
        .html()
    val readability = Readability4J(url, doc)
    val article = readability.parse()
    article.title ?: ""
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