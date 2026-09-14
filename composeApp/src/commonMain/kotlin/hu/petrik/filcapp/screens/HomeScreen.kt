package hu.petrik.filcapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.components.DateView
import hu.petrik.filcapp.components.FilcPanel
import hu.petrik.filcapp.news.PetrikNewsApi
import hu.petrik.filcapp.news.PetrikNewsItem
import hu.petrik.filcapp.settings.tr
import kotlinx.datetime.LocalDate

private const val PETRIK_HOME_URL = "https://petrik.hu/"
private const val PETRIK_INSTAGRAM_URL = "https://www.instagram.com/PetrikInsta/"

@Composable
fun HomeScreen() {
    val uriHandler = LocalUriHandler.current
    var news by remember { mutableStateOf<List<PetrikNewsItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null

        runCatching {
            PetrikNewsApi.latest(limit = 3)
        }.onSuccess { items ->
            news = items
        }.onFailure { throwable ->
            error =
                throwable.message
                    ?: tr(
                        "Nem sikerült betölteni a Petrik híreit.",
                        "Could not load Petrik news.",
                    )
        }

        loading = false
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DateView()

        SectionHeader(
            title = tr("Friss hírek", "Latest news"),
            actionLabel = "petrik.hu",
            onAction = { uriHandler.openUri(PETRIK_HOME_URL) },
        )

        when {
            loading -> NewsLoadingPanel()
            news.isNotEmpty() -> {
                news.forEach { item ->
                    NewsPanel(
                        item = item,
                        onOpen = { uriHandler.openUri(item.link) },
                    )
                }
            }

            else -> {
                NewsErrorPanel(
                    message =
                        error
                            ?: tr(
                                "Most nem érhetők el a hírek.",
                                "News is currently unavailable.",
                            ),
                    onRetry = { reloadKey++ },
                    onOpenWebsite = { uriHandler.openUri(PETRIK_HOME_URL) },
                )
            }
        }

        InstagramPanel(
            onOpen = { uriHandler.openUri(PETRIK_INSTAGRAM_URL) },
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 14.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TextButton(onClick = onAction) {
            Icon(Icons.Default.Language, contentDescription = null)
            Text(" $actionLabel")
        }
    }
}

@Composable
private fun NewsPanel(
    item: PetrikNewsItem,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 7.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatPostDate(item.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            if (item.excerpt.isNotBlank()) {
                Text(
                    text = item.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NewsLoadingPanel() {
    FilcPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Column {
                Text(
                    text = tr("Hírek betöltése…", "Loading news…"),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = tr("Kapcsolódás a petrik.hu-hoz", "Connecting to petrik.hu"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NewsErrorPanel(
    message: String,
    onRetry: () -> Unit,
    onOpenWebsite: () -> Unit,
) {
    FilcPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = tr("A hírek most nem tölthetők be", "News could not be loaded"),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(tr(" Újra", " Retry"))
            }
            OutlinedButton(onClick = onOpenWebsite) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Text(" petrik.hu")
            }
        }
    }
}

@Composable
private fun InstagramPanel(onOpen: () -> Unit) {
    FilcPanel(
        modifier = Modifier.fillMaxWidth(),
        title = tr("Közösség", "Community"),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Petrik az Instagramon", "Petrik on Instagram"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "@PetrikInsta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text =
                tr(
                    "Képek, iskolai programok, projektek és a Petrik mindennapjai.",
                    "Photos, school events, projects and everyday life at Petrik.",
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = onOpen) {
            Icon(Icons.Default.OpenInNew, contentDescription = null)
            Text(tr(" Instagram megnyitása", " Open Instagram"))
        }
    }
}

private fun formatPostDate(rawDate: String): String {
    val isoDate = rawDate.substringBefore('T')
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val month = date.monthNumber.toString().padStart(2, '0')
    val day = date.day.toString().padStart(2, '0')
    return "${date.year}.$month.$day."
}

object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = tr("Kezdőlap", "Home")
            val icon = rememberVectorPainter(Icons.Default.Home)
            return remember(title) {
                TabOptions(index = 0u, title = title, icon = icon)
            }
        }

    @Composable
    override fun Content() {
        HomeScreen()
    }
}
