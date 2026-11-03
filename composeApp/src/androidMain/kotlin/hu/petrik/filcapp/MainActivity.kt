package hu.petrik.filcapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import hu.petrik.filcapp.auth.AndroidAuthContext
import hu.petrik.filcapp.auth.Auth
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.auth.WebAuthSession
import hu.petrik.filcapp.notifications.MobilePushRegistration
import hu.petrik.filcapp.notifications.NotificationEvents
import hu.petrik.filcapp.notifications.NotificationNavigation
import hu.petrik.filcapp.settings.initializeAndroidLanguageStorage
import hu.petrik.filcapp.update.PlayInAppUpdateHandler
import hu.petrik.filcapp.widgets.MergenWidgetProvider
import hu.petrik.filcapp.widgets.WidgetNavigation
import hu.petrik.filcapp.widgets.WidgetSyncWorker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        val preferences = getSharedPreferences("mergen_push_permission", MODE_PRIVATE)
        if (!preferences.getBoolean("requested", false)) {
            preferences.edit().putBoolean("requested", true).apply()
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initializeAndroidLanguageStorage(this)
        AndroidAuthContext.register(this)
        handleRedirectIntent(intent)
        handleWidgetIntent(intent)
        handleNotificationIntent(intent)
        lifecycleScope.launch {
            Auth.state.collectLatest { state ->
                if (state is AuthState.SignedIn) requestNotificationPermissionOnce()
                MobilePushRegistration.syncAccount(applicationContext, state)
            }
        }
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
        handleNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        NotificationEvents.changed()
    }

    override fun onDestroy() {
        AndroidAuthContext.unregister(this)
        super.onDestroy()
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        if (intent.getStringExtra("type") == "notification" || intent.hasExtra("notificationType")) {
            NotificationNavigation.open(
                intent.getStringExtra("notificationType"),
                intent.getStringExtra("notificationId"),
            )
            intent.removeExtra("type")
            intent.removeExtra("notificationType")
        }
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
