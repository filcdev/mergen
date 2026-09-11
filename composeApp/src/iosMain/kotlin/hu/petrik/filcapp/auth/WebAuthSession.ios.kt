package hu.petrik.filcapp.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * `ASWebAuthenticationSession` sign-in: the system browser (with the device's Entra SSO state)
 * opens, and the session itself intercepts the `filcapp` redirect - no URL scheme registration is
 * required on iOS.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual class WebAuthSession actual constructor() {
    private var session: ASWebAuthenticationSession? = null

    // ASWebAuthenticationSession holds its context provider weakly; keep it alive ourselves.
    private var contextProvider: PresentationContextProvider? = null

    actual suspend fun authorize(
        url: String,
        callbackScheme: String,
    ): String =
        suspendCancellableCoroutine { continuation ->
            val authorizationUrl = NSURL.URLWithString(url)
            if (authorizationUrl == null) {
                continuation.resumeWithException(AuthException("The authorization URL could not be read."))
                return@suspendCancellableCoroutine
            }

            val authenticationSession =
                ASWebAuthenticationSession(
                    uRL = authorizationUrl,
                    callbackURLScheme = callbackScheme,
                ) { callbackUrl, error ->
                    session = null
                    contextProvider = null
                    if (!continuation.isActive) {
                        return@ASWebAuthenticationSession
                    }
                    val redirectUri = callbackUrl?.absoluteString
                    if (redirectUri != null) {
                        continuation.resume(redirectUri)
                    } else {
                        continuation.resumeWithException(
                            AuthException(error?.localizedDescription ?: "Sign-in was cancelled."),
                        )
                    }
                }

            val provider = PresentationContextProvider()
            authenticationSession.presentationContextProvider = provider
            contextProvider = provider
            session = authenticationSession

            continuation.invokeOnCancellation { authenticationSession.cancel() }

            if (!authenticationSession.start()) {
                session = null
                contextProvider = null
                continuation.resumeWithException(AuthException("The sign-in browser could not be opened."))
            }
        }

    /** iOS is single-activity: a redirect can never arrive without a waiting session. */
    actual fun consumePendingRedirect(): String? = null
}

private class PresentationContextProvider :
    NSObject(),
    ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor {
        val keyWindow = UIApplication.sharedApplication.keyWindow
        return keyWindow ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow) ?: UIWindow()
    }
}
