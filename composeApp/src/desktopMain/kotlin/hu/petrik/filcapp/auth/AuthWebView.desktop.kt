package hu.petrik.filcapp.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.net.URI

@Composable
actual fun AuthWebView(apiBaseUrl: String, onSessionAcquired: (String) -> Unit, onDismiss: () -> Unit) {
    var tokenInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        runCatching { Desktop.getDesktop().browse(URI("$apiBaseUrl")) }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(420.dp)) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Sign in (debug)", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Sign in via the browser at $apiBaseUrl, then open DevTools " +
                        "(F12) → Application → Cookies → find filc.session_token and paste its value below.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("filc.session_token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { if (tokenInput.isNotBlank()) onSessionAcquired(tokenInput.trim()) },
                        modifier = Modifier.weight(1f),
                        enabled = tokenInput.isNotBlank(),
                    ) { Text("Continue") }
                }
            }
        }
    }
}
