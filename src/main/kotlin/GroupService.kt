import com.example.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction


@Serializable
data class ExposedGroup(
    val name: String,
    @Serializable(with = ByteArrayBase64Serializer::class)
    val photo: ByteArray? = null
)


class GroupService(database: Database) {
    object Groups : IdTable<Long>("\"Group\"") {
        override val id = long("ID_Group").autoIncrement().entityId()
        val name = varchar("Name", length = 100)
        val photo = binary("Photo").nullable()

        override val primaryKey = PrimaryKey(id)  // ← ОБЯЗАТЕЛЬНО override
    }


    init {
        transaction(database) {
            SchemaUtils.create(Groups)
        }
    }

    suspend fun create(group: ExposedGroup): Long {
        return dbQuery {
            Groups.insert {
                it[name] = group.name
                if (group.photo != null) {
                    it[photo] = group.photo
                }
            }[Groups.id].value
        }
    }
    suspend fun attachUserToGroup(userId: Long, groupId: Long) {
        dbQuery {
            UserService.Users.update({ UserService.Users.id eq userId }) {
                it[UserService.Users.group_id] = groupId
            }
        }
    }


    suspend fun read(id: Long): ExposedGroup? {
        return dbQuery {
            Groups.selectAll()
                .where { Groups.id eq id }
                .map {
                    ExposedGroup(
                        name = it[Groups.name],
                        photo = it[Groups.photo]

                    )
                }
                .singleOrNull()
        }
    }

    suspend fun readAll(): List<ExposedGroup> {
        return dbQuery {
            Groups.selectAll().map {
                ExposedGroup(
                    name = it[Groups.name],
                    photo = it[Groups.photo]

                )
            }
        }
    }
    suspend fun getGroupById(id: Long): ExposedGroup? {
        return dbQuery {
            val result =Groups.selectAll().where { Groups.id eq id }.singleOrNull()
            result?.let {
                ExposedGroup(
                    name = it[Groups.name],
                    photo = it[Groups.photo]

                )
            }
        }
    }
    suspend fun groupExists(groupId: Long): Boolean = dbQuery {
        Groups.selectAll().where { Groups.id eq groupId }.count() > 0
    }
    suspend fun update(groupId: Long, updatedGroup: ExposedGroup) {
        dbQuery {
            Groups.update({ Groups.id eq groupId }) {
                it[name] = updatedGroup.name
                it[photo] = updatedGroup.photo
            }
        }
    }
}