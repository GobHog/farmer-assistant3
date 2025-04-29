import kotlinx.serialization.Serializable

// Модель для регистрации пользователя
@Serializable
data class RegistrationRequest(
    val surname: String,
    val name: String,
    val mail: String,
    val password: String // Не захешированный пароль
)
