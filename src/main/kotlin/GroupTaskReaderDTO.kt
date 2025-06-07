import kotlinx.serialization.Serializable

@Serializable
data class GroupTaskReaderDTO(
    val id: Long,
    val description: String,
    val group_id: Long,
    val user_id: Long,
    val executionDays: String? = null,
    val end_date: String? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val assignedUsers: List<NameUsersForGroupTaskDTO> = emptyList()
)

