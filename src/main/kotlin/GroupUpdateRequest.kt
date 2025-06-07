import kotlinx.serialization.Serializable

@Serializable
data class GroupUpdateRequest(
    val groupId: Long,
    val name: String,
    val photo: String? = null, // base64
    val token: String
)
