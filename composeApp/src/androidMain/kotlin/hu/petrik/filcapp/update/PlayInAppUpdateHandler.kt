package hu.petrik.filcapp.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import hu.petrik.filcapp.settings.tr

@Composable
fun PlayInAppUpdateHandler() {
    val context = LocalContext.current
    val appUpdateManager =
        remember(context) {
            AppUpdateManagerFactory.create(context.applicationContext)
        }

    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    val updateLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) {
            // Ha a felhasználó megszakítja a frissítést, az app tovább használható.
        }

    DisposableEffect(appUpdateManager) {
        val listener =
            InstallStateUpdatedListener { state ->
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    showRestartDialog = true
                }
            }

        appUpdateManager.registerListener(listener)

        onDispose {
            appUpdateManager.unregisterListener(listener)
        }
    }

    LaunchedEffect(appUpdateManager) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> {
                    showRestartDialog = true
                }

                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    availableUpdate = info
                    showUpdateDialog = true
                }
            }
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Text(tr("Új Mergen verzió érhető el", "A new Mergen version is available"))
            },
            text = {
                Text(
                    tr(
                        "Megjelent a Mergen egy újabb verziója. Frissíthetsz most, " +
                            "vagy folytathatod a jelenlegi verzió használatát.",
                        "A newer version of Mergen is available. You can update now " +
                            "or continue using the current version.",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val info = availableUpdate
                        showUpdateDialog = false

                        if (info != null) {
                            runCatching {
                                appUpdateManager.startUpdateFlowForResult(
                                    info,
                                    updateLauncher,
                                    AppUpdateOptions
                                        .newBuilder(AppUpdateType.FLEXIBLE)
                                        .build(),
                                )
                            }
                        }
                    },
                ) {
                    Text(tr("Frissítés", "Update"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text(tr("Később", "Later"))
                }
            },
        )
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = {
                Text(tr("A frissítés letöltve", "Update downloaded"))
            },
            text = {
                Text(
                    tr(
                        "Az új verzió letöltése elkészült. Az alkalmazás újraindításával " +
                            "befejezheted a frissítést.",
                        "The new version has finished downloading. Restart the app to " +
                            "complete the update.",
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        appUpdateManager.completeUpdate()
                    },
                ) {
                    Text(tr("Újraindítás", "Restart"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(tr("Később", "Later"))
                }
            },
        )
    }
}
