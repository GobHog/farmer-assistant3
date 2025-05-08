import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

@Serializable
data class ExposedGroupTask(
    val group_id: Long,
    val description: String
)

class GroupTaskService(database: Database) {
    object GroupTasks : Table("Group_task") {
        val id_group_task = long("ID_Group_task").autoIncrement()
        val group_id = long("Group_ID")
        val description = text("Description")
        override val primaryKey = PrimaryKey(id_group_task, group_id)
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
            }

            // После вставки извлекаем ID вставленной записи
            statement[GroupTasks.id_group_task]
        }
    }
    suspend fun read(id: Long): ExposedGroupTask? {
        return dbQuery {
            GroupTasks.selectAll()
                .where { GroupTasks.group_id eq id }
                .map {
                    ExposedGroupTask(
                        description = it[GroupTasks.description],
                        group_id = it[GroupTasks.group_id]
                    )
                }
                .singleOrNull()
        }
    }
}