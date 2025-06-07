import kotlinx.serialization.Serializable

@Serializable
data class SettingsUser(
    val surname: String,
    val name: String,
    val patronymic: String? = null,
    val mail: String,
    val photo: String? = null // base64-строка
)
