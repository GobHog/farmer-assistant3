import kotlinx.serialization.Serializable

@Serializable
data class GroupRequest(
    val token: String,
    val name: String,
    val photo: String? = null // Base64 строка
)


