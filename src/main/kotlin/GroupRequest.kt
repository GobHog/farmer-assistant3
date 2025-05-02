import kotlinx.serialization.Serializable

@Serializable
data class GroupRequest(
    val name: String,
    val photo: ByteArray? = null // Массив байт для хранения изображения
)

