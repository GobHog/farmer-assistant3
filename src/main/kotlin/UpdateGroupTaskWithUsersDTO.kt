import kotlinx.serialization.Serializable

@Serializable
data class UpdateGroupTaskWithUsersDTO(

    val group_id: Long,
    val description: String,
    val assignedUsers: List<UserTaskDetailsDTO>,
    val token: String
)
