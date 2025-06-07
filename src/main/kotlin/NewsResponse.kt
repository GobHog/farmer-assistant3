import kotlinx.serialization.Serializable

@Serializable
data class NewsPageResponse(
    val news: List<NewsArticleResponse>,
    val summary: String
)
