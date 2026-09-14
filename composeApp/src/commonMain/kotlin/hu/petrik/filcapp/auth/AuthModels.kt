package hu.petrik.filcapp.auth

import kotlinx.serialization.Serializable

/**
 * User as returned by Better Auth (`/auth/get-session`, `/auth/sign-in/social`).
 * `displayName`, `cohortId`, `nickname`, `roles` and `permissions` come from Chronos.
 */
@Serializable
data class AuthUser(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val nickname: String? = null,
    val displayName: String? = null,
    val cohortId: String? = null,
    val roles: List<String>? = null,
    val permissions: List<String>? = null,
) {
    val preferredName: String
        get() =
            displayName?.takeIf { it.isNotBlank() }
                ?: nickname?.takeIf { it.isNotBlank() }
                ?: name.ifBlank { email }
}

/** What the UI renders. */
sealed interface AuthState {
    data object Loading : AuthState

    data class SigningIn(val message: String? = null) : AuthState

    data class SignedOut(val message: String? = null) : AuthState

    data class SignedIn(val user: AuthUser) : AuthState

    /**
     * Compatibility accessors used by the timetable/settings feature branch.
     * They intentionally delegate to the native OIDC state machine instead of
     * introducing a second authentication implementation.
     */
    companion object {
        val user: AuthUser?
            get() = (Auth.state.value as? SignedIn)?.user

        val profile: SchoolProfileDto?
            get() = SchoolProfileState.profile

        val signedIn: Boolean
            get() = Auth.state.value is SignedIn

        val loading: Boolean
            get() = Auth.state.value is Loading || Auth.state.value is SigningIn

        val error: String?
            get() = (Auth.state.value as? SignedOut)?.message

        val callbackVersion: Int
            get() = SchoolProfileState.version
    }
}

/** Expected sign-in failures (dismissed browser, rejected token, unconfigured build). */
class AuthException(
    message: String,
    val code: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Authorization request parameters that must survive the browser round-trip. */
@Serializable
internal data class PendingFlow(
    val state: String,
    val codeVerifier: String,
    val nonce: String,
    val issuedAt: Long,
)

/** ID token proof handed to Chronos. Interactive login carries its OIDC nonce. */
internal data class IdTokenProof(
    val token: String,
    val nonce: String? = null,
)

/** Tokens returned by Entra's token endpoint. */
internal data class OidcTokens(
    val idToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long?,
)
