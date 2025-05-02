import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database

fun Application.configureGroupRoutes() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/backend_db",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "123",
    )
    val groupService = GroupService(database)

    routing {
        // Создание группы
        post("/create-group") {
            val groupRequest = call.receive<GroupRequest>()

            val newGroup = ExposedGroup(
                name = groupRequest.name,
                photo = groupRequest.photo // Фото
            )

            try {
                // Создаем группу
                val groupId = groupService.create(newGroup)

                // Возвращаем ответ с кодом группы (group_id)
                call.respond(HttpStatusCode.Created,  CreateGroupResponse(groupId))

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Ошибка при создании группы")
            }
        }
        get("/info-group") {
            val groupId = call.request.queryParameters["group_id"]?.toLongOrNull()

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

//        post("/join-group") {
//            val groupId = call.receive<Long>() // Получаем id группы от пользователя
//
//            try {
//                // Проверка существования группы по group_id
//                val group = groupService.getGroupById(groupId)
//
//                if (group != null) {
//                    // Присоединяем пользователя к группе
//                    val user = getCurrentUser()  // Получаем текущего пользователя
//                    userService.updateUserGroup(user.user_id, groupId)  // Обновляем запись о пользователе в базе данных
//
//                    call.respond(HttpStatusCode.OK, "Вы присоединились к группе с id: $groupId")
//                } else {
//                    call.respond(HttpStatusCode.BadRequest, "Группа с таким id не существует")
//                }
//            } catch (e: Exception) {
//                call.respond(HttpStatusCode.InternalServerError, "Ошибка при присоединении к группе")
//            }
//        }


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

fun generateConfirmationLinkForGroup(groupId: Long): String {
    val token = createJWT("group-$groupId") // Генерация токена для группы
    return "http://localhost:8080/confirm-group?token=$token" // Ссылка для подтверждения группы
}
