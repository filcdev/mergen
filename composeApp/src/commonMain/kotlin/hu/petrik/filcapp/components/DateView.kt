package hu.petrik.filcapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.petrik.filcapp.models.EnrichedLesson
import hu.petrik.filcapp.models.Period
import hu.petrik.filcapp.models.MovedLessonWithRelations
import hu.petrik.filcapp.screens.TimetableState
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.Clock

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun DateView(modifier: Modifier = Modifier) {
    var currentDateTime by remember {
        mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            delay(1000L)
        }
    }

    val dayOfWeek = currentDateTime.dayOfWeek.name
        .lowercase().replaceFirstChar { it.uppercase() }

    val monthName = when (currentDateTime.month) {
        Month.JANUARY -> "Jan"; Month.FEBRUARY -> "Feb"; Month.MARCH -> "Mar"
        Month.APRIL -> "Apr"; Month.MAY -> "May"; Month.JUNE -> "Jun"
        Month.JULY -> "Jul"; Month.AUGUST -> "Aug"; Month.SEPTEMBER -> "Sep"
        Month.OCTOBER -> "Oct"; Month.NOVEMBER -> "Nov"; Month.DECEMBER -> "Dec"
    }

    val day = currentDateTime.dayOfMonth.toString().padStart(2, '0')
    val year = currentDateTime.year
    val hour = currentDateTime.hour.toString().padStart(2, '0')
    val minute = currentDateTime.minute.toString().padStart(2, '0')

    val status = rememberLessonStatus(currentDateTime)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = dayOfWeek,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "$day, $monthName $year",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "$hour:$minute",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .background(
                    color = when (status) {
                        is LessonStatus.InClass -> MaterialTheme.colorScheme.primaryContainer
                        is LessonStatus.Break -> MaterialTheme.colorScheme.secondaryContainer
                        is LessonStatus.NotStarted -> MaterialTheme.colorScheme.tertiaryContainer
                        is LessonStatus.Done, LessonStatus.NoData -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(12.dp),
            ) {
                when (status) {
                    is LessonStatus.InClass -> {
                        Text(
                            text = "In class",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = status.subject,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                    is LessonStatus.NotStarted -> {
                        Text(
                            text = "First class",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                            text = "starts at ${status.firstStart}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            textAlign = TextAlign.Center,
                        )
                    }
                    is LessonStatus.Break -> {
                        Text(
                            text = "Break",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "until ${status.nextStart}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center,
                        )
                    }
                    is LessonStatus.Done -> {
                        Text(
                            text = "School done",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    LessonStatus.NoData -> {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Upcoming classes ──────────────────────────────────────────────────────────

data class UpcomingLesson(val subject: String, val startTime: String, val room: String?)

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun UpcomingClasses(modifier: Modifier = Modifier) {
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val upcoming = rememberUpcomingLessons(now, limit = 3)

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "Upcoming",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        if (upcoming.isEmpty()) {
            Text(
                text = "No more classes today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                upcoming.forEach { lesson ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = lesson.subject,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = lesson.startTime,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (lesson.room != null) {
                                Text(
                                    text = lesson.room,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Status & upcoming helpers ─────────────────────────────────────────────────

sealed interface LessonStatus {
    data class InClass(val subject: String) : LessonStatus
    data class NotStarted(val firstStart: String) : LessonStatus
    data class Break(val nextStart: String) : LessonStatus
    data object Done : LessonStatus
    data object NoData : LessonStatus
}

private data class Slot(
    val start: LocalTime,
    val end: LocalTime,
    val subject: String,
    val room: String?,
)

private fun buildTodaySlots(
    lessons: List<EnrichedLesson>,
    movedLessons: List<MovedLessonWithRelations>,
    today: LocalDate,
): List<Slot> {
    val dayNum = today.dayOfWeek.isoDayNumber.toString()
    val movedToday = movedLessons.filter { it.movedLesson.date.startsWith(today.toString()) }
    val movedAwayIds = movedLessons
        .filter { ml ->
            !ml.movedLesson.date.startsWith(today.toString()) &&
            ml.dayDefinition?.days?.contains(dayNum) == true
        }
        .flatMap { it.lessons }.toSet()

    val base = lessons.filter { lesson ->
        val days = lesson.day?.days?.filterNotNull() ?: return@filter false
        days.any { it == dayNum } && lesson.id !in movedAwayIds
    }
    val extra = movedToday.mapNotNull { ml -> lessons.find { it.id in ml.lessons } }

    return (base + extra).mapNotNull { lesson ->
        val ml = movedToday.firstOrNull { lesson.id in it.lessons }
        val p: Period = ml?.period ?: lesson.period ?: return@mapNotNull null
        val start = p.startTime?.take(5)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return@mapNotNull null
        val end = p.endTime?.take(5)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return@mapNotNull null
        val subject = lesson.subject?.name ?: return@mapNotNull null
        val room = (ml?.classroom?.name ?: lesson.classrooms.firstOrNull()?.name)?.let { "Room $it" }
        Slot(start, end, subject, room)
    }.sortedBy { it.start }
}

@Composable
private fun rememberLessonStatus(now: LocalDateTime): LessonStatus {
    val lessons = TimetableState.lessons
    val movedLessons = TimetableState.movedLessons
    if (!TimetableState.timetableLoaded || lessons.isEmpty()) return LessonStatus.NoData

    val slots = buildTodaySlots(lessons, movedLessons, now.date)
    if (slots.isEmpty()) return LessonStatus.NoData

    val time = now.time
    val current = slots.firstOrNull { time >= it.start && time <= it.end }
    if (current != null) return LessonStatus.InClass(current.subject)

    val next = slots.firstOrNull { it.start > time }
    if (next != null) {
        val anyPast = slots.any { it.end < time }
        return if (anyPast) LessonStatus.Break(next.start.toString().take(5))
        else LessonStatus.NotStarted(next.start.toString().take(5))
    }

    return LessonStatus.Done
}

@Composable
private fun rememberUpcomingLessons(now: LocalDateTime, limit: Int): List<UpcomingLesson> {
    val lessons = TimetableState.lessons
    val movedLessons = TimetableState.movedLessons
    if (!TimetableState.timetableLoaded || lessons.isEmpty()) return emptyList()

    val slots = buildTodaySlots(lessons, movedLessons, now.date)
    val time = now.time
    val future = slots.filter { it.start > time }

    // When status is Break/NotStarted the first future slot is already shown in the square — skip it
    val inClass = slots.any { time >= it.start && time <= it.end }
    val tail = if (inClass) future else future.drop(1)

    return tail.take(limit).map { UpcomingLesson(it.subject, it.start.toString().take(5), it.room) }
}
