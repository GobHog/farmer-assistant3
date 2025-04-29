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
import java.util.logging.Logger


fun Application.configureAuthentication() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/backend_db",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "123",
    )
    val userService = UserService(database)
    val logger = Logger.getLogger("RegistrationLogger")

    routing {
        // Регистрация пользователя
        post("/register") {
            val user = call.receive<RegistrationRequest>()

            val existingUser = userService.getUserByEmail(user.mail)
//            logger.info("Проверка существования пользователя с email: ${user.mail}")
//            println(existingUser.toString())

            if (existingUser != null) {
//                logger.warning("Почта уже используется: ${user.mail}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(success = false, message = "Почта уже используется")
                )
                return@post
            }

            val hashedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())
            val newUser = ExposedUser(user.surname, user.name, null, user.mail, hashedPassword, null, null, null)

            try {
//                logger.info("Попытка создать нового пользователя: ${user.mail}")
                userService.create(newUser)
//                logger.info("Пользователь успешно зарегистрирован: ${user.mail}")
                call.respond(
                    HttpStatusCode.Created,
                    RegisterResponse(success = true, message = "User registered successfully")
                )
            } catch (e: IllegalArgumentException) {
//                logger.severe("Ошибка при регистрации пользователя: ${e.message}")
//                println("Ошибка при регистрации: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(success = false, message = e.message ?: "Ошибка при регистрации")
                )
            } catch (e: Exception) {
//                logger.severe("Необработанная ошибка при регистрации: ${e.message}")
//                println("Необработанная ошибка: ${e.message}")
                call.respond(
                    HttpStatusCode.InternalServerError,
                    RegisterResponse(success = false, message = "Внутренняя ошибка сервера")
                )
            }
        }


        // Логин и получение JWT токена
        post("/login") {
            val user = call.receive<RegistrationRequest>()

            // Получаем пользователя из базы по email
            val storedUser = userService.getUserByEmail(user.mail)
            if (storedUser != null) {
                if (BCrypt.checkpw(user.password, storedUser.password)) {
                    val token = createJWT(storedUser.mail)  // Используем email для токена
                    logger.info("Пользователь авторизован: ${storedUser.mail}")
                    call.respond(HttpStatusCode.OK, mapOf("token" to token))
                } else {
                    logger.warning("Неверный пароль для пользователя: ${user.mail}")
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                }
            } else {
                logger.warning("Пользователь не найден: ${user.mail}")
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
