package hu.petrik.filcapp.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.auth.AndroidAuthContext
import hu.petrik.filcapp.auth.Auth
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.components.SearchableSelection
import hu.petrik.filcapp.data.ChronosApi
import hu.petrik.filcapp.theme.FilcTheme
import kotlinx.coroutines.CancellationException

class MergenWidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        val id =
            intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        AndroidAuthContext.initialize(applicationContext)
        val previous = WidgetStorage.readConfig(this, id)
        setContent {
            FilcTheme {
                WidgetConfiguration(
                    previous = previous,
                    onCancel = { finish() },
                    onSave = { selected ->
                        WidgetStorage.saveConfig(this, id, selected)
                        WidgetRenderer.render(this, id)
                        WidgetSyncWorker.schedulePeriodic(this)
                        WidgetSyncWorker.requestNow(this)
                        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        setResult(Activity.RESULT_OK, result)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun WidgetConfiguration(
    previous: WidgetConfig?,
    onCancel: () -> Unit,
    onSave: (WidgetConfig) -> Unit,
) {
    var mode by remember { mutableStateOf(previous?.mode ?: WidgetMode.CURRENT_LESSON) }
    var scope by remember { mutableStateOf(previous?.scope ?: WidgetScope.TEACHER) }
    var selectionId by remember { mutableStateOf(previous?.selectionId) }
    var choices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scope) {
        busy = true
        error = null
        try {
            Auth.refreshSession()
            val user = (Auth.state.value as? AuthState.SignedIn)?.user
            if (user == null) {
                error = "Először jelentkezz be a Mergen alkalmazásban."
                choices = emptyList()
            } else {
                choices =
                    when (scope) {
                        WidgetScope.TEACHER -> {
                            ChronosApi.getTeachers().map { it.id to it.displayName }.sortedBy { it.second }
                        }

                        WidgetScope.COHORT -> {
                            val timetable = ChronosApi.getLatestValidTimetable()
                            ChronosApi.getCohorts(timetable.id).map { it.id to it.name }.sortedBy { it.second }
                        }
                    }
                if (selectionId !in choices.map { it.first }) {
                    val ownId =
                        when (scope) {
                            WidgetScope.TEACHER -> runCatching { ChronosApi.getMyTeacher()?.id }.getOrNull()
                            WidgetScope.COHORT -> user.cohortId
                        }
                    val matchingName =
                        if (scope == WidgetScope.TEACHER) {
                            val names = listOfNotNull(user.displayName, user.name, user.preferredName)
                            choices.firstOrNull { (_, name) ->
                                names.any { it.isNotBlank() && it.trim().equals(name.trim(), ignoreCase = true) }
                            }?.first
                        } else {
                            null
                        }
                    selectionId = ownId?.takeIf { id -> choices.any { it.first == id } } ?: matchingName
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            error = exception.message ?: "Nem sikerült betölteni a választható adatokat."
            choices = emptyList()
        } finally {
            busy = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        enabled = !busy && selectionId != null,
                        onClick = {
                            val id = selectionId ?: return@Button
                            val name = choices.firstOrNull { it.first == id }?.second.orEmpty()
                            onSave(WidgetConfig(mode, scope, id, name))
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Text("Widget hozzáadása")
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Mégse")
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Widget beállítása", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Válaszd ki, mit szeretnél látni a kezdőképernyőn.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            WidgetSection(title = "Megjelenített adatok") {
                WidgetChoice(
                    title = "Aktuális / következő óra",
                    selected = mode == WidgetMode.CURRENT_LESSON,
                    onClick = { mode = WidgetMode.CURRENT_LESSON },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                WidgetChoice(
                    title = "Helyettesítések",
                    selected = mode == WidgetMode.SUBSTITUTIONS,
                    onClick = { mode = WidgetMode.SUBSTITUTIONS },
                )
            }

            WidgetSection(title = "Kinek az adatai?") {
                WidgetChoice(
                    title = "Tanár",
                    selected = scope == WidgetScope.TEACHER,
                    onClick = {
                        selectionId = null
                        scope = WidgetScope.TEACHER
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                WidgetChoice(
                    title = "Osztály",
                    selected = scope == WidgetScope.COHORT,
                    onClick = {
                        selectionId = null
                        scope = WidgetScope.COHORT
                    },
                )
            }

            if (busy) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text("Adatok betöltése…")
                }
            } else {
                error?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                SearchableSelection(
                    options = choices,
                    selectedId = selectionId,
                    label = if (scope == WidgetScope.TEACHER) "Tanár" else "Osztály",
                    placeholder = "Válassz a listából",
                    onSelected = { selectionId = it },
                )
            }

            Text(
                "Az adatok háttérben frissülnek. A visszaszámláló másodpercenként jár; " +
                    "a folyamatjelző frissítését az Android késleltetheti.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun WidgetSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun WidgetChoice(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 58.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}
