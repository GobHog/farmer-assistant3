import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun Application.configureRoleRoutes() {
    val database = Database.connect(
        url = "jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}",
        driver = "org.postgresql.Driver",
        user = System.getenv("DB_USER"),
        password = System.getenv("DB_PASSWORD")
    )

}