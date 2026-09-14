package hu.petrik.filcapp.auth

import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Sign-in state machine: bootstrap, interactive sign-in, silent refresh, sign-out. */
internal class AuthRepository(
    http: HttpClient,
    private val cookies: PersistentCookieStorage,
    private val storage: AuthStorage,
    private val webAuthSession: WebAuthSession = WebAuthSession(),
) {
    private val api = AuthApi(http)
    private val entra = EntraAuthFlow(http, storage, webAuthSession)

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val mutex = Mutex()

    /**
     * Restores the session on app start: an unanswered browser redirect first, then the stored
     * Chronos cookie, then a silent Entra refresh before the user is asked to sign in again.
     */
    suspend fun bootstrap() =
        mutex.withLock {
            val redirectUri = webAuthSession.consumePendingRedirect()
            if (redirectUri != null && entra.hasPendingSignIn()) {
                completeSignIn { entra.completeSignIn(redirectUri) }
                return@withLock
            }

            val session = runCatching { api.getSession() }.getOrNull()
            if (session != null) {
                _state.value = AuthState.SignedIn(session)
                return@withLock
            }

            cookies.clear()

            val refreshedProof = runCatching { entra.refreshIdToken() }.getOrNull()
            if (refreshedProof == null) {
                _state.value = AuthState.SignedOut()
                return@withLock
            }

            val restored = runCatching { api.signInWithMicrosoft(refreshedProof) }
            val user = restored.getOrNull()
            if (user != null) {
                _state.value = AuthState.SignedIn(user)
                return@withLock
            }

            discardStoredCredentials()
            _state.value =
                AuthState.SignedOut(
                    restored.exceptionOrNull().describeFailure(),
                )
        }

    suspend fun signIn() =
        mutex.withLock {
            completeSignIn { entra.acquireIdToken() }
        }

    suspend fun refreshSession() =
        mutex.withLock {
            val session = runCatching { api.getSession() }.getOrNull()
            if (session == null) {
                cookies.clear()
                _state.value = AuthState.SignedOut()
            } else {
                _state.value = AuthState.SignedIn(session)
            }
        }

    suspend fun updateUser(
        nickname: String?,
        cohortId: String?,
    ) = mutex.withLock {
        val user = api.updateUser(nickname = nickname, cohortId = cohortId)
        _state.value = AuthState.SignedIn(user)
    }

    suspend fun signOut() =
        mutex.withLock {
            runCatching { api.signOut() }
            discardStoredCredentials()
            _state.value = AuthState.SignedOut()
        }

    private suspend fun completeSignIn(acquireIdToken: suspend () -> IdTokenProof) {
        _state.value = AuthState.SigningIn()
        try {
            _state.value = AuthState.SignedIn(api.signInWithMicrosoft(acquireIdToken()))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            if (failure.isRejectedToken()) {
                discardStoredCredentials()
            }
            _state.value = AuthState.SignedOut(failure.describeFailure())
        }
    }

    private suspend fun discardStoredCredentials() {
        cookies.clear()
        storage.clear()
    }
}

private fun Throwable?.describeFailure(): String {
    if (this == null) {
        return "Sign-in failed."
    }
    if (isRejectedToken()) {
        return "Chronos rejected the sign-in token."
    }
    return message?.takeIf(String::isNotBlank) ?: "Sign-in failed."
}

private fun Throwable?.isRejectedToken(): Boolean = this is AuthException && code == INVALID_TOKEN_CODE

private const val INVALID_TOKEN_CODE = "INVALID_TOKEN"

/**
 * Process-wide auth wiring. [httpClient] is the authenticated client for Chronos.
 */
object Auth {
    private val cookieStorage = PersistentCookieStorage()
    private val client = createAuthHttpClient(cookieStorage)
    private val repository = AuthRepository(client, cookieStorage, AuthStorage())

    val state: StateFlow<AuthState>
        get() = repository.state

    val httpClient: HttpClient
        get() = client

    suspend fun bootstrap() = repository.bootstrap()

    suspend fun signIn() = repository.signIn()

    suspend fun refreshSession() = repository.refreshSession()

    suspend fun updateUser(
        nickname: String?,
        cohortId: String?,
    ) = repository.updateUser(nickname = nickname, cohortId = cohortId)

    suspend fun signOut() = repository.signOut()
}
