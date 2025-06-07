import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.example.UserService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun Application.configureGroupTaskRoutes() {
    val database = Database.connect(
        url = "jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}",
        driver = "org.postgresql.Driver",
        user = System.getenv("DB_USER"),
        password = System.getenv("DB_PASSWORD")
    )

    val groupTaskService = GroupTaskService(database)
    val userGroupTaskService = UserGroupTaskService(database)
    val groupService=GroupService(database)
    val  userService=UserService(database)
    routing {
        post("/group-task/full-create") {
            try {
                val rawBody = call.receiveText()
                val request = Json.decodeFromString<GroupTaskWithUsersDTO>(rawBody)
                // 🔐 Валидация токена
                val userIdFromToken = verifyTokenAndGetUserId(request.token)
                if (userIdFromToken == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Неверный или просроченный токен")
                    return@post
                }

                if (!groupService.groupExists(request.group_id)) {
                    call.respond(HttpStatusCode.BadRequest, "Группа с ID ${request.group_id} не существует")
                    return@post
                }

                // 👇 Можно проверить, принадлежит ли пользователь группе (опционально)
//                if (!groupService.isUserInGroup(userIdFromToken, request.group_id)) {
//                    call.respond(HttpStatusCode.Forbidden, "Вы не состоите в этой группе")
//                    return@post
//                }

                val groupTaskId = groupTaskService.create(
                    ExposedGroupTask(
                        group_id = request.group_id,
                        description = request.description,
                        user_id = userIdFromToken
                    )
                )

                request.assignedUsers.forEach { user ->
                    try {
                        userGroupTaskService.create(
                            ExposedUserGroupTask(
                                group_id = request.group_id,
                                user_id = user.user_id,
                                group_task_id = groupTaskId,
                                execution_days = user.execution_days,
                                start_time = stringToLocalTime(user.start_time),
                                end_time = stringToLocalTime(user.end_time),
                                end_date = stringToLocalDate(user.end_date)
                            ),
                            groupTaskId
                        )
                    }
                    catch (userError: Exception) {
                        println("Ошибка создания задания для пользователя ${user.user_id}: ${userError.message}")
                        userError.printStackTrace()
                    }
                }

                call.respond(
                    status = HttpStatusCode.Created,
                    message = CreateTaskResponse(
                        message = "Group task with users created",
                        user_id = userIdFromToken,
                        group_task_id = groupTaskId
                    )
                )

            } catch (e: Exception) {
                println("Ошибка при обработке запроса: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Ошибка при создании задачи")
            }
        }

        get("/group-task/{groupId}") {
            val groupIdParam = call.parameters["groupId"]
            val groupId = groupIdParam?.toLongOrNull()

            if (groupId == null) {
                call.respond(HttpStatusCode.BadRequest, "Некорректный ID группы")
                return@get
            }

            if (!groupService.groupExists(groupId)) {
                call.respond(HttpStatusCode.NotFound, "Группа с ID $groupId не найдена")
                return@get
            }

            val tasks = groupTaskService.readAllByGroup(groupId)
            call.respond(HttpStatusCode.OK, tasks)
        }
        get("/group-task/by-group/{groupId}") {
            val groupId = call.parameters["groupId"]?.toLongOrNull()
            if (groupId == null) {
                call.respond(HttpStatusCode.BadRequest, "Некорректный ID группы")
                return@get
            }

            val tasks = groupTaskService.getTasksWithUsersByGroupId(groupId) // ✅ вызываем верный метод
            call.respond(tasks)
        }
        delete("/delete-group-task/{id}") {
            val taskId = call.parameters["id"]?.toLongOrNull()
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")

            if (taskId == null || token.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing taskId or token")
                return@delete
            }

            try {
                val algorithm = Algorithm.HMAC256("mySuperSecretKey")
                val verifier = JWT.require(algorithm)
                    .withIssuer("ktor-app")
                    .build()

                val decodedJWT = verifier.verify(token)
                val email = decodedJWT.getClaim("email").asString()
                val userId = decodedJWT.getClaim("user_id").asLong()

                if (email == null || userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@delete
                }

                // 🔎 Проверяем, существует ли пользователь
                val user = userService.getUserByEmail(email) // реализуй userRepository сам
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                    return@delete
                }

                // 🗑 Удаляем задачу
                userGroupTaskService.deleteUserGroupTaskById(taskId)
                groupTaskService.deleteGroupTaskById(taskId)



                call.respond(HttpStatusCode.OK, "Task deleted")

            } catch (e: JWTVerificationException) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid or expired token")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Something went wrong")
            }
        }
        put("/group-task/update/{id}") {
            val taskId = call.parameters["id"]?.toLongOrNull()

            if (taskId == null) {
                call.respond(HttpStatusCode.BadRequest, "Некорректный ID задачи")
                return@put
            }

            val rawBody = call.receiveText()
            val request = try {
                Json.decodeFromString<UpdateGroupTaskWithUsersDTO>(rawBody)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Неверный формат запроса")
                return@put
            }

            val userIdFromToken = verifyTokenAndGetUserId(request.token)
            if (userIdFromToken == null) {
                call.respond(HttpStatusCode.Unauthorized, "Неверный или просроченный токен")
                return@put
            }

            val existingTask = groupTaskService.read(taskId)
            if (existingTask == null) {
                call.respond(HttpStatusCode.NotFound, "Задача с ID $taskId не найдена")
                return@put
            }

            if (!groupService.groupExists(request.group_id)) {
                call.respond(HttpStatusCode.BadRequest, "Группа с ID ${request.group_id} не существует")
                return@put
            }

            try {
                groupTaskService.update(
                    ExposedGroupTask(
                        id_group_task = taskId,
                        group_id = request.group_id,
                        description = request.description,
                        user_id = userIdFromToken // можно обновить или оставить прежним
                    )
                )

                userGroupTaskService.deleteUserGroupTaskById(taskId)

                request.assignedUsers.forEach { user ->
                    userGroupTaskService.create(
                        ExposedUserGroupTask(
                            group_id = request.group_id,
                            user_id = user.user_id,
                            group_task_id = taskId,
                            execution_days = user.execution_days,
                            start_time = stringToLocalTime(user.start_time),
                            end_time = stringToLocalTime(user.end_time),
                            end_date = stringToLocalDate(user.end_date)
                        ),
                        taskId
                    )
                }

                call.respond(
                    HttpStatusCode.OK,
                    UpdateTaskResponse(
                        userId = userIdFromToken,
                        groupTaskId = taskId
                    )
                )

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Ошибка при обновлении задачи: ${e.message}")
            }
        }


    }
}
fun stringToLocalTime(timeString: String?): LocalTime? {
    return if (!timeString.isNullOrBlank()) {
        LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        null
    }
}
fun stringToLocalDate(dateString: String?): LocalDate? {
    return if (!dateString.isNullOrBlank()) {
        LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
    } else {
        null
    }
}


