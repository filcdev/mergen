package hu.petrik.filcapp.auth

/**
 * Entra ID / OpenID Connect settings for the native sign-in flow.
 *
 * [ENTRA_CLIENT_ID] and [ENTRA_TENANT_ID] must match the Chronos deployment's
 * `CHRONOS_ENTRA_CLIENT_ID` and `CHRONOS_ENTRA_TENANT_ID`: Better Auth verifies the ID token
 * against Entra's JWKS and rejects tokens whose `aud` (client id) or `iss`/`tid` (tenant) do
 * not match, responding with `INVALID_TOKEN`.
 *
 * [REDIRECT_URI] must be registered on that app registration's
 * "Mobile and desktop applications" platform. Mergen is a public client: it never uses a
 * client secret, it verifies nothing locally, and the ID token is what proves the sign-in to
 * Chronos.
 */
object AuthConfig {
    /**
     * The Chronos deployment's `CHRONOS_ENTRA_CLIENT_ID`. Better Auth rejects ID tokens whose
     * `aud` is a different client id, so this must match the backend - the mobile app shares the
     * deployment's app registration (its `filcapp://auth` redirect is registered there).
     */
    const val ENTRA_CLIENT_ID = "97d7a0db-2afc-4c0b-af6f-c9ca8357d68d"

    /** Must equal `CHRONOS_ENTRA_TENANT_ID` (tenant GUID or verified domain). */
    const val ENTRA_TENANT_ID = "6701f6a4-b306-4be9-93be-7b9809225222"

    const val AUTHORITY = "https://login.microsoftonline.com"

    /** Custom scheme handed to the platform browser; registered in AndroidManifest.xml. */
    const val REDIRECT_URI = "filcapp://auth"

    const val CALLBACK_SCHEME = "filcapp"

    /** `offline_access` buys the refresh token used to re-sign-in without user interaction. */
    const val SCOPES = "openid profile email offline_access"

    /**
     * Chronos API root. Kept separate from the generated client's `ApiClient.BASE_URL`: the
     * checked-in OpenAPI document still points at the retired `filc.space` domain.
     */
    const val API_BASE_URL = "https://filc.petrik.hu/api"

    const val AUTH_BASE_URL = "$API_BASE_URL/auth"

    /**
     * Origin sent with Better Auth requests. Better Auth validates the origin of requests that
     * carry a session cookie, so state-changing `/auth` calls (sign-out) need one the deployment
     * trusts - which is its own base URL.
     */
    const val TRUSTED_ORIGIN = "https://filc.petrik.hu"

    val authorizationEndpoint: String
        get() = "$AUTHORITY/$ENTRA_TENANT_ID/oauth2/v2.0/authorize"

    val tokenEndpoint: String
        get() = "$AUTHORITY/$ENTRA_TENANT_ID/oauth2/v2.0/token"

    val isConfigured: Boolean
        get() = ENTRA_CLIENT_ID.isNotBlank() && ENTRA_TENANT_ID.isNotBlank()
}
