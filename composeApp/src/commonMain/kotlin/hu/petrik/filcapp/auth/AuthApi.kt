package hu.petrik.filcapp.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Better Auth endpoints. These are not part of the generated OpenAPI client: Better Auth owns
 * the whole `/api/auth` path and is consumed directly.
 */
internal class AuthApi(
    private val http: HttpClient,
    private val json: Json = AuthJson,
) {
    /**
     * Exchanges an Entra ID token for a Chronos session. Better Auth creates the session cookie
     * (and the user, on first sign-in); our cookie jar stores it.
     *
     * For an interactive OIDC flow the nonce is forwarded too, so Chronos can bind the ID token
     * to the authorization request. Refresh-token sign-ins have no new authorization nonce.
     */
    suspend fun signInWithMicrosoft(proof: IdTokenProof): AuthUser {
        val response =
            http.post("${AuthConfig.AUTH_BASE_URL}/sign-in/social") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Origin, AuthConfig.TRUSTED_ORIGIN)
                setBody(
                    SignInSocialRequest(
                        provider = MICROSOFT_PROVIDER,
                        idToken =
                            IdTokenPayload(
                                token = proof.token,
                                nonce = proof.nonce,
                            ),
                    ),
                )
            }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw response.toAuthException(body)
        }
        return runCatching {
            json.decodeFromString<SignInSocialResponse>(body)
        }.getOrNull()?.user
            ?: throw AuthException("Chronos accepted the sign-in but returned no user.")
    }

    /** Current session, or `null` when the stored cookie jar has expired. */
    suspend fun getSession(): AuthUser? {
        val response =
            http.get("${AuthConfig.AUTH_BASE_URL}/get-session") {
                header(HttpHeaders.Origin, AuthConfig.TRUSTED_ORIGIN)
            }
        if (!response.status.isSuccess()) {
            return null
        }
        val body = response.bodyAsText()
        if (body.isBlank() || body == "null") {
            return null
        }
        return runCatching { json.decodeFromString<GetSessionResponse>(body) }
            .getOrNull()
            ?.user
    }

    /**
     * Better Auth owns nickname/cohort updates because those are configured as input-enabled
     * additional user fields in Chronos.
     */
    suspend fun updateUser(
        nickname: String?,
        cohortId: String?,
    ): AuthUser {
        val response =
            http.post("${AuthConfig.AUTH_BASE_URL}/update-user") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Origin, AuthConfig.TRUSTED_ORIGIN)
                setBody(UpdateUserRequest(nickname = nickname, cohortId = cohortId))
            }

        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw response.toAuthException(body)
        }

        return getSession()
            ?: throw AuthException("The account was updated, but the Chronos session could not be restored.")
    }

    suspend fun signOut() {
        http.post("${AuthConfig.AUTH_BASE_URL}/sign-out") {
            header(HttpHeaders.Origin, AuthConfig.TRUSTED_ORIGIN)
        }
    }

    private fun HttpResponse.toAuthException(body: String): AuthException {
        val parsed = runCatching { json.decodeFromString<AuthErrorBody>(body) }.getOrNull()
        val message =
            parsed?.message?.takeIf { it.isNotBlank() }
                ?: "Chronos rejected the request (HTTP ${status.value})."
        return AuthException(message, code = parsed?.code)
    }

    private companion object {
        const val MICROSOFT_PROVIDER = "microsoft"
    }
}

@Serializable
private data class SignInSocialRequest(
    val provider: String,
    val idToken: IdTokenPayload,
)

@Serializable
private data class IdTokenPayload(
    val token: String,
    val nonce: String? = null,
)

@Serializable
private data class UpdateUserRequest(
    val nickname: String?,
    val cohortId: String?,
)

@Serializable
private data class SignInSocialResponse(
    val user: AuthUser? = null,
    val token: String? = null,
)

@Serializable
private data class GetSessionResponse(
    val user: AuthUser,
    val session: SessionPayload? = null,
)

@Serializable
private data class SessionPayload(
    val token: String? = null,
    @SerialName("expiresAt") val expiresAt: String? = null,
)

@Serializable
private data class AuthErrorBody(
    val message: String? = null,
    val code: String? = null,
)
