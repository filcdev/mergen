package hu.petrik.filcapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DateView()

        SectionHeader(
            title = tr("Friss hírek", "Latest news"),
            subtitle =
                tr(
                    "A petrik.hu legújabb bejegyzései",
                    "Latest posts from petrik.hu",
                ),
            actionLabel = "petrik.hu",
            onAction = { uriHandler.openUri(PETRIK_HOME_URL) },
        )

        when {
            loading -> NewsLoadingCard()
            news.isNotEmpty() -> {
                news.forEach { item ->
                    NewsCard(
                        item = item,
                        onOpen = { uriHandler.openUri(item.link) },
                    )
                }
            }

            else -> {
                NewsErrorCard(
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

        InstagramCard(
            onOpen = { uriHandler.openUri(PETRIK_INSTAGRAM_URL) },
        )

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TextButton(onClick = onAction) {
            Icon(Icons.Default.Language, contentDescription = null)
            Text(
                text = " $actionLabel",
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun NewsCard(
    item: PetrikNewsItem,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
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
                        style = MaterialTheme.typography.labelMedium,
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
                HorizontalDivider()
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
private fun NewsLoadingCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
private fun NewsErrorCard(
    message: String,
    onRetry: () -> Unit,
    onOpenWebsite: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = tr("A hírek most nem tölthetők be", "News could not be loaded"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
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
}

@Composable
private fun InstagramCard(onOpen: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
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
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
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
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )

            Button(onClick = onOpen) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Text(tr(" Instagram megnyitása", " Open Instagram"))
            }
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
