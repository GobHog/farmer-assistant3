import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import java.time.LocalDate
import java.time.LocalTime

fun Application.configureGroupTaskRoutes() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/backend_db",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "123",
    )
    val groupTaskService = GroupTaskService(database)
    val userGroupTaskService = UserGroupTaskService(database)
    routing {
        post("/group-task/full-create") {
            val request = call.receive<GroupTaskWithUsersDTO>()

            try {
                val groupTaskId = groupTaskService.create(
                    ExposedGroupTask(
                        group_id = request.group_id,
                        description = request.description
                    )
                )

                request.users.forEach { user ->
                    userGroupTaskService.create(
                        ExposedUserGroupTask(
                            group_id = request.group_id,
                            user_id = user.user_id,
                            group_task_id = groupTaskId,
                            execution_days = user.execution_days,
                            start_time = user.start_time,  // ← уже LocalTime?
                            end_time = user.end_time,
                            end_date = user.end_date
                        ),
                        groupTaskId
                    )
                }
                call.respond(HttpStatusCode.Created, "Group task with users created")
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Ошибка при создании задачи")
            }
        }
    }
}