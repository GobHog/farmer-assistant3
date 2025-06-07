import com.example.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedRole(
    val name: String,
    val team_task_managment: Boolean
)


class RoleService(database: Database) {
    object Roles : IdTable<Long>("\"Role\"") {
        override val id = long("ID_Role").autoIncrement().entityId()
        val name = varchar("Name", length = 100)
        val team_task_managment = bool("Team_task_management")
        override val primaryKey = PrimaryKey(id)  // ← ОБЯЗАТЕЛЬНО override
    }

    init {
        transaction(database) {
            SchemaUtils.create(Roles)
        }
    }

    suspend fun create(role: ExposedRole): Long {
        return dbQuery {
            Roles.insert {
                it[name] = role.name
                it[team_task_managment] = role.team_task_managment
            }[Roles.id].value
        }
    }
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}