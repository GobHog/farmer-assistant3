import kotlinx.serialization.Serializable

@Serializable
data class UpdateTaskResponse(
    val userId: Long,
    val groupTaskId: Long
)
