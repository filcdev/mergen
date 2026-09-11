package hu.petrik.filcapp.auth

/**
 * Opens an authorization URL in the system browser and returns the redirect URI that the
 * platform hands back to the app.
 *
 * Android: Chrome Custom Tabs (falling back to any browser via `ACTION_VIEW`), redirect
 * captured by `MainActivity`'s intent filter.
 * iOS: `ASWebAuthenticationSession`, which intercepts the callback itself.
 *
 * The system browser - not an embedded WebView - is deliberate: it shares the device's Entra
 * SSO state, and the app never renders the identity provider's credential prompt.
 */
internal expect class WebAuthSession() {
    /**
     * Opens [url] and suspends until a [callbackScheme] redirect arrives.
     *
     * @throws AuthException if the user dismisses the browser, no browser is installed, or the
     * app is recreated before the redirect (Android) - the caller can resume via
     * [consumePendingRedirect] after a restart.
     */
    suspend fun authorize(
        url: String,
        callbackScheme: String,
    ): String

    /**
     * A [callbackScheme] redirect that arrived while no [authorize] call was waiting - Android
     * may restart the activity (and the process) while the browser is in the foreground.
     * Returns `null` on platforms that cannot deliver a redirect out of band.
     */
    fun consumePendingRedirect(): String?
}
