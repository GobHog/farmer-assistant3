import kotlinx.serialization.Serializable

@Serializable
data class NameUsersForGroupTaskDTO(
    val id: Long,
    val surname: String,
    val name: String,
    val patronymic: String?=null
)
