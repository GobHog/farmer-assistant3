@kotlinx.serialization.Serializable
data class MemberForGroupResponse(
    val ID_User: Long,
    val Surname: String,
    val Name: String,
    val Patronymic: String?  // Разрешить null
)

