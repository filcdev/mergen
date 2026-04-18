package hu.petrik.filcapp.auth

import hu.petrik.filcapp.api.client.APIClient
import hu.petrik.filcapp.apiBaseUrl
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

@Serializable
private data class SocialSignInRequest(val provider: String, val callbackURL: String)

@Serializable
private data class SocialSignInResponse(val url: String, val redirect: Boolean)

@Serializable
data class SessionUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val cohortId: String? = null,
    val nickname: String? = null,
    val displayName: String = "",
    val roles: List<String> = emptyList(),
    val image: String? = null,
)

@Serializable
private data class SessionResponse(val user: SessionUser? = null)

suspend fun fetchSessionUser(): SessionUser? {
    val response = APIClient.get("$apiBaseUrl/api/auth/get-session")
    if (!response.status.isSuccess()) return null
    return response.body<SessionResponse>().user
}
