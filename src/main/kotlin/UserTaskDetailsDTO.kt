import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class UserTaskDetailsDTO(
    val user_id: Long,
    val group_id: Long,
    val execution_days: String? = null,
    val start_time: String? = null, // Теперь строка
    val end_time: String? = null,   // Теперь строка
    val end_date: String? = null    // Теперь строка
)
