package hu.petrik.filcapp.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
                    selectionId = ownId?.takeIf { id -> choices.any { it.first == id } }
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Mergen widget beállítása", style = MaterialTheme.typography.headlineSmall)
        Text("Mit mutasson a kezdőképernyőn?", style = MaterialTheme.typography.titleMedium)
        WidgetChoice("Aktuális / következő óra", mode == WidgetMode.CURRENT_LESSON) {
            mode = WidgetMode.CURRENT_LESSON
        }
        WidgetChoice("Helyettesítések", mode == WidgetMode.SUBSTITUTIONS) {
            mode = WidgetMode.SUBSTITUTIONS
        }

        Text("Kinek az adatai jelenjenek meg?", style = MaterialTheme.typography.titleMedium)
        WidgetChoice("Tanár", scope == WidgetScope.TEACHER) {
            selectionId = null
            scope = WidgetScope.TEACHER
        }
        WidgetChoice("Osztály", scope == WidgetScope.COHORT) {
            selectionId = null
            scope = WidgetScope.COHORT
        }

        if (busy) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                Text("Adatok betöltése…")
            }
        } else {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            SearchableSelection(
                options = choices,
                selectedId = selectionId,
                label = if (scope == WidgetScope.TEACHER) "Tanár" else "Osztály",
                placeholder = "Válassz a listából",
                onSelected = { selectionId = it },
            )
        }
        Text(
            "Az adatok háttérben frissülnek. Az óra visszaszámlálója másodpercenként jár; " +
                "a folyamatjelző és az adatfrissítés Android-energiatakarékosságtól függ.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            enabled = !busy && selectionId != null,
            onClick = {
                val id = selectionId ?: return@Button
                val name = choices.firstOrNull { it.first == id }?.second.orEmpty()
                onSave(WidgetConfig(mode, scope, id, name))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Widget hozzáadása")
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Mégse")
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title)
    }
}
