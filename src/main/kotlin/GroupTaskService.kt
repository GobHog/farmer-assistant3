
import com.example.UserService
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedGroupTask(
    val id_group_task: Long? = null,  // Auto-generated ID
    val group_id: Long,
    val description: String,
    val user_id:Long
)

class GroupTaskService(database: Database) {
    object GroupTasks : IdTable<Long>("\"Group_task\"") {
        override val id = long("ID_Group_task").autoIncrement().entityId()
        val group_id = long("Group_ID").references(GroupService.Groups.id,
            onDelete = ReferenceOption.CASCADE,
            onUpdate = ReferenceOption.CASCADE)
        val description = text("Description")
        val user_id = long("User_ID").references(UserService.Users.id,
            onDelete = ReferenceOption.CASCADE,
            onUpdate = ReferenceOption.CASCADE)
        override val primaryKey = PrimaryKey(id) // или просто rely on IdTable primaryKey
    }


    init {
        transaction(database) {
            SchemaUtils.create(GroupTasks)
        }
    }

    suspend fun create(groupTask: ExposedGroupTask): Long {
        return dbQuery {
            // Вставка задачи
            val statement = GroupTasks.insert {
                it[group_id] = groupTask.group_id
                it[description] = groupTask.description
                it[user_id]=groupTask.user_id
            }

            // После вставки извлекаем ID вставленной записи
            statement[GroupTasks.id].value
        }
    }
    suspend fun read(id: Long): ExposedGroupTask? {
        return dbQuery {
            GroupTasks.selectAll()
                .where { GroupTasks.id eq id }
                .map {
                    ExposedGroupTask(
                        description = it[GroupTasks.description],
                        group_id = it[GroupTasks.group_id],
                        user_id = it[GroupTasks.user_id]
                    )
                }
                .singleOrNull()
        }
    }
    suspend fun readAllByGroup(groupId: Long): List<ExposedGroupTask> {
        return dbQuery {
            GroupTasks.selectAll().where { GroupTasks.group_id eq groupId }
                .map {
                    ExposedGroupTask(
                        id_group_task = it[GroupTasks.id].value,
                        group_id = it[GroupTasks.group_id],
                        description = it[GroupTasks.description],
                        user_id = it[GroupTasks.user_id]
                    )
                }
        }
    }
    fun getTasksByGroupId(groupId: Long): List<GroupTaskReaderDTO> {
        return transaction {
            (GroupTasks innerJoin UserGroupTaskService.UserGroupTasks)
                .selectAll().where { GroupTasks.group_id eq groupId and (GroupTasks.id eq UserGroupTaskService.UserGroupTasks.group_task_id) }
                .map {
                    GroupTaskReaderDTO(
                        id = it[GroupTasks.id].value,
                        description = it[GroupTasks.description],
                        group_id = it[GroupTasks.group_id],
                        user_id = it[GroupTasks.user_id],
                        executionDays = it[UserGroupTaskService.UserGroupTasks.execution_days],
                        end_date = it[UserGroupTaskService.UserGroupTasks.end_date]?.toString(),
                        start_time = it[UserGroupTaskService.UserGroupTasks.start_time]?.toString(),
                        end_time = it[UserGroupTaskService.UserGroupTasks.end_time]?.toString()
                    )
                }
        }
    }

    suspend fun getTasksWithUsersByGroupId(groupId: Long): List<GroupTaskReaderDTO> {
        val tasks = getTasksByGroupId(groupId) // получаем базовые задачи

        return tasks.map { task ->
            val assignedUsers = getUsersByGroupTaskId(task.id) // получаем пользователей
            task.copy(assignedUsers = assignedUsers)
        }
    }
    fun getUsersByGroupTaskId(groupTaskId: Long): List<NameUsersForGroupTaskDTO> {
        return transaction {
            UserGroupTaskService.UserGroupTasks
                .join(UserService.Users, JoinType.INNER) { UserGroupTaskService.UserGroupTasks.user_id eq UserService.Users.id }
                .selectAll().where { UserGroupTaskService.UserGroupTasks.group_task_id eq groupTaskId }
                .map {
                    NameUsersForGroupTaskDTO(
                        id = it[UserService.Users.id].value,
                        surname = it[UserService.Users.surname],
                        name = it[UserService.Users.name],
                        patronymic = it[UserService.Users.patronymic]
                    )
                }
        }
    }
    suspend fun deleteGroupTaskById(id: Long) {
        dbQuery {
            GroupTasks.deleteWhere { GroupTasks.id eq id }
        }
    }
    suspend fun update(groupTask: ExposedGroupTask) {
        dbQuery {
            GroupTasks.update({ GroupTasks.id eq groupTask.id_group_task!! }) {
                it[group_id] = groupTask.group_id
                it[description] = groupTask.description
                it[user_id] = groupTask.user_id
            }
        }
    }
    suspend fun deleteByGroupTaskId(groupTaskId: Long) {
        dbQuery {
            UserGroupTaskService.UserGroupTasks.deleteWhere { UserGroupTaskService.UserGroupTasks.group_task_id eq groupTaskId }
        }
    }

}

