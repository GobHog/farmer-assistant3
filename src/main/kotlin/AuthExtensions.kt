import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun ApplicationCall.getUserIdFromToken(): Long? {
    val principal = this.principal<JWTPrincipal>()
    return principal?.payload?.getClaim("user_id")?.asLong()
}