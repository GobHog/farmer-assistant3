import kotlinx.serialization.Serializable

@Serializable
data class NewsArticleResponse(
    val content: String,
    val creation_date: String,
    val link: String
)
