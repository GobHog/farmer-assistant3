import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.ExposedUser
import com.example.UserService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.mindrot.jbcrypt.BCrypt

// Модель для регистрации пользователя
@Serializable
data class RegistrationRequest(
    val surname: String,
    val name: String,
    val mail: String,
    val password: String // Не захешированный пароль
)

fun Application.configureAuthentication() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/backend_db",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "123",
    )
    val userService = UserService(database)

    routing {
        // Регистрация пользователя
        post("/register") {
            val user = call.receive<RegistrationRequest>()

            // Проверка на уникальность почты
            val existingUser = userService.getUserByEmail(user.mail)
            println(existingUser.toString())
            if (existingUser != null) {
                call.respond(HttpStatusCode.BadRequest, "Почта уже используется")
                return@post
            }

            val hashedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())
            val newUser = ExposedUser(user.surname, user.name, null, user.mail, hashedPassword, null, null, null)

            try {
                userService.create(newUser)  // попытаемся создать пользователя
                call.respond(HttpStatusCode.Created, "User registered successfully")
            } catch (e: IllegalArgumentException) {
                println("Ошибка при регистрации: ${e.message}")  // Логируем ошибку
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Ошибка при регистрации")
            }
        }

        // Логин и получение JWT токена
        post("/login") {
            val user = call.receive<RegistrationRequest>()

            // Получаем пользователя из базы по email
            val storedUser = userService.getUserByEmail(user.mail)
            if (storedUser != null && BCrypt.checkpw(user.password, storedUser.password)) {
                val token = createJWT(storedUser.mail)  // Используем email или username для токена
                call.respond(HttpStatusCode.OK, mapOf("token" to token))
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }
    }
}

fun createJWT(email: String): String {
    val algorithm = Algorithm.HMAC256("mySuperSecretKey")
    return JWT.create()
        .withIssuer("ktor-app")
        .withClaim("email", email)
        .sign(algorithm)
}

// Сохранение нового пользователя в базе данных
fun saveUserToDatabase(user: RegistrationRequest) {
    // Реализуйте сохранение пользователя в базу данных
}
