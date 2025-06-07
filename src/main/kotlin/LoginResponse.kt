@kotlinx.serialization.Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val group_id: Long?,
    val token: String?,
    val user: SettingsUser,
    val id:Long
)

