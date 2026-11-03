package hu.petrik.filcapp.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.settings.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun NotificationInboxScreen(
    onBack: () -> Unit,
    onOpenSubstitutions: () -> Unit,
) {
    val userId = AuthState.user?.id
    val refresh = NotificationEvents.version
    val scope = rememberCoroutineScope()
    var items by remember(userId) { mutableStateOf<List<NotificationEntry>>(emptyList()) }
    var busy by remember(userId) { mutableStateOf(true) }
    var error by remember(userId) { mutableStateOf<String?>(null) }

    LaunchedEffect(userId, refresh) {
        if (userId == null) {
            return@LaunchedEffect
        }
        busy = true
        try {
            items = NotificationRepository.list()
            error = null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            error = exception.message ?: tr("Nem sikerült betölteni.", "Could not load notifications.")
        } finally {
            busy = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(tr("Értesítések", "Notifications"), style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = onBack) { Text(tr("Vissza", "Back")) }
        }
        if (items.any { !it.read }) {
            OutlinedButton(onClick = {
                scope.launch {
                    try {
                        NotificationRepository.markAllRead()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        error = exception.message
                    }
                }
            }) {
                Text(tr("Összes megjelölése olvasottként", "Mark all as read"))
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Button(onClick = { NotificationEvents.changed() }) { Text(tr("Újrapróbálás", "Retry")) }
        }
        if (busy) {
            CircularProgressIndicator()
        } else if (items.isEmpty() && error == null) {
            Text(tr("Nincs értesítés.", "No notifications yet."))
        } else {
            items.forEach { entry ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        scope.launch {
                            if (!entry.read) {
                                try {
                                    NotificationRepository.markRead(entry.id)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (exception: Exception) {
                                    error = exception.message
                                }
                            }
                            if (opensSubstitutions(entry.type)) {
                                onOpenSubstitutions()
                            }
                        }
                    },
                    color = if (entry.read) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (entry.content.isNotBlank()) {
                            Text(entry.content, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(entry.createdAt.take(16).replace("T", " "), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationSettingsSection() {
    val userId = AuthState.user?.id
    val scope = rememberCoroutineScope()
    var prefs by remember(userId) { mutableStateOf<NotificationPreferences?>(null) }
    var saving by remember(userId) { mutableStateOf(false) }
    var error by remember(userId) { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId == null) {
            return@LaunchedEffect
        }
        try {
            prefs = NotificationRepository.settings()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            error = exception.message
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("Értesítések", "Notifications"), style = MaterialTheme.typography.titleLarge)
        Text(
            tr("A FILC webes felületével közös értesítési beállítások.", "Shared notification settings with FILC web."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (prefs == null && error == null) {
            CircularProgressIndicator()
        }
        prefs?.let { current ->
            NotificationToggle(tr("Email és push értesítések", "Email and push notifications"), current.channelsEnabled) {
                prefs = current.copy(channelsEnabled = it)
            }
            HorizontalDivider()
            NotificationToggle(tr("Helyettesítések", "Substitutions"), current.substitution) {
                prefs = current.copy(substitution = it)
            }
            NotificationToggle(tr("Áthelyezett órák", "Moved lessons"), current.movedLesson) {
                prefs = current.copy(movedLesson = it)
            }
            NotificationToggle(tr("Közlemények", "Announcements"), current.announcement) {
                prefs = current.copy(announcement = it)
            }
            NotificationToggle(tr("Rendszerüzenetek", "System messages"), current.systemMessage) {
                prefs = current.copy(systemMessage = it)
            }
            NotificationToggle(tr("Hírek", "News"), current.blogPost) {
                prefs = current.copy(blogPost = it)
            }
            NotificationToggle(tr("Beléptetőkártya használata", "Door-lock card usage"), current.doorlockCardUsed) {
                prefs = current.copy(doorlockCardUsed = it)
            }
            Button(
                enabled = !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            NotificationRepository.saveSettings(current)
                            error = null
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (exception: Exception) {
                            error = exception.message
                        } finally {
                            saving = false
                        }
                    }
                },
            ) { Text(tr("Beállítások mentése", "Save settings")) }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
