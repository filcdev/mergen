package hu.petrik.filcapp.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSURL
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

// Runs inside the WebView so cookies from the POST response land in the WebView's jar,
// fixing the state_mismatch that occurs when the POST is made from Ktor instead.
private val AUTH_HTML = """
<!DOCTYPE html><html><body><script>
fetch('/api/auth/sign-in/social', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({provider: 'microsoft', callbackURL: '/'})
}).then(r => r.json()).then(d => { window.location.href = d.url; });
</script></body></html>
""".trimIndent()

@Composable
actual fun AuthWebView(apiBaseUrl: String, onSessionAcquired: (String) -> Unit, onDismiss: () -> Unit) {
    val baseUrl = remember(apiBaseUrl) { NSURL.URLWithString(apiBaseUrl) }
    val delegate = remember { NavDelegate(onSessionAcquired) }

    Box(Modifier.fillMaxSize()) {
        UIKitView(
            factory = {
                val webView = WKWebView()
                webView.navigationDelegate = delegate
                webView.loadHTMLString(AUTH_HTML, baseURL = baseUrl)
                webView
            },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

private class NavDelegate(
    private val onSessionAcquired: (String) -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        WKWebsiteDataStore.defaultDataStore().httpCookieStore.getAllCookies { cookies ->
            @Suppress("UNCHECKED_CAST")
            val list = cookies as? List<NSHTTPCookie> ?: return@getAllCookies
            val token = list.firstOrNull { it.name == "filc.session_token" }?.value
            if (token != null) onSessionAcquired(token)
        }
    }
}
