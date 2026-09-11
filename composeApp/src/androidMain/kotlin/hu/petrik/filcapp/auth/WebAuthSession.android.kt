package hu.petrik.filcapp.auth

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.CompletableDeferred

/**
 * Chrome Custom Tabs sign-in. The redirect is captured by `MainActivity`'s `filcapp://auth`
 * intent filter and handed to [registerRedirect].
 *
 * When the activity (or process) is destroyed while the browser is open, the redirect is queued
 * instead so `Auth.bootstrap()` can finish the sign-in on the next start.
 */
internal actual class WebAuthSession actual constructor() {
    private var pending: CompletableDeferred<String>? = null

    actual suspend fun authorize(
        url: String,
        callbackScheme: String,
    ): String {
        val activity =
            AndroidAuthContext.currentActivity()
                ?: throw AuthException("The sign-in browser could not be opened.")
        val deferred = CompletableDeferred<String>()
        pending = deferred
        if (!openBrowser(activity, url)) {
            pending = null
            throw AuthException("No browser is available to sign in with.")
        }
        inFlight = this
        try {
            return deferred.await()
        } finally {
            if (inFlight === this) {
                inFlight = null
            }
            pending = null
        }
    }

    actual fun consumePendingRedirect(): String? = pendingRedirects.removeFirstOrNull()

    private fun openBrowser(
        activity: ComponentActivity,
        url: String,
    ): Boolean {
        val uri = Uri.parse(url)
        return try {
            CustomTabsIntent.Builder().setUrlBarHidingEnabled(true).build().launchUrl(activity, uri)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }

    internal companion object {
        private var inFlight: WebAuthSession? = null
        private val pendingRedirects = ArrayDeque<String>()

        /** Called by `MainActivity` for every intent carrying the `filcapp://auth` redirect. */
        fun registerRedirect(uri: String) {
            val session = inFlight
            val waiter = session?.pending
            if (waiter != null && !waiter.isCompleted) {
                waiter.complete(uri)
                return
            }
            pendingRedirects.addLast(uri)
        }
    }
}
