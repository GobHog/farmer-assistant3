import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class GroupTaskWithUsersDTO(
    val group_id: Long,
    val description: String,
    val users: List<UserTaskDetailsDTO>
)
@Serializable
data class UserTaskDetailsDTO(
    val user_id: Long,
    val execution_days: String?,
    @Serializable(with = LocalDateSerializer::class)
    val end_date: LocalDate?,
    @Serializable(with = LocalTimeSerializer::class)
    val start_time: LocalTime?,
    @Serializable(with = LocalTimeSerializer::class)
    val end_time: LocalTime?
)
