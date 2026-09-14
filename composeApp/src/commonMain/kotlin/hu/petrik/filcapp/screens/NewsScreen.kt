package hu.petrik.filcapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.network.AnnouncementDto
import hu.petrik.filcapp.network.FilcPublicApi
import hu.petrik.filcapp.settings.tr
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun NewsScreen() {
    var announcements by remember { mutableStateOf<List<AnnouncementDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        try {
            announcements = FilcPublicApi.getAnnouncements().filter(::isRelevantAnnouncement)
        } catch (throwable: Throwable) {
            error = throwable.message ?: tr("Nem sikerült betölteni a híreket.", "Could not load news.")
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr("Hírek", "News"), style = MaterialTheme.typography.headlineSmall)
                Text(
                    tr(
                        "A Filc aktuális közleményei egy helyen.",
                        "Current Filc announcements in one place.",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!loading && announcements.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        announcements.size.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(tr("Hiba történt", "Something went wrong"), style = MaterialTheme.typography.titleMedium)
                        Text(error.orEmpty())
                        Button(onClick = { reloadKey++ }) {
                            Text(tr("Újrapróbálás", "Retry"))
                        }
                    }
                }
            }

            announcements.isEmpty() -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Newspaper, null)
                        Text(tr("Nincs aktuális hír", "No current news"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            tr(
                                "Jelenleg nincs a következő 14 napra érvényes közlemény.",
                                "There are no announcements relevant for the next 14 days.",
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                announcements
                    .sortedBy { normalizeDate(it.validFrom) }
                    .forEach { announcement -> AnnouncementCard(announcement) }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(item: AnnouncementDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(
                        Icons.Default.Campaign,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Text(
                    item.title?.takeIf { it.isNotBlank() } ?: tr("Közlemény", "Announcement"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            renderContent(item.content).takeIf { it.isNotBlank() }?.let { content ->
                Text(content, style = MaterialTheme.typography.bodyMedium)
            }

            val from = formatDate(item.validFrom)
            val until = formatDate(item.validUntil)
            Text(
                if (from == until) from else "$from – $until",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun renderContent(element: JsonElement): String =
    when (element) {
        is JsonPrimitive -> element.content
        is JsonArray -> element.joinToString(" ") { renderContent(it) }.trim()
        is JsonObject -> {
            element["content"]?.let(::renderContent)
                ?: element.values.joinToString(" ") { renderContent(it) }.trim()
        }
    }

@OptIn(ExperimentalTime::class)
private fun isRelevantAnnouncement(item: AnnouncementDto): Boolean =
    runCatching {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val end = LocalDate.fromEpochDays(today.toEpochDays() + 14)
        val from = LocalDate.parse(normalizeDate(item.validFrom))
        val until = LocalDate.parse(normalizeDate(item.validUntil))
        from <= end && until >= today
    }.getOrDefault(true)

private fun normalizeDate(value: String): String = value.take(10)

private fun formatDate(value: String): String {
    val normalized = normalizeDate(value)
    val parts = normalized.split("-")
    if (parts.size != 3) {
        return normalized
    }
    return if (hu.petrik.filcapp.settings.AppSettings.language.value == hu.petrik.filcapp.settings.AppLanguage.HU) {
        "${parts[0]}. ${parts[1]}. ${parts[2]}."
    } else {
        "${parts[0]}-${parts[1]}-${parts[2]}"
    }
}

object NewsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = tr("Hírek", "News")
            val icon = rememberVectorPainter(Icons.Default.Campaign)
            return remember(title) {
                TabOptions(index = 3u, title = title, icon = icon)
            }
        }

    @Composable
    override fun Content() {
        NewsScreen()
    }
}
