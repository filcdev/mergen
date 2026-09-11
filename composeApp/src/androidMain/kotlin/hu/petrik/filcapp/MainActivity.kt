package hu.petrik.filcapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import hu.petrik.filcapp.auth.AuthBridge
import hu.petrik.filcapp.auth.initializeAndroidAuth
import hu.petrik.filcapp.settings.AppSettings
import hu.petrik.filcapp.settings.initializeAndroidLanguageStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initializeAndroidLanguageStorage(this)
        initializeAndroidAuth(this)
        AppSettings.initialize()
        handleAuthIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        intent?.dataString?.let(AuthBridge::handleDeepLink)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
