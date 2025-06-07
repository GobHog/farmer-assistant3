import kotlinx.serialization.Serializable

@Serializable
data class UpdateGroupRequest(
    val groupId: Long,
    val name: String,
    val photo: String? = null,
    val token: String
)


