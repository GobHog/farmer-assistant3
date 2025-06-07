@kotlinx.serialization.Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean
)

