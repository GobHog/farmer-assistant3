import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val surname: String,
    val name: String,
    val patronymic: String?,
    val photo: String?, // фото в формате Base64
    val token: String?
)
