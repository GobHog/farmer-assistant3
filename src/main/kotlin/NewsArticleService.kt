import com.example.UserService
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SchemaUtils.withDataBaseLock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDate

@Serializable
data class ExposedNewsArticle(
    val content: String,
    val creation_date: String,
    val link: String
)

class NewsArticleService(database: Database) {
    object NewsArticles : IdTable<Long>("\"News_article\"") {
        override val id = long("ID_News_article").autoIncrement().entityId() // auto-generated ID
        val content = text("Content")
        val creation_date = date("Creation_date")
        val link = text("Link")
        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            // Создаем таблицу, если она еще не существует
            SchemaUtils.create(NewsArticles)
        }
    }

    // Добавление статьи и возврат ID
    suspend fun create(newsArticle: ExposedNewsArticle): Long {
        return dbQuery {
            // Вставляем новую статью и получаем auto-generated ID
            NewsArticles.insert {
                it[content] = newsArticle.content
                it[creation_date] = LocalDate.parse(newsArticle.creation_date)
                it[link] = newsArticle.link
            }[NewsArticles.id].value
        }
    }


    // Получение статьи по ID
    suspend fun read(id: Long): ExposedNewsArticle? {
        return transaction {
            NewsArticles.selectAll()
                .where { NewsArticles.id eq id }
                .map {
                    ExposedNewsArticle(
                        content = it[NewsArticles.content],
                        creation_date = it[NewsArticles.creation_date].toString(),
                        link = it[NewsArticles.link]
                    )
                }
                .singleOrNull()
        }
    }

    // Получение всех статей
    suspend fun readAll(): List<ExposedNewsArticle> {
        return transaction {
            val result = NewsArticles.selectAll()
                .map {
                    val date = it[NewsArticles.creation_date]
                    ExposedNewsArticle(
                        content = it[NewsArticles.content],
                        creation_date = date.toString(),
                        link = it[NewsArticles.link]
                    )
                }

            // Вывод даты первой записи, если она есть
            result.firstOrNull()?.let {
                println("👉 Первая дата из БД: ${it.creation_date}")
            }

            result
        }
    }
    // Получение последних 5 новостей по дате
    suspend fun readLatest5(): List<ExposedNewsArticle> {
        return transaction {
            NewsArticles
                .selectAll()
                .orderBy(NewsArticles.creation_date, SortOrder.DESC)
                .limit(5)
                .map {
                    ExposedNewsArticle(
                        content = it[NewsArticles.content],
                        creation_date = it[NewsArticles.creation_date].toString(),
                        link = it[NewsArticles.link]
                    )
                }
        }
    }

    suspend fun existsByLink(link: String): Boolean {
        return dbQuery {
            NewsArticleService.NewsArticles
                .selectAll().where { NewsArticleService.NewsArticles.link eq link }
                .count() > 0
        }
    }
}