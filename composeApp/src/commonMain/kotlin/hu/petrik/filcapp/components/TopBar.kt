package hu.petrik.filcapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.settings.tr
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val topBarTimeZone = TimeZone.of("Europe/Budapest")

@OptIn(ExperimentalTime::class)
@Composable
fun TopBar() {
    val user = AuthState.user
    val profile = AuthState.profile
    val hour = Clock.System.now().toLocalDateTime(topBarTimeZone).hour
    val greeting =
        when {
            hour >= 18 -> tr("Jó estét", "Good evening")
            hour >= 10 -> tr("Jó napot", "Good afternoon")
            hour >= 4 -> tr("Jó reggelt", "Good morning")
            else -> tr("Jó estét", "Good evening")
        }

    val shortName =
        user
            ?.nickname
            ?.takeIf { it.isNotBlank() }
            ?: user
                ?.preferredName
                ?.trim()
                ?.split(" ")
                ?.lastOrNull()
                .orEmpty()

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text =
                        if (shortName.isBlank()) {
                            greeting
                        } else {
                            "$greeting, $shortName!"
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Text(
                    text =
                        profile?.teacher?.short?.takeIf { it.isNotBlank() }
                            ?: profile?.cohort?.short?.takeIf { it.isNotBlank() }
                            ?: profile?.cohort?.name
                            ?: tr("Petrik", "Petrik"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = shortName.firstOrNull()?.uppercase() ?: "P",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
