package hu.petrik.filcapp.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.settings.tr

@Composable
fun TopBar() {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    "Filc",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    tr("Petrik iskolai alkalmazás", "Petrik school app"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
