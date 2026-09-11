package hu.petrik.filcapp

import androidx.compose.ui.window.ComposeUIViewController
import hu.petrik.filcapp.settings.AppSettings

fun MainViewController() =
    run {
        AppSettings.initialize()
        ComposeUIViewController { App() }
    }
