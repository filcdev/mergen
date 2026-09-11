package hu.petrik.filcapp.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Entra ID public-client (PKCE) calls. Nothing is validated on the device: Chronos checks the
 * ID token's signature, audience, tenant and age, so the app only has to obtain one.
 */
internal class OidcClient(private val http: HttpClient) {
    fun newPendingFlow(): PendingFlow =
        PendingFlow(
            state = randomBytes(16).toBase64Url(),
            codeVerifier = newCodeVerifier(),
            nonce = randomBytes(16).toBase64Url(),
            issuedAt = nowMillis(),
        )

    fun authorizationUrl(
        flow: PendingFlow,
        prompt: String? = null,
    ): String =
        URLBuilder(AuthConfig.authorizationEndpoint)
            .apply {
                parameters.append("client_id", AuthConfig.ENTRA_CLIENT_ID)
                parameters.append("response_type", "code")
                parameters.append("redirect_uri", AuthConfig.REDIRECT_URI)
                parameters.append("response_mode", "query")
                parameters.append("scope", AuthConfig.SCOPES)
                parameters.append("state", flow.state)
                parameters.append("nonce", flow.nonce)
                parameters.append("code_challenge", codeChallengeS256(flow.codeVerifier))
                parameters.append("code_challenge_method", "S256")
                prompt?.let { parameters.append("prompt", it) }
            }.buildString()

    suspend fun exchangeCode(
        code: String,
        flow: PendingFlow,
    ): OidcTokens {
        val response =
            http.submitForm(
                AuthConfig.tokenEndpoint,
                parameters {
                    append("grant_type", "authorization_code")
                    append("client_id", AuthConfig.ENTRA_CLIENT_ID)
                    append("code", code)
                    append("redirect_uri", AuthConfig.REDIRECT_URI)
                    append("code_verifier", flow.codeVerifier)
                    append("scope", AuthConfig.SCOPES)
                },
            )
        return response.parseTokens()
    }

    /** Re-signs in without user interaction; Entra only reissues an ID token while SSO holds. */
    suspend fun refresh(refreshToken: String): OidcTokens {
        val response =
            http.submitForm(
                AuthConfig.tokenEndpoint,
                parameters {
                    append("grant_type", "refresh_token")
                    append("client_id", AuthConfig.ENTRA_CLIENT_ID)
                    append("refresh_token", refreshToken)
                    append("scope", AuthConfig.SCOPES)
                },
            )
        return response.parseTokens()
    }

    private suspend fun HttpResponse.parseTokens(): OidcTokens {
        val text = bodyAsText()
        if (!status.isSuccess()) {
            throw AuthException(entraErrorMessage(text) ?: "Entra rejected the sign-in (HTTP ${status.value}).")
        }
        val tokens =
            runCatching { AuthJson.decodeFromString<TokenResponse>(text) }.getOrNull()
                ?: throw AuthException("Entra returned an unexpected token response.")
        val idToken = tokens.idToken ?: throw AuthException("Entra did not return an ID token.")
        return OidcTokens(
            idToken = idToken,
            refreshToken = tokens.refreshToken,
            expiresInSeconds = tokens.expiresIn,
        )
    }
}

@OptIn(ExperimentalTime::class)
internal fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Parameter the browser handed back to the app. */
internal data class RedirectResult(val code: String?, val state: String?, val error: String?) {
    val isSuccess: Boolean get() = code != null && error == null
}

internal fun parseRedirect(redirectUri: String): RedirectResult {
    val url =
        runCatching { Url(redirectUri) }.getOrNull()
            ?: throw AuthException("The sign-in redirect could not be read.")
    return RedirectResult(
        code = url.parameters["code"],
        state = url.parameters["state"],
        error = url.parameters["error_description"] ?: url.parameters["error"],
    )
}

@Serializable
private data class TokenResponse(
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

private fun entraErrorMessage(text: String): String? {
    val parsed = runCatching { AuthJson.decodeFromString<TokenResponse>(text) }.getOrNull() ?: return null
    val description = parsed.errorDescription?.takeIf { it.isNotBlank() }
    val code = parsed.error?.takeIf { it.isNotBlank() }
    return when {
        code != null && description != null -> "$code: $description"
        else -> code ?: description
    }
}
