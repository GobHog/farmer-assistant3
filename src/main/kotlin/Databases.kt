package com.example

import UpdateProfileRequest
import UpdateProfileResponse
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.event.*
import java.util.*

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}",
        driver = "org.postgresql.Driver",
        user = System.getenv("DB_USER"),
        password = System.getenv("DB_PASSWORD")
    )

    install(ContentNegotiation) {
        json(Json { prettyPrint = true; isLenient = true })
    }
    val userService = UserService(database)
    routing {
        // Create user

        post("/users") {
            val user = call.receive<ExposedUser>()
            val id = userService.create(user)
            call.respond(HttpStatusCode.Created, id)
        }
        
        // Read user
        get("/users/{id}") {
            val id = call.parameters["User_id"]?.toLong() ?: throw IllegalArgumentException("Invalid ID")
            val user = userService.read(id)
            if (user != null) {
                call.respond(HttpStatusCode.OK, user)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        // Get all users (new endpoint)
        get("/users") {
            val users = userService.readAll()  // Предполагаем, что такой метод есть в UserService
            if (users.isNotEmpty()) {
                call.respond(HttpStatusCode.OK, users)
            } else {
                call.respond(HttpStatusCode.NoContent) // Пустой список
            }
        }

        // Update user
        put("/users/{id}") {
            val id = call.parameters["id"]?.toLong() ?: throw IllegalArgumentException("Invalid ID")
            val user = call.receive<ExposedUser>()
            userService.update(id, user)
            call.respond(HttpStatusCode.OK)
        }
        
        // Delete user
        delete("/users/{id}") {
            val id = call.parameters["id"]?.toLong() ?: throw IllegalArgumentException("Invalid ID")
            userService.delete(id)
            call.respond(HttpStatusCode.OK)
        }
        post("/user/updateProfile") {
            val request = call.receive<UpdateProfileRequest>()

            val token = request.token
            if (token == null) {
                call.respond(HttpStatusCode.Unauthorized, UpdateProfileResponse(false, "Токен не предоставлен"))
                return@post
            }

            // Проверяем токен и извлекаем mail
            val userMail = extractMailFromToken(token)
            if (userMail == null) {
                call.respond(HttpStatusCode.Unauthorized, UpdateProfileResponse(false, "Невалидный токен"))
                return@post
            }

            // Безопасно декодируем фото
            val decodedPhoto: ByteArray? = request.photo?.takeIf { it.isNotBlank() }?.let {
                try {
                    Base64.getDecoder().decode(it)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, UpdateProfileResponse(false, "Некорректное фото"))
                    return@post
                }
            }

            transaction {
                UserService.Users.update({ UserService.Users.mail eq userMail }) {
                    it[name] = request.name
                    it[surname] = request.surname
                    it[patronymic] = request.patronymic
                    it[photo] = decodedPhoto
                }
            }

            call.respond(HttpStatusCode.OK, UpdateProfileResponse(true, "Профиль обновлён"))
        }

    }
}
fun extractMailFromToken(token: String): String? {
    // Здесь мы используем библиотеку для декодирования и проверки JWT
    // Это может быть JWT, созданный с использованием JWT-верификатора, например, с использованием `jwt-ktor` или `auth-jwt` библиотеки

    try {
        val jwt = JWT.decode(token)
        return jwt.getClaim("email").asString()  // Извлекаем email из токена
    } catch (e: Exception) {
        return null
    }
}