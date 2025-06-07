@kotlinx.serialization.Serializable
data class CreateTaskResponse(
    val message: String,
    val user_id: Long,
    val group_task_id: Long
)

