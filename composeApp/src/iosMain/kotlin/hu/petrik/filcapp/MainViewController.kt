package hu.petrik.filcapp

import androidx.compose.ui.window.ComposeUIViewController
import hu.petrik.filcapp.settings.AppSettings
import hu.petrik.filcapp.update.AppStoreUpdateHandler

fun MainViewController() =
    run {
        AppSettings.initialize()
        ComposeUIViewController {
            App()
            AppStoreUpdateHandler()
        }
    }
