package hu.petrik.filcapp.auth

import androidx.compose.runtime.Composable

@Composable
expect fun AuthWebView(
    apiBaseUrl: String,
    onSessionAcquired: (String) -> Unit,
    onDismiss: () -> Unit,
)
