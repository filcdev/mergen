package hu.petrik.filcapp.auth

import kotlinx.serialization.Serializable

/**
 * User as returned by Better Auth (`/auth/get-session`, `/auth/sign-in/social`).
 * `displayName`, `cohortId` and `roles` come from Chronos' `customSession` and the `user`
 * table's additional fields.
 */
@Serializable
data class AuthUser(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val displayName: String? = null,
    val cohortId: String? = null,
    val roles: List<String>? = null,
)

/** What the UI renders. */
sealed interface AuthState {
    data object Loading : AuthState

    data class SigningIn(val message: String? = null) : AuthState

    data class SignedOut(val message: String? = null) : AuthState

    data class SignedIn(val user: AuthUser) : AuthState
}

/** Expected sign-in failures (dismissed browser, rejected token, unconfigured build). */
class AuthException(message: String, val code: String? = null, cause: Throwable? = null) :
    Exception(message, cause)

/** Authorization request parameters that must survive the browser round-trip. */
@Serializable
internal data class PendingFlow(
    val state: String,
    val codeVerifier: String,
    val nonce: String,
    val issuedAt: Long,
)

/** Tokens returned by Entra's token endpoint. */
internal data class OidcTokens(
    val idToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long?,
)
