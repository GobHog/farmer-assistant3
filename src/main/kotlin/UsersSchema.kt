package com.example

import RegistrationRequest
import UserFull
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import sun.security.util.Password

@Serializable
data class ExposedUser(
        val surname: String,
        val name: String,
        val patronymic: String?,
        val mail: String,
        val password: String,
        val photo: ByteArray?,
        val group_id: Long?,
        val role_id:Long?,
        val email_confirmed:Boolean,
)


class UserService(database: Database) {
    object Users : Table("User") {
        val user_id = long("User_id").autoIncrement()  // изменено на long для bigint
        val surname = varchar("Surname", length = 50)
        val name = varchar("Name", length = 50)
        val patronymic = varchar("Patronymic", length = 50).nullable()
        val mail = varchar("Mail", length = 100).uniqueIndex()
        val password = text("Password")
        val photo = binary("Photo").nullable()  // photo может быть NULL
        val group_id = long("Group_id").nullable()  // group_id может быть NULL
        val role_id = long("Role_id").nullable()  // role_id может быть NULL
        val email_confirmed=bool("Email_confirmed")
        override val primaryKey = PrimaryKey(user_id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun create(user: ExposedUser): Long {
        // Проверяем, существует ли пользователь с таким email
        val existingUser = getUserByEmail(user.mail)
        if (existingUser != null) {
            // Если такой пользователь уже существует, можно выбросить исключение или вернуть ошибку
            throw IllegalArgumentException("Почта уже используется")
        }

        // Если email уникален, создаём пользователя
        return dbQuery {
            Users.insert {
                it[surname] = user.surname
                it[name] = user.name
                it[patronymic] = user.patronymic
                it[mail] = user.mail
                it[password] = user.password
                it[photo] = user.photo
                it[group_id] = user.group_id
                it[role_id] = user.role_id
                it[email_confirmed]=user.email_confirmed
            }[Users.user_id]
        }
    }


    suspend fun read(id: Long): ExposedUser? {
        return dbQuery {
            Users.selectAll()
                .where { Users.user_id eq id }
                .map { ExposedUser(it[Users.surname], it[Users.name],
                    it[Users.patronymic],  it[Users.mail], it[Users.password],
                    it[Users.photo], it[Users.group_id], it[Users.role_id], it[Users.email_confirmed]) }
                .singleOrNull()
        }
    }
    suspend fun readAll(): List<ExposedUser> {
        return dbQuery {
            Users.selectAll()
                .map { ExposedUser(it[Users.surname], it[Users.name],
                    it[Users.patronymic],  it[Users.mail], it[Users.password],
                    it[Users.photo], it[Users.group_id], it[Users.role_id], it[Users.email_confirmed])
                }
        }

    }
    suspend fun getUserByEmail(mail: String): UserFull? {
        return dbQuery {
            val result = Users.selectAll().where { Users.mail eq mail }.singleOrNull()
//            val result = Users.selectAll().firstOrNull { it[Users.mail] == mail }
            result?.let {
                UserFull(
                    user_id = it[Users.user_id],
                    surname = it[Users.surname],
                    name = it[Users.name],
                    patronymic = it[Users.patronymic],
                    mail = it[Users.mail],
                    password = it[Users.password],
                    photo = it[Users.photo],
                    group_id = it[Users.group_id],
                    role_id = it[Users.role_id],
                    email_confirmed = it[Users.email_confirmed]
                )
            }
        }
    }

    suspend fun update(id: Long, user: ExposedUser) {
        dbQuery {
            Users.update({ Users.user_id eq id }) {
                it[surname] = user.surname
                it[name] = user.name
                it[patronymic] = user.patronymic
                it[mail] = user.mail
                it[password] = user.password
                it[photo] = user.photo
                it[group_id] = user.group_id
                it[role_id] = user.role_id
            }
        }
    }
    suspend fun updateEmailConfirmationStatus(userId: Long, confirmed: Boolean) {
        dbQuery {
            Users.update({ Users.user_id eq userId }) {
                it[email_confirmed] = confirmed
            }
        }
    }

    suspend fun delete(id: Long) {
        dbQuery {
            Users.deleteWhere { Users.user_id.eq(id) }
        }
    }


    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

