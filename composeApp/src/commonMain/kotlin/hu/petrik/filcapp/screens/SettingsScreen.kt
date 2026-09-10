package hu.petrik.filcapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.settings.AppLanguage
import hu.petrik.filcapp.settings.AppSettings
import hu.petrik.filcapp.settings.tr

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tr("Beállítások", "Settings"), style = MaterialTheme.typography.headlineSmall)
            Text(
                tr(
                    "Az alkalmazás megjelenésének és nyelvének beállításai.",
                    "Application appearance and language settings.",
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Language, null)
                    Column {
                        Text(tr("Nyelv", "Language"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            tr("A módosítás azonnal életbe lép.", "Changes apply immediately."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                LanguageOption(
                    title = "Magyar",
                    selected = AppSettings.language.value == AppLanguage.HU,
                    onClick = { AppSettings.setLanguage(AppLanguage.HU) },
                )
                LanguageOption(
                    title = "English",
                    selected = AppSettings.language.value == AppLanguage.EN,
                    onClick = { AppSettings.setLanguage(AppLanguage.EN) },
                )
            }
        }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

object SettingsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = tr("Beállítások", "Settings")
            val icon = rememberVectorPainter(Icons.Default.Settings)
            return remember(title) {
                TabOptions(index = 4u, title = title, icon = icon)
            }
        }

    @Composable
    override fun Content() {
        SettingsScreen()
    }
}
