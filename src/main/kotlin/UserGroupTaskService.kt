import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.time
import java.time.LocalTime


@Serializable
data class ExposedUserGroupTask(
    val group_id: Long,
    val user_id: Long,
    val group_task_id: Long,
    val execution_days: String?,
    @Serializable(with = LocalDateSerializer::class)
    val end_date: LocalDate?,
    @Serializable(with = LocalTimeSerializer::class)
    val start_time: LocalTime?,
    @Serializable(with = LocalTimeSerializer::class)
    val end_time: LocalTime?
)

class UserGroupTaskService(database: Database) {
    object UserGroupTasks : Table("User_Group_task") {
        val group_id = long("Group_ID")
        val user_id = long("User_ID")
        val group_task_id = long("Group_task_ID")
        val execution_days = varchar("Execution_days", length = 100).nullable()
        val start_time = time("Start_time").nullable()
        val end_time = time("End_time").nullable()
        val end_date = date("End_date").nullable()  // <-- Исправлено имя поля

        override val primaryKey = PrimaryKey(group_id, user_id, group_task_id)
    }
    object GroupTasks : Table("Group_task") {
        val id_group_task = long("ID_Group_task").autoIncrement()
        val group_id = long("Group_ID")
        val description = text("Description")
        override val primaryKey = PrimaryKey(id_group_task, group_id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(UserGroupTasks)
        }
    }

    suspend fun create(userGroupTask: ExposedUserGroupTask, groupTaskId: Long): Unit = dbQuery {
        UserGroupTasks.insert {
            it[group_id] = userGroupTask.group_id
            it[user_id] = userGroupTask.user_id
            it[group_task_id] = groupTaskId
            it[execution_days] = userGroupTask.execution_days
            it[start_time] = userGroupTask.start_time
            it[end_time] = userGroupTask.end_time
            it[end_date] = userGroupTask.end_date
        }
    }
    suspend fun read(id: Long): ExposedUserGroupTask? {
        return dbQuery {
            UserGroupTasks.selectAll()
                .where { UserGroupTasks.group_id eq id }
                .map {
                    ExposedUserGroupTask(
                        group_id = it[UserGroupTasks.group_id],
                        user_id = it[UserGroupTasks.user_id],
                        end_date = it[UserGroupTasks.end_date],
                        execution_days = it[UserGroupTasks.execution_days],
                        end_time = it[UserGroupTasks.end_time],
                        start_time = it[UserGroupTasks.start_time],
                        group_task_id = it[UserGroupTasks.group_task_id]
                    )
                }
                .singleOrNull()
        }
    }
}
