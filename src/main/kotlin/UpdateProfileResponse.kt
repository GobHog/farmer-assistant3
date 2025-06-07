import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileResponse(
    val success: Boolean,
    val message: String
)
