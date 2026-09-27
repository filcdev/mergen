package hu.petrik.filcapp.screens
import androidx.compose.foundation.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.calendar.SchoolCalendarApi
import hu.petrik.filcapp.calendar.SchoolCalendarEvent
import hu.petrik.filcapp.components.SchoolCalendarScreen
import hu.petrik.filcapp.news.PetrikNewsApi
import hu.petrik.filcapp.news.PetrikNewsItem
import hu.petrik.filcapp.settings.tr
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val PETRIK_HOME_URL = "https://petrik.hu/"
private val HOME_TIME_ZONE = TimeZone.of("Europe/Budapest")

private object HomeSessionCache {
    var news: List<PetrikNewsItem> = emptyList()
    var newsLoaded = false
    var newsError: String? = null
    var calendarEvents: List<SchoolCalendarEvent> = emptyList()
    var calendarLoaded = false
    var calendarError: String? = null
}

@OptIn(ExperimentalTime::class)
@Composable
fun HomeScreen() {
    val uriHandler = LocalUriHandler.current
    var news by remember { mutableStateOf(HomeSessionCache.news) }
    var loading by remember { mutableStateOf(!HomeSessionCache.newsLoaded) }
    var error by remember { mutableStateOf(HomeSessionCache.newsError) }
    var reloadKey by remember { mutableIntStateOf(0) }

    var calendarEvents by remember { mutableStateOf(HomeSessionCache.calendarEvents) }
    var calendarLoading by remember { mutableStateOf(!HomeSessionCache.calendarLoaded) }
    var calendarError by remember { mutableStateOf(HomeSessionCache.calendarError) }
    var calendarReloadKey by remember { mutableIntStateOf(0) }
    var showFullCalendar by remember { mutableStateOf(false) }

    LaunchedEffect(reloadKey) {
        if (reloadKey == 0 && HomeSessionCache.newsLoaded) {
            loading = false
            return@LaunchedEffect
        }

        loading = true
        error = null
        runCatching { PetrikNewsApi.latest(limit = 3) }
            .onSuccess { items ->
                news = items
                HomeSessionCache.news = items
                HomeSessionCache.newsError = null
            }.onFailure { throwable ->
                error =
                    throwable.message
                        ?: tr(
                            "Nem sikerült betölteni a Petrik híreit.",
                            "Could not load Petrik news.",
                        )
                HomeSessionCache.newsError = error
            }
        HomeSessionCache.newsLoaded = true
        loading = false
    }

    LaunchedEffect(calendarReloadKey) {
        if (calendarReloadKey == 0 && HomeSessionCache.calendarLoaded) {
            calendarLoading = false
            return@LaunchedEffect
        }

        calendarLoading = true
        calendarError = null
        runCatching { SchoolCalendarApi.getEvents() }
            .onSuccess { items ->
                calendarEvents = items
                HomeSessionCache.calendarEvents = items
                HomeSessionCache.calendarError = null
            }.onFailure { throwable ->
                calendarError =
                    throwable.message
                        ?: tr(
                            "Nem sikerült betölteni az iskolai naptárt.",
                            "Could not load the school calendar.",
                        )
                HomeSessionCache.calendarError = calendarError
            }
        HomeSessionCache.calendarLoaded = true
        calendarLoading = false
    }

    if (showFullCalendar) {
        SchoolCalendarScreen(
            events = calendarEvents,
            loading = calendarLoading,
            error = calendarError,
            onRetry = { calendarReloadKey++ },
            onBack = { showFullCalendar = false },
        )
        return
    }

    val now = Clock.System.now().toLocalDateTime(HOME_TIME_ZONE)
    val today = now.date
    val userName =
        AuthState.user
            ?.nickname
            ?.takeIf { it.isNotBlank() }
            ?: AuthState.user?.preferredName.orEmpty()
    val firstName = userName.trim().split(" ").lastOrNull().orEmpty()
    val greeting =
        when {
            now.hour >= 18 -> tr("Jó estét", "Good evening")
            now.hour >= 10 -> tr("Jó napot", "Good afternoon")
            now.hour >= 4 -> tr("Jó reggelt", "Good morning")
            else -> tr("Jó estét", "Good evening")
        }

    val nextEvent =
        calendarEvents.firstOrNull { event ->
            event.end?.date?.let { it >= today } ?: (event.start.date >= today)
        } ?: calendarEvents.firstOrNull()

    PullToRefreshBox(
        isRefreshing = loading || calendarLoading,
        onRefresh = {
            reloadKey++
            calendarReloadKey++
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatHeaderDate(today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                        if (firstName.isBlank()) {
                            "$greeting!"
                        } else {
                            "$greeting, $firstName!"
                        },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            when {
                calendarLoading -> HeroLoadingCard()
                nextEvent != null -> {
                    UpcomingEventHero(
                        event = nextEvent,
                        onOpen = { showFullCalendar = true },
                    )
                }

                else -> {
                    ErrorCard(
                        title = tr("Az iskolai naptár most nem érhető el", "School calendar is unavailable"),
                        message = calendarError.orEmpty(),
                        onRetry = { calendarReloadKey++ },
                    )
                }
            }

            LatestAnnouncementHeader()

            when {
                loading -> AnnouncementLoadingCard()
                news.isNotEmpty() -> {
                    AnnouncementCard(
                        item = news.first(),
                        onOpen = { uriHandler.openUri(news.first().link) },
                    )
                }

                else -> {
                    ErrorCard(
                        title = tr("A hírek most nem tölthetők be", "News could not be loaded"),
                        message = error.orEmpty(),
                        onRetry = { reloadKey++ },
                    )
                }
            }

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(PETRIK_HOME_URL) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("További hírek a Petrik oldalán", "More news on the Petrik website"),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "petrik.hu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun UpcomingEventHero(
    event: SchoolCalendarEvent,
    onOpen: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = tr("KÖVETKEZŐ ESEMÉNY", "NEXT EVENT"),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Text(
                    text = formatEventTime(event),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                event.location?.takeIf { it.isNotBlank() }?.let { location ->
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatEventDate(event.start.date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = tr("Iskolai naptár megnyitása", "Open school calendar"),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestAnnouncementHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = tr("Legfrissebb iskolai hirdetmény", "Latest school announcement"),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun AnnouncementCard(
    item: PetrikNewsItem,
    onOpen: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = tr("Petrik", "Petrik"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.excerpt.isNotBlank()) {
                Text(
                    text = item.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = tr("Elolvasom →", "Read more →"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun HeroLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text(tr("Naptár betöltése…", "Loading calendar…"))
        }
    }
}

@Composable
private fun AnnouncementLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text(tr("Hírek betöltése…", "Loading news…"))
        }
    }
}

@Composable
private fun ErrorCard(
    title: String,
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (message.isNotBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(tr(" Újra", " Retry"))
            }
        }
    }
}

private fun formatHeaderDate(date: LocalDate): String {
    val dayName =
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "HÉTFŐ"
            DayOfWeek.TUESDAY -> "KEDD"
            DayOfWeek.WEDNESDAY -> "SZERDA"
            DayOfWeek.THURSDAY -> "CSÜTÖRTÖK"
            DayOfWeek.FRIDAY -> "PÉNTEK"
            DayOfWeek.SATURDAY -> "SZOMBAT"
            DayOfWeek.SUNDAY -> "VASÁRNAP"
        }
    return "${date.year}. ${date.monthNumber.twoDigits()}. ${date.day.twoDigits()}., $dayName"
}

private fun formatEventDate(date: LocalDate): String =
    "${date.year}. ${date.monthNumber.twoDigits()}. ${date.day.twoDigits()}."

private fun formatEventTime(event: SchoolCalendarEvent): String {
    if (event.allDay) return "Egész nap"
    val start = "${event.start.hour.twoDigits()}:${event.start.minute.twoDigits()}"
    val end = event.end
    return if (end != null && end.date == event.start.date) {
        "$start – ${end.hour.twoDigits()}:${end.minute.twoDigits()}"
    } else {
        start
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

object HomeTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = tr("Főoldal", "Home")
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
