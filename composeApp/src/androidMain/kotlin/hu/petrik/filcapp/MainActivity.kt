package hu.petrik.filcapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import hu.petrik.filcapp.auth.AndroidAuthContext
import hu.petrik.filcapp.auth.WebAuthSession

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Secure storage and the Custom Tabs launcher need a context before the UI composes.
        AndroidAuthContext.register(this)
        handleRedirectIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRedirectIntent(intent)
    }

    override fun onDestroy() {
        AndroidAuthContext.unregister(this)
        super.onDestroy()
    }

    /**
     * The sign-in redirect comes back as `filcapp://auth?code=…&state=…`. While the browser is
     * open the process may be killed, in which case the URI arrives here on a fresh start and
     * `Auth.bootstrap()` resumes the flow.
     */
    private fun handleRedirectIntent(intent: Intent?) {
        intent?.data?.let { uri -> WebAuthSession.registerRedirect(uri.toString()) }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
