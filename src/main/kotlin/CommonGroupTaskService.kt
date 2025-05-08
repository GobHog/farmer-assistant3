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

class CommonGroupTaskService(database: Database) {
    object GroupTasks : Table("Group_task") {
        val id_group_task = long("ID_Group_task").autoIncrement()
        val group_id = long("Group_ID")
        val description = text("Description")
        override val primaryKey = PrimaryKey(id_group_task, group_id)
    }

    object UserGroupTasks : Table("User_Group_task") {
        val group_id = long("Group_ID")
        val user_id = long("User_ID")
        val group_task_id = long("Group_task_ID")
        val execution_days = varchar("Execution_days", length = 100).nullable()
        val start_time = time("Start_time").nullable()
        val end_time = time("End_time").nullable()
        val end_date = date("End_date").nullable()

        override val primaryKey = PrimaryKey(group_id, user_id, group_task_id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(GroupTasks, UserGroupTasks) // Создаем таблицы
        }
    }

    // Получение задач для группы
    suspend fun getTasksByGroupId(groupId: Long): List<ExposedUserGroupTask> {
        return dbQuery {
            (GroupTasks innerJoin UserGroupTasks)
                .selectAll()
                .where { UserGroupTasks.group_id eq groupId }
                .map {
                    ExposedUserGroupTask(
                        group_id = it[GroupTasks.group_id],
                        user_id = it[UserGroupTasks.user_id],
                        group_task_id = it[UserGroupTasks.group_task_id],
                        execution_days = it[UserGroupTasks.execution_days],
                        start_time = it[UserGroupTasks.start_time],
                        end_time = it[UserGroupTasks.end_time],
                        end_date = it[UserGroupTasks.end_date]
                    )
                }
        }
    }
}
