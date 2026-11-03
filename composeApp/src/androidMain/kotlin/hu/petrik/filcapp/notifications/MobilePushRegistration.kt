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
import kotlinx.coroutines.tasks.await

/** Associates the FCM installation with the authenticated Chronos user, never with a teacher name. */
internal object MobilePushRegistration {
    private const val PREFS = "mergen_fcm_registration"
    private const val USER_KEY = "userId"
    private const val TOKEN_KEY = "token"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun syncAccount(
        context: Context,
        state: AuthState,
    ) {
        if (!MergenPushApplication.isConfigured(context)) {
            return
        }
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val priorId = preferences.getString(USER_KEY, null)
        if (state is AuthState.SignedIn) {
            try {
                if (priorId != null && priorId != state.user.id) {
                    FirebaseMessaging.getInstance().deleteToken().await()
                    preferences.edit().clear().apply()
                }
                val token = FirebaseMessaging.getInstance().token.await()
                NotificationRepository.registerToken(token)
                preferences.edit().putString(USER_KEY, state.user.id).putString(TOKEN_KEY, token).apply()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Network problems are retried on the next sign-in or token refresh.
            }
        } else if (state is AuthState.SignedOut && priorId != null) {
            try {
                FirebaseMessaging.getInstance().deleteToken().await()
                preferences.edit().clear().apply()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Do not erase the record until the old token has been invalidated.
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
                val user = (Auth.state.value as? AuthState.SignedIn)?.user ?: return@launch
                NotificationRepository.registerToken(token)
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(USER_KEY, user.id)
                    .putString(TOKEN_KEY, token)
                    .apply()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A later app launch registers the current token again.
            }
        }
    }
}
