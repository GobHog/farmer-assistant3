import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class GroupTaskWithUsersDTO(
    val group_id: Long,
    val description: String,
    val assignedUsers: List<UserTaskDetailsDTO>,
    val token: String
)

