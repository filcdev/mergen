package hu.petrik.filcapp.auth

import io.ktor.client.HttpClient

/**
 * Authorization Code + PKCE against Entra ID, in the system browser.
 *
 * The in-flight request (state, PKCE verifier, nonce) is persisted before the browser opens so a
 * sign-in survives the app being killed while the user is authenticating - on Android the
 * redirect can arrive at a freshly started process.
 */
internal class EntraAuthFlow(
    http: HttpClient,
    private val storage: AuthStorage,
    private val webAuthSession: WebAuthSession = WebAuthSession(),
) {
    private val oidc = OidcClient(http)

    /** Runs the interactive sign-in and returns an ID token for Chronos. */
    suspend fun acquireIdToken(): String {
        ensureConfigured()
        val flow = oidc.newPendingFlow()
        storage.writePendingFlow(flow)
        val redirectUri = webAuthSession.authorize(oidc.authorizationUrl(flow), AuthConfig.CALLBACK_SCHEME)
        return completeSignIn(redirectUri)
    }

    /** Completes a sign-in whose browser round-trip outlived this app process. */
    suspend fun completeSignIn(redirectUri: String): String {
        ensureConfigured()
        val flow =
            storage.readPendingFlow()
                ?: throw AuthException("The sign-in session expired. Please try again.")
        storage.writePendingFlow(null)
        if (nowMillis() - flow.issuedAt > PENDING_FLOW_TTL_MILLIS) {
            throw AuthException("The sign-in session expired. Please try again.")
        }

        val redirect = parseRedirect(redirectUri)
        redirect.error?.let { throw AuthException(it) }
        val code = redirect.code ?: throw AuthException("The sign-in response contained no authorization code.")
        if (redirect.state != flow.state) {
            throw AuthException("The sign-in response did not match this request.")
        }

        val tokens = oidc.exchangeCode(code, flow)
        tokens.refreshToken?.let(storage::writeRefreshToken)
        return tokens.idToken
    }

    /**
     * ID token from the stored refresh token, or `null` when there is none or Entra SSO lapsed
     * (Chronos sessions expire after 7 days, the Entra session usually outlives that).
     */
    suspend fun refreshIdToken(): String? {
        if (!AuthConfig.isConfigured) {
            return null
        }
        val refreshToken = storage.readRefreshToken() ?: return null
        val tokens = oidc.refresh(refreshToken)
        tokens.refreshToken?.let(storage::writeRefreshToken)
        return tokens.idToken
    }

    fun hasPendingSignIn(): Boolean = storage.readPendingFlow() != null

    private fun ensureConfigured() {
        if (!AuthConfig.isConfigured) {
            throw AuthException("This build has no Entra configuration. Set AuthConfig.ENTRA_CLIENT_ID and ENTRA_TENANT_ID.")
        }
    }

    private companion object {
        /** How long a browser round-trip may take before we consider the request abandoned. */
        const val PENDING_FLOW_TTL_MILLIS = 15 * 60 * 1000L
    }
}
