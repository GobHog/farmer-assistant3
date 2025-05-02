import kotlinx.serialization.Serializable

@Serializable
data class UserFull(
    val user_id: Long,
    val surname: String,
    val name: String,
    val patronymic: String?,
    val mail: String,
    val password: String,
    val photo: ByteArray?,
    val group_id: Long?,
    val role_id:Long?,
    val email_confirmed:Boolean,
)
