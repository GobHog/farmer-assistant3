import kotlinx.serialization.Serializable

@Serializable
data class OllamaResponse(
    val response: String,
    val done: Boolean = true
)


