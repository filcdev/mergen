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
     * Restores the session on app start: an unanswered browser redirect first (Android may have
     * restarted the process mid-sign-in), then the stored session cookie, then a silent refresh.
     */
    suspend fun bootstrap() =
        mutex.withLock {
            val redirectUri = webAuthSession.consumePendingRedirect()
            if (redirectUri != null && entra.hasPendingSignIn()) {
                completeSignIn { entra.completeSignIn(redirectUri) }
                return@withLock
            }

            val user = runCatching { api.getSession() }.getOrNull()
            if (user != null) {
                _state.value = AuthState.SignedIn(user)
                return@withLock
            }

            val refreshedIdToken = runCatching { entra.refreshIdToken() }.getOrNull()
            val refreshedUser =
                refreshedIdToken?.let { idToken ->
                    runCatching { api.signInWithMicrosoft(idToken) }.getOrNull()
                }
            _state.value =
                if (refreshedUser != null) AuthState.SignedIn(refreshedUser) else AuthState.SignedOut()
        }

    suspend fun signIn() = mutex.withLock { completeSignIn { entra.acquireIdToken() } }

    suspend fun signOut() =
        mutex.withLock {
            runCatching { api.signOut() }
            cookies.clear()
            storage.clear()
            _state.value = AuthState.SignedOut()
        }

    private suspend fun completeSignIn(acquireIdToken: suspend () -> String) {
        _state.value = AuthState.SigningIn()
        try {
            val user = api.signInWithMicrosoft(acquireIdToken())
            _state.value = AuthState.SignedIn(user)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (auth: AuthException) {
            _state.value = AuthState.SignedOut(auth.message)
        } catch (failure: Exception) {
            _state.value = AuthState.SignedOut(failure.message ?: "Sign-in failed.")
        }
    }
}

/**
 * Process-wide auth wiring. [httpClient] is the authenticated client for Chronos - hand it to the
 * generated `ApiClient` (`ApiClient(baseUrl, httpClient)`) so generated requests carry the session
 * cookie stored by the cookie jar.
 */
object Auth {
    private val cookieStorage = PersistentCookieStorage()
    private val client = createAuthHttpClient(cookieStorage)
    private val repository = AuthRepository(client, cookieStorage, AuthStorage())

    val state: StateFlow<AuthState> get() = repository.state

    val httpClient: HttpClient get() = client

    suspend fun bootstrap() = repository.bootstrap()

    suspend fun signIn() = repository.signIn()

    suspend fun signOut() = repository.signOut()
}
