import kotlinx.serialization.Serializable

@Serializable
data class UserGroupTaskDTO(
    val userId: Long,
    val groupId: Long,
    val groupTaskId: Long,
    val executionDays: String?,
    val startTime: String?, // Format: "HH:mm:ss"
    val endTime: String?,   // Format: "HH:mm:ss"
    val endDate: String?    // Format: "yyyy-MM-dd"
)
