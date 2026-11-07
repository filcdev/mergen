package hu.petrik.filcapp.notifications

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import hu.petrik.filcapp.auth.AndroidAuthContext
import hu.petrik.filcapp.auth.Auth
import hu.petrik.filcapp.auth.AuthState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/** Associates the FCM installation with the Chronos user, never with the teacher name. */
internal object MobilePushRegistration {
    private const val PREFS = "mergen_fcm_registration"
    private const val USER_KEY = "userId"
    private const val TOKEN_KEY = "token"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var signingOut = false

    suspend fun syncAccount(
        context: Context,
        state: AuthState,
    ) {
        if (!MergenPushApplication.isConfigured(context)) return
        mutex.withLock {
            val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val priorId = preferences.getString(USER_KEY, null)
            if (state is AuthState.SignedIn && !signingOut) {
                try {
                    if (priorId != null && priorId != state.user.id) {
                        // The previous session is gone: invalidate its device token before reuse.
                        FirebaseMessaging.getInstance().deleteToken().await()
                        preferences.edit().clear().apply()
                    }
                    val token = FirebaseMessaging.getInstance().token.await()
                    NotificationRepository.registerToken(token)
                    preferences.edit().putString(USER_KEY, state.user.id).putString(TOKEN_KEY, token).apply()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Retry on a later sign-in or token refresh.
                }
            } else if (state is AuthState.SignedOut) {
                signingOut = false
                if (priorId != null) {
                    try {
                        FirebaseMessaging.getInstance().deleteToken().await()
                        preferences.edit().clear().apply()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Keep the token for a later invalidation attempt.
                    }
                }
            }
        }
    }

    /** The authenticated session still exists here; revoke the backend token first. */
    suspend fun unregisterOnSignOut(context: Context) {
        if (!MergenPushApplication.isConfigured(context)) return
        mutex.withLock {
            signingOut = true
            val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val token = preferences.getString(TOKEN_KEY, null)
            if (token != null) {
                try {
                    NotificationRepository.unregisterToken(token)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Deleting the Firebase token also invalidates a stale server registration.
                }
            }
            try {
                FirebaseMessaging.getInstance().deleteToken().await()
                preferences.edit().clear().apply()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Preserve the stored token for retry on next launch.
            }
        }
    }

    fun onTokenRefreshed(
        context: Context,
        token: String,
    ) {
        serviceScope.launch {
            AndroidAuthContext.initialize(context)
            try {
                Auth.refreshSession()
                mutex.withLock {
                    if (signingOut) return@withLock
                    val user = (Auth.state.value as? AuthState.SignedIn)?.user ?: return@withLock
                    NotificationRepository.registerToken(token)
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(USER_KEY, user.id)
                        .putString(TOKEN_KEY, token)
                        .apply()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A later app launch registers the current token again.
            }
        }
    }
}
