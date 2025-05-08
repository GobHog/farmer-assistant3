import kotlinx.serialization.Serializable

@Serializable
data class GroupRequestId(
    val jwt_token: String,
    val group_id: Long
)