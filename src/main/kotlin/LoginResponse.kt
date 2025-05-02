@kotlinx.serialization.Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String
)

