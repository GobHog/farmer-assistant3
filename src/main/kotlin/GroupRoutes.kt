import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.example.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Application.configureGroupRoutes() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/backend_db",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "123",
    )
    val groupService = GroupService(database)
    val userService = UserService(database)
    routing {
        // Создание группы
        post("/create-group") {
            val groupRequest = call.receive<GroupRequest>()

            val photoBytes = try {
                groupRequest.photo?.let { Base64.getDecoder().decode(it) }
            } catch (e: Exception) {
                null
            }

            val newGroup = ExposedGroup(
                name = groupRequest.name,
                photo = photoBytes
            )

            try {
                // Декодируем JWT и достаём user_id
                val decodedJWT = JWT.require(Algorithm.HMAC256("mySuperSecretKey"))
                    .withIssuer("ktor-app")
                    .build()
                    .verify(groupRequest.token)

                val userId = decodedJWT.getClaim("user_id")?.asLong()
                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID пользователя не найден в токене")
                    return@post
                }

                // Создание группы
                val groupId = groupService.create(newGroup)

                // Привязка пользователя к группе
                groupService.attachUserToGroup(userId, groupId)

                call.respond(HttpStatusCode.Created, CreateGroupResponse(groupId))
            } catch (e: JWTVerificationException) {
                call.respond(HttpStatusCode.BadRequest, "Неверный токен")
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Ошибка при создании группы")
            }
        }



        get("/group/{id}") {
            val groupId = call.parameters["id"]?.toLongOrNull()

            if (groupId == null) {
                call.respond(HttpStatusCode.BadRequest, "Некорректный ID группы")
                return@get
            }

            val group = groupService.getGroupById(groupId)
            if (group != null) {
                call.respond(group)
            } else {
                call.respond(HttpStatusCode.NotFound, "Группа не найдена")
            }
        }
        get("/group/{groupId}/tasks") {
            val groupId = call.parameters["groupId"]?.toLongOrNull()

            if (groupId == null) {
                call.respond(HttpStatusCode.BadRequest, "Некорректный ID группы")
                return@get
            }

            // Создаем экземпляр CommonGroupTaskService
            val groupTaskService = CommonGroupTaskService(database)

            // Получаем задачи для группы
            val tasks = groupTaskService.getTasksByGroupId(groupId)
            call.respond(tasks)
        }


        post("/join-group") {
            val request = call.receive<GroupRequestId>()
            val groupId = request.group_id
            val token = request.jwt_token

            try {
                val decodedJWT = JWT.require(Algorithm.HMAC256("mySuperSecretKey"))
                    .withIssuer("ktor-app")
                    .build()
                    .verify(token)

                val userId = decodedJWT.getClaim("user_id")?.asLong()
                println("userId = $userId")

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Пользователь не авторизован")
                    return@post
                }

                val group = groupService.getGroupById(groupId)

                if (group != null) {
                    userService.updateUserGroup(userId, groupId)
                    call.respond(HttpStatusCode.OK, "Вы присоединились к группе с ID: $groupId")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Группа с таким ID не существует")
                }
            } catch (e: JWTVerificationException) {
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest, "Неверный токен")
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Ошибка при присоединении к группе")
            }
        }





        // Получение информации о группе по ID
//        get("/group/{id}") {
//            val groupId = call.parameters["id"]?.toLongOrNull()
//            if (groupId != null) {
//                val group = groupService.getGroupById(groupId)
//                if (group != null) {
//                    call.respond(group)
//                } else {
//                    call.respond(HttpStatusCode.NotFound, "Группа не найдена")
//                }
//            } else {
//                call.respond(HttpStatusCode.BadRequest, "Неверный ID группы")
//            }
//        }

    }
}

