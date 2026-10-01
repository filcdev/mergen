package hu.petrik.filcapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import hu.petrik.filcapp.settings.AppLanguage
import hu.petrik.filcapp.settings.AppSettings
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

private object NewsSessionCache {
    var announcements: List<AnnouncementDto> = emptyList()
    var loaded = false
}

@Composable
fun NewsScreen() {
    var announcements by remember { mutableStateOf(NewsSessionCache.announcements) }
    var loading by remember { mutableStateOf(!NewsSessionCache.loaded) }
    var error by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        if (reloadKey == 0 && NewsSessionCache.loaded) {
            loading = false
            return@LaunchedEffect
        }

        loading = true
        error = false

        try {
            announcements =
                FilcPublicApi
                    .getAnnouncements()
                    .filter(::isRelevantAnnouncement)
                    .sortedByDescending { normalizeDate(it.validFrom) }

            NewsSessionCache.announcements = announcements
        } catch (_: Throwable) {
            error = true
        } finally {
            NewsSessionCache.loaded = true
            loading = false
        }
    }

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { reloadKey++ },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NewsOverviewHeader(announcementCount = announcements.size)

            when {
                loading -> NewsLoadingBlock()
                error -> NewsErrorBlock(onRetry = { reloadKey++ })
                announcements.isEmpty() -> NewsEmptyBlock()
                else -> {
                    announcements.forEach { announcement ->
                        AnnouncementCard(announcement)
                    }
                }
            }

            Spacer(Modifier.size(4.dp))
        }
    }
}

@Composable
private fun NewsOverviewHeader(announcementCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = tr("Hírek", "News"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        tr(
                            "Aktuális iskolai közlemények és információk",
                            "Current school announcements and information",
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Text(
                    text = announcementCount.toString(),
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AnnouncementCard(item: AnnouncementDto) {
    val content = renderContent(item.content).trim()
    val from = formatDate(item.validFrom)
    val until = formatDate(item.validUntil)
    val dateLabel = if (from == until) from else "$from – $until"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Text(
                    text = item.title?.takeIf { it.isNotBlank() } ?: tr("Közlemény", "Announcement"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (content.isNotBlank()) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ) {
                    Text(
                        text = tr("Közlemény", "Announcement"),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NewsLoadingBlock() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NewsErrorBlock(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = tr("A hírek most nem érhetők el", "News is currently unavailable"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                    tr(
                        "Nem sikerült kapcsolódni a hírek szolgáltatásához. Próbáld újra egy kicsit később.",
                        "Could not connect to the news service. Please try again shortly.",
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetry) {
                Text(tr("Újrapróbálás", "Retry"))
            }
        }
    }
}

@Composable
private fun NewsEmptyBlock() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = tr("Nincs aktuális hír", "No current news"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                    tr(
                        "Jelenleg nincs megjeleníthető iskolai közlemény.",
                        "There are currently no school announcements to display.",
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun renderContent(element: JsonElement): String =
    when (element) {
        is JsonPrimitive -> element.content
        is JsonArray ->
            element
                .map(::renderContent)
                .filter { it.isNotBlank() }
                .joinToString("\n")

        is JsonObject ->
            element["text"]?.let(::renderContent)
                ?: element["content"]?.let(::renderContent)
                ?: ""
    }

@OptIn(ExperimentalTime::class)
private fun isRelevantAnnouncement(item: AnnouncementDto): Boolean =
    runCatching {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val end = LocalDate.fromEpochDays(today.toEpochDays() + 14L)
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

    return if (AppSettings.language.value == AppLanguage.HU) {
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
                TabOptions(
                    index = 3u,
                    title = title,
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
        NewsScreen()
    }
}
