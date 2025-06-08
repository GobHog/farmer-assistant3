import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
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
import java.util.*
import java.util.logging.Logger
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Base64

fun Application.configureAuthentication() {
    val database = Database.connect(
        url = "jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}",
        driver = "org.postgresql.Driver",
        user = System.getenv("DB_USER"),
        password = System.getenv("DB_PASSWORD")
    )

    val userService = UserService(database)
    val logger = Logger.getLogger("RegistrationLogger")

    routing {
        // Регистрация пользователя
        post("/register") {
            val user = call.receive<RegistrationRequest>()

            val existingUser = userService.getUserByEmail(user.mail)

            if (existingUser != null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(success = false, message = "Почта уже используется")
                )
                return@post
            }

            val hashedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())

            val newUser = ExposedUser(
                surname = user.surname,
                name = user.name,
                patronymic = null,
                mail = user.mail,
                password = hashedPassword,
                photo = null,
                group_id = null,
                role_id = null,
                email_confirmed = false
            )

            try {
                // Сохраняем пользователя и получаем его ID
                val userId = userService.create(newUser)

                // Генерируем ссылку с userId
                val confirmationLink = generateConfirmationLink(user.mail, userId)

                sendEmail(
                    to = user.mail,
                    subject = "Подтверждение почты",
                    content = "Перейдите по следующей ссылке для подтверждения почты: $confirmationLink"
                )

                call.respond(
                    HttpStatusCode.Created,
                    RegisterResponse(
                        success = true,
                        message = "Пользователь зарегистрирован, проверьте почту для подтверждения"
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    RegisterResponse(success = false, message = e.message ?: "Ошибка при регистрации")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    RegisterResponse(success = false, message = "Внутренняя ошибка сервера")
                )
            }
        }


        get("/confirm-email") {
            val token = call.request.queryParameters["token"]

            if (token != null) {
                try {
                    val decodedJWT = JWT.require(Algorithm.HMAC256("mySuperSecretKey"))
                        .withIssuer("ktor-app")
                        .build()
                        .verify(token)

                    val email = decodedJWT.claims["email"]?.asString()

                    if (email != null) {
                        val user = userService.getUserByEmail(email)
                        if (user != null && !user.email_confirmed) {
                            userService.updateEmailConfirmationStatus(user.user_id, true)

                            call.respond(
                                HttpStatusCode.OK,
                                "Почта подтверждена. Вы можете войти."
                            )
                        } else {
                            call.respond(HttpStatusCode.BadRequest, "Невозможно подтвердить почту.")
                        }
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Неверный токен.")
                    }
                } catch (e: JWTVerificationException) {
                    call.respond(HttpStatusCode.BadRequest, "Неверный токен.")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "Токен не предоставлен.")
            }
        }




        // Логин и получение JWT токена
        post("/login") {
            val user = call.receive<LoginRequest>()

            if (user.mail.isBlank() || user.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Email и пароль обязательны")
                return@post
            }

            val storedUser = userService.getUserByEmail(user.mail)

            if (storedUser != null) {
                // 🔐 Проверка подтверждения email
                if (!storedUser.email_confirmed) {
                    call.respond(HttpStatusCode.Forbidden, "Подтвердите вашу почту перед входом")
                    return@post
                }

                // Проверка пароля
                if (BCrypt.checkpw(user.password, storedUser.password)) {
                    val token = createJWT(storedUser.mail, storedUser.user_id)
                    // Создаем объект ответа
                    val response = LoginResponse(
                        success = true,
                        message = "Успешный вход",
                        group_id = storedUser.group_id,  // Добавляем group_id
                        token = token,
                        id = storedUser.user_id,  // Добавляем user_id здесь
                        user= SettingsUser(storedUser.surname, storedUser.name, storedUser.patronymic, storedUser.mail, storedUser.photo.toString())
                    )
                    // Логирование ответа перед отправкой
//                    println("Sending response: ${storedUser.group_id}")  // Логирование ответа

                    // Отправляем ответ с токеном и group_id
                    call.respond(HttpStatusCode.OK, response)
                }
                else {
                    call.respond(HttpStatusCode.Unauthorized, "Неверные учетные данные")
                }
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Неверные учетные данные")
            }
        }


    }
}

fun createJWT(email: String, userId: Long): String {
    val algorithm = Algorithm.HMAC256("mySuperSecretKey")
    return JWT.create()
        .withIssuer("ktor-app")
        .withClaim("email", email)
        .withClaim("user_id", userId) // <-- добавляем user_id
        .sign(algorithm)
}
fun verifyTokenAndGetUserId(token: String): Long? {
    return try {
        val algorithm = Algorithm.HMAC256("mySuperSecretKey")
        val verifier = JWT.require(algorithm)
            .withIssuer("ktor-app")
            .build()

        val decodedJWT = verifier.verify(token)
        decodedJWT.getClaim("user_id").asLong()  // <-- Получаем user_id
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun generateConfirmationLink(email: String, id: Long): String {
    val token = createJWT(email, id)
    val host = System.getenv("APP_HOST") ?: "http://31.128.51.62:8080"
    return "$host/confirm-email?token=$token"
}


fun sendEmail(to: String, subject: String, content: String) {
    val fromEmail = "noreply.farmer_assistant@mail.ru"
    val password = "im67hQxTYp3PBxmcqHzR" // Пароль приложения или обычный (если включен)

    val props = Properties().apply {
        put("mail.smtp.host", "smtp.mail.ru")
        put("mail.smtp.port", "465")
        put("mail.smtp.auth", "true")
        put("mail.smtp.socketFactory.port", "465")
        put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
        put("mail.smtp.socketFactory.fallback", "false")
    }

    val session = Session.getInstance(props, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(fromEmail, password)
        }
    })

    try {
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(fromEmail))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            setSubject(subject) // Используй метод setSubject, а не просто присваивание
            setText(content)
        }


        Transport.send(message)
        println("Письмо отправлено на $to")
    } catch (e: MessagingException) {
        e.printStackTrace()
        println("Ошибка при отправке письма: ${e.message}")
    }
}

