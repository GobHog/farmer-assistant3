import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Application.configureNewsArticleRoutes() {
    val database = Database.connect(
        url = "jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}",
        driver = "org.postgresql.Driver",
        user = System.getenv("DB_USER"),
        password = System.getenv("DB_PASSWORD")
    )

    val newsArticleService = NewsArticleService(database)

    routing {
        get("/news") {
            val news = newsArticleService.readAll().map {
                NewsArticleResponse(
                    content = it.content,
                    creation_date = it.creation_date,
                    link = it.link
                )
            }
            val summary = SummaryStorage.lastSummary ?: "Выжимка недоступна"
            call.respond(NewsPageResponse(news = news, summary = summary))
        }

    }
}

