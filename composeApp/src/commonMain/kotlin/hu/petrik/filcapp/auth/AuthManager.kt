package hu.petrik.filcapp.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hu.petrik.filcapp.network.CohortDto
import hu.petrik.filcapp.network.FilcPublicApi
import hu.petrik.filcapp.network.GroupDto
import hu.petrik.filcapp.network.TeacherDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val API_BASE_URL = "https://filc.petrik.hu/api"
private const val AUTH_BASE_URL = "$API_BASE_URL/auth"
private const val APP_ORIGIN = "mergen://"
private const val CALLBACK_URL = "mergen://auth/callback"
private const val ERROR_CALLBACK_URL = "mergen://auth/error"

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String,
    val name: String = "",
    val nickname: String? = null,
    val displayName: String? = null,
    val cohortId: String? = null,
    val image: String? = null,
    val roles: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
) {
    val preferredName: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: nickname?.takeIf { it.isNotBlank() }
            ?: name.ifBlank { email }
}

@Serializable
data class AuthSessionDto(
    val id: String = "",
    val userId: String = "",
    val expiresAt: String? = null,
)

@Serializable
data class SessionResponseDto(
    val user: AuthUserDto? = null,
    val session: AuthSessionDto? = null,
)

@Serializable
data class SchoolProfileDto(
    val cohort: CohortDto? = null,
    val groups: List<GroupDto> = emptyList(),
    val teacher: TeacherDto? = null,
)

@Serializable
private data class SocialSignInRequest(
    val provider: String,
    val callbackURL: String,
    val errorCallbackURL: String,
    val newUserCallbackURL: String,
    val disableRedirect: Boolean,
)

@Serializable
private data class SocialSignInResponse(
    val redirect: Boolean = false,
    val url: String? = null,
)

@Serializable
private data class UpdateUserRequest(
    val nickname: String? = null,
    val cohortId: String? = null,
)

@Serializable
private data class SelectGroupRequest(
    val groupId: String,
)

object AuthState {
    var initialized by mutableStateOf(false)
        internal set
    var loading by mutableStateOf(false)
        internal set
    var user by mutableStateOf<AuthUserDto?>(null)
        internal set
    var profile by mutableStateOf<SchoolProfileDto?>(null)
        internal set
    var error by mutableStateOf<String?>(null)
        internal set
    var callbackVersion by mutableIntStateOf(0)
        internal set

    internal var cookie: String? = null

    val signedIn: Boolean
        get() = user != null && !cookie.isNullOrBlank()
}

expect fun openExternalUrl(url: String)

expect fun loadStoredAuthCookie(): String?

expect fun saveStoredAuthCookie(cookie: String?)

object AuthManager {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
    }

    suspend fun initialize() {
        if (AuthState.initialized) return
        AuthState.cookie = loadStoredAuthCookie()
        if (!AuthState.cookie.isNullOrBlank()) {
            refreshSession()
        }
        AuthState.initialized = true
    }

    suspend fun signInWithMicrosoft() {
        AuthState.loading = true
        AuthState.error = null
        try {
            val response = client.post("$AUTH_BASE_URL/sign-in/social") {
                contentType(ContentType.Application.Json)
                header("mergen-origin", APP_ORIGIN)
                header("x-skip-oauth-proxy", "true")
                AuthState.cookie?.let { header(HttpHeaders.Cookie, it) }
                setBody(
                    SocialSignInRequest(
                        provider = "microsoft",
                        callbackURL = CALLBACK_URL,
                        errorCallbackURL = ERROR_CALLBACK_URL,
                        newUserCallbackURL = CALLBACK_URL,
                        disableRedirect = true,
                    ),
                )
            }

            val signIn = response.body<SocialSignInResponse>()
            val authorizationUrl = signIn.url
                ?: error("A Microsoft bejelentkezési URL nem érkezett meg a szervertől.")
            val oauthState = extractCookieValue(response, "oauth_state")

            val proxyUrl = URLBuilder("$AUTH_BASE_URL/mergen-authorization-proxy").apply {
                parameters.append("authorizationURL", authorizationUrl)
                oauthState?.let { parameters.append("oauthState", it) }
            }.buildString()

            openExternalUrl(proxyUrl)
        } catch (throwable: Throwable) {
            AuthState.error = throwable.message ?: "Nem sikerült elindítani a Microsoft bejelentkezést."
        } finally {
            AuthState.loading = false
        }
    }

    suspend fun refreshSession() {
        val cookie = AuthState.cookie ?: return clearLocalSession()
        AuthState.loading = true
        try {
            val session = client.get("$AUTH_BASE_URL/get-session") {
                header(HttpHeaders.Cookie, cookie)
                header("mergen-origin", APP_ORIGIN)
                header("x-skip-oauth-proxy", "true")
            }.body<SessionResponseDto>()

            val user = session.user
            if (user == null || session.session == null) {
                clearLocalSession()
                return
            }

            AuthState.user = user
            val profileResult = runCatching { FilcPublicApi.getMyProfile() }
            AuthState.profile = profileResult.getOrNull()
            AuthState.error = profileResult.exceptionOrNull()?.message
        } catch (throwable: Throwable) {
            clearLocalSession()
            AuthState.error = throwable.message
        } finally {
            AuthState.loading = false
        }
    }

    suspend fun signOut() {
        AuthState.loading = true
        try {
            AuthState.cookie?.let { cookie ->
                runCatching {
                    client.post("$AUTH_BASE_URL/sign-out") {
                        header(HttpHeaders.Cookie, cookie)
                        header("mergen-origin", APP_ORIGIN)
                        header("x-skip-oauth-proxy", "true")
                    }
                }
            }
        } finally {
            clearLocalSession()
            AuthState.loading = false
        }
    }

    suspend fun updateUser(
        nickname: String? = AuthState.user?.nickname,
        cohortId: String? = AuthState.user?.cohortId,
    ) {
        val cookie = AuthState.cookie ?: return
        client.post("$AUTH_BASE_URL/update-user") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Cookie, cookie)
            header("mergen-origin", APP_ORIGIN)
            header("x-skip-oauth-proxy", "true")
            setBody(UpdateUserRequest(nickname = nickname, cohortId = cohortId))
        }
        refreshSession()
    }

    suspend fun selectGroup(groupId: String) {
        FilcPublicApi.selectGroup(groupId)
        refreshSession()
    }

    suspend fun syncLanguage(language: String) {
        if (!AuthState.signedIn) return
        runCatching { FilcPublicApi.updateLanguage(language.lowercase()) }
    }

    fun handleDeepLink(url: String) {
        runCatching {
            val parsed = Url(url)
            if (parsed.protocol.name != "mergen") return

            if (parsed.host == "auth" && parsed.segments.firstOrNull() == "error") {
                AuthState.error = parsed.parameters["error"] ?: "Microsoft bejelentkezési hiba."
                AuthState.callbackVersion++
                return
            }

            if (parsed.host != "auth" || parsed.segments.firstOrNull() != "callback") return
            val setCookie = parsed.parameters["cookie"] ?: error("Hiányzik a session cookie a callbackból.")
            val cookieHeader = extractSessionCookie(setCookie)
                ?: error("Nem található Filc session cookie a callbackban.")

            AuthState.cookie = cookieHeader
            saveStoredAuthCookie(cookieHeader)
            AuthState.error = null
            AuthState.callbackVersion++
        }.onFailure { throwable ->
            AuthState.error = throwable.message ?: "Nem sikerült feldolgozni a bejelentkezést."
            AuthState.callbackVersion++
        }
    }

    private fun extractCookieValue(response: HttpResponse, suffix: String): String? {
        val headers = response.headers.getAll(HttpHeaders.SetCookie).orEmpty()
        return headers.asSequence()
            .flatMap { it.split(Regex(",(?=\\s*(?:__Secure-)?[^;,=]+=[^;,]*)")).asSequence() }
            .map { it.substringBefore(';').trim() }
            .firstOrNull { it.substringBefore('=').contains(suffix) }
            ?.substringAfter('=', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractSessionCookie(setCookie: String): String? {
        val regex = Regex("((?:__Secure-)?filc\\.session_token)=([^;,\\s]+)")
        val match = regex.find(setCookie) ?: return null
        return "${match.groupValues[1]}=${match.groupValues[2]}"
    }

    private fun clearLocalSession() {
        AuthState.cookie = null
        AuthState.user = null
        AuthState.profile = null
        saveStoredAuthCookie(null)
    }
}

object AuthBridge {
    fun handleDeepLink(url: String) {
        AuthManager.handleDeepLink(url)
    }
}
