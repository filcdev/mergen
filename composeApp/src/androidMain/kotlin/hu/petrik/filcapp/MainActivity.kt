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
import hu.petrik.filcapp.widgets.MergenWidgetProvider
import hu.petrik.filcapp.widgets.WidgetNavigation
import hu.petrik.filcapp.widgets.WidgetSyncWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initializeAndroidLanguageStorage(this)
        AndroidAuthContext.register(this)
        handleRedirectIntent(intent)
        handleWidgetIntent(intent)
        if (MergenWidgetProvider.widgetIds(this).isNotEmpty()) {
            WidgetSyncWorker.requestNow(this)
        }

        setContent {
            App()
            PlayInAppUpdateHandler()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRedirectIntent(intent)
        handleWidgetIntent(intent)
    }

    override fun onDestroy() {
        AndroidAuthContext.unregister(this)
        super.onDestroy()
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent?.action != WidgetNavigation.ACTION_OPEN) {
            return
        }
        WidgetNavigation.open(
            mode = intent.getStringExtra(WidgetNavigation.EXTRA_MODE),
            scope = intent.getStringExtra(WidgetNavigation.EXTRA_SCOPE),
            selectionId = intent.getStringExtra(WidgetNavigation.EXTRA_SELECTION_ID),
        )
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
