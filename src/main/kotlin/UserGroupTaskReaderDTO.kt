import kotlinx.serialization.Serializable

@Serializable
data class UserGroupTaskReaderDTO(
    val id: Long,
    val description: String,
    val group_id: Long,
    val executionDays: String?,
    val end_date: String?,
    val start_time: String?,
    val end_time: String?

)
