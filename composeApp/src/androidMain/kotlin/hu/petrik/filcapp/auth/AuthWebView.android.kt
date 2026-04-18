package hu.petrik.filcapp.auth

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            // Rewrite localhost → emulator host passthrough so the OAuth callback
                            // reaches the dev server and sends the correct cookies.
                            val url = request.url.toString()
                            val rewritten = url
                                .replace("http://localhost:", "http://10.0.2.2:")
                                .replace("https://localhost:", "https://10.0.2.2:")
                            if (rewritten != url) {
                                view.loadUrl(rewritten)
                                return true
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            val raw = CookieManager.getInstance().getCookie(url) ?: return
                            val token = raw.split(";")
                                .map { it.trim() }
                                .firstOrNull { it.startsWith("filc.session_token=") }
                                ?.removePrefix("filc.session_token=")
                            if (token != null) onSessionAcquired(token)
                        }
                    }
                    loadDataWithBaseURL(apiBaseUrl, AUTH_HTML, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}
