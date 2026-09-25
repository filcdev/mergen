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
import hu.petrik.filcapp.settings.initializeAndroidLanguageStorage
import hu.petrik.filcapp.update.PlayInAppUpdateHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initializeAndroidLanguageStorage(this)
        AndroidAuthContext.register(this)
        handleRedirectIntent(intent)

        setContent {
            App()
            PlayInAppUpdateHandler()
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

    private fun handleRedirectIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            WebAuthSession.registerRedirect(uri.toString())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
