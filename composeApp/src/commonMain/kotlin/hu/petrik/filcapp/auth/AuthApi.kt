package hu.petrik.filcapp.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Better Auth endpoints. These are not part of the generated OpenAPI client: Better Auth owns
 * the whole `/api/auth` path and is consumed directly.
 */
internal class AuthApi(private val http: HttpClient, private val json: Json = AuthJson) {
    /**
     * Exchanges an Entra ID token for a Chronos session. Better Auth creates the session cookie
     * (and the user, on first sign-in); our cookie jar stores it.
     */
    suspend fun signInWithMicrosoft(idToken: String): AuthUser {
        val response =
            http.post("${AuthConfig.AUTH_BASE_URL}/sign-in/social") {
                contentType(ContentType.Application.Json)
                setBody(
                    SignInSocialRequest(
                        provider = MICROSOFT_PROVIDER,
                        idToken = IdTokenPayload(token = idToken),
                    ),
                )
            }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw response.toAuthException(body)
        }
        val user =
            runCatching { json.decodeFromString<SignInSocialResponse>(body) }.getOrNull()?.user
                ?: throw AuthException("Chronos accepted the sign-in but returned no user.")
        return user
    }

    /** Current session, or `null` when the stored cookie jar has expired. */
    suspend fun getSession(): AuthUser? {
        val response = http.get("${AuthConfig.AUTH_BASE_URL}/get-session")
        if (!response.status.isSuccess()) {
            return null
        }
        val body = response.bodyAsText()
        if (body.isBlank() || body == "null") {
            return null
        }
        return runCatching { json.decodeFromString<GetSessionResponse>(body) }.getOrNull()?.user
    }

    suspend fun signOut() {
        http.post("${AuthConfig.AUTH_BASE_URL}/sign-out")
    }

    private suspend fun HttpResponse.toAuthException(body: String): AuthException {
        val parsed = runCatching { json.decodeFromString<AuthErrorBody>(body) }.getOrNull()
        val message =
            parsed?.message?.takeIf { it.isNotBlank() }
                ?: "Chronos rejected the sign-in (HTTP ${status.value})."
        return AuthException(message, code = parsed?.code)
    }

    private companion object {
        const val MICROSOFT_PROVIDER = "microsoft"
    }
}

@Serializable
private data class SignInSocialRequest(val provider: String, val idToken: IdTokenPayload)

@Serializable
private data class IdTokenPayload(val token: String)

@Serializable
private data class SignInSocialResponse(val user: AuthUser? = null, val token: String? = null)

@Serializable
private data class GetSessionResponse(val user: AuthUser, val session: SessionPayload? = null)

@Serializable
private data class SessionPayload(
    val token: String? = null,
    @SerialName("expiresAt") val expiresAt: String? = null,
)

@Serializable
private data class AuthErrorBody(val message: String? = null, val code: String? = null)
