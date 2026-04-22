package hu.petrik.filcapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import hu.petrik.filcapp.models.EnrichedLesson
import hu.petrik.filcapp.models.Period
import hu.petrik.filcapp.models.MovedLessonWithRelations
import hu.petrik.filcapp.models.SubstitutionWithRelations
import hu.petrik.filcapp.models.Teacher
import hu.petrik.filcapp.screens.TimetableMode
import hu.petrik.filcapp.screens.TimetableState
import hu.petrik.filcapp.screens.TimetableTab
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.Clock

private val ColorCancelled = Color(0xFFE53935)
private val ColorMoved = Color(0xFF9C27B0)
private val ColorSubstituted = Color(0xFFF59E0B)

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
    val tabNavigator = LocalTabNavigator.current

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
                )
                .then(
                    if (status is LessonStatus.InClass)
                        Modifier.clickable {
                            TimetableState.timetableMode = TimetableMode.Class
                            tabNavigator.current = TimetableTab
                        }
                    else Modifier
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
                        status.lines.forEachIndexed { index, line ->
                            if (index > 0) {
                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                    thickness = 0.5.dp,
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            val nameColor = when {
                                line.isCancelled -> ColorCancelled
                                line.isMovedHere -> ColorMoved
                                line.substituteTeacher != null -> ColorSubstituted
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                            Text(
                                text = line.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = nameColor,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                            if (line.isCancelled) {
                                Text(
                                    text = "Cancelled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorCancelled,
                                    textAlign = TextAlign.Center,
                                )
                            } else if (line.substituteTeacher != null) {
                                Text(
                                    text = line.substituteTeacher,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorSubstituted,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
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

data class SubjectLine(
    val name: String,
    val detail: String?,
    val isCancelled: Boolean = false,
    val isMovedHere: Boolean = false,
    val substituteTeacher: String? = null,
)
data class UpcomingLesson(val lines: List<SubjectLine>, val startTime: String)

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun UpcomingClasses(modifier: Modifier = Modifier) {
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val upcoming = rememberUpcomingLessons(now, limit = 3)
    val tabNavigator = LocalTabNavigator.current

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
                            .clickable {
                                TimetableState.timetableMode = TimetableMode.Class
                                tabNavigator.current = TimetableTab
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            lesson.lines.forEachIndexed { i, line ->
                                if (i > 0) {
                                    Spacer(Modifier.height(6.dp))
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                        thickness = 0.5.dp,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                }
                                val nameColor = when {
                                    line.isCancelled -> ColorCancelled
                                    line.isMovedHere -> ColorMoved
                                    line.substituteTeacher != null -> ColorSubstituted
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Text(
                                    text = line.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = nameColor,
                                    maxLines = 1,
                                )
                                val statusLabel = when {
                                    line.isCancelled -> "Cancelled"
                                    line.isMovedHere -> "Moved here"
                                    else -> null
                                }
                                if (statusLabel != null) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = nameColor,
                                        maxLines = 1,
                                    )
                                }
                                if (line.substituteTeacher != null) {
                                    Text(
                                        text = line.substituteTeacher,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorSubstituted,
                                        maxLines = 1,
                                    )
                                } else if (line.detail != null && !line.isCancelled) {
                                    Text(
                                        text = line.detail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = lesson.startTime,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

// ── Status & upcoming helpers ─────────────────────────────────────────────────

sealed interface LessonStatus {
    data class InClass(val lines: List<SubjectLine>) : LessonStatus
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
    val teacher: String?,
    val isCancelled: Boolean = false,
    val isMovedHere: Boolean = false,
    val substituteTeacher: String? = null,
)

private fun buildTodaySlots(
    lessons: List<EnrichedLesson>,
    movedLessons: List<MovedLessonWithRelations>,
    substitutions: List<SubstitutionWithRelations>,
    teacherMap: Map<String, Teacher>,
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

    val activeSubs = substitutions
        .filter { it.substitution.date.startsWith(today.toString()) }
        .flatMap { sub -> sub.lessons.map { id -> id to sub } }
        .toMap()

    val base = lessons.filter { lesson ->
        lesson.day.days.filterNotNull().any { it == dayNum } && lesson.id !in movedAwayIds
    }
    val extra = movedToday.mapNotNull { ml -> lessons.find { it.id in ml.lessons } }

    return (base + extra).mapNotNull { lesson ->
        val ml = movedToday.firstOrNull { lesson.id in it.lessons }
        val p: Period = ml?.period ?: lesson.period ?: return@mapNotNull null
        val start = runCatching { LocalTime.parse(p.startTime.take(5)) }.getOrNull() ?: return@mapNotNull null
        val end = runCatching { LocalTime.parse(p.endTime.take(5)) }.getOrNull() ?: return@mapNotNull null
        val subject = lesson.subject?.name ?: return@mapNotNull null
        val room = (ml?.classroom?.name ?: lesson.classrooms.firstOrNull()?.name)?.let { "Room $it" }
        val teacher = lesson.teachers.firstOrNull()?.name

        val sub = activeSubs[lesson.id]
        val isCancelled = sub != null && sub.teacher == null
        val isSubstituted = sub != null && sub.teacher != null
        val substituteTeacher = if (isSubstituted) {
            sub!!.substitution.substituter?.let { id -> teacherMap[id] }
                ?.let { "${it.lastName} ${it.firstName}" }
                ?: sub.teacher?.let { "${it.lastName} ${it.firstName}" }
        } else null

        Slot(
            start = start,
            end = end,
            subject = subject,
            room = room,
            teacher = teacher,
            isCancelled = isCancelled,
            isMovedHere = ml != null,
            substituteTeacher = substituteTeacher,
        )
    }.sortedBy { it.start }
}

private fun formatSlotGroupLines(slots: List<Slot>): List<SubjectLine> {
    val bySubject = slots.groupBy { it.subject }
    return if (bySubject.size == 1) {
        val teachers = slots.mapNotNull { it.teacher }.distinct()
        val rooms = slots.mapNotNull { it.room }.distinct()
        val detail = buildList {
            if (teachers.isNotEmpty()) add(teachers.joinToString(", "))
            if (rooms.isNotEmpty()) add(rooms.joinToString(", "))
        }.joinToString(" · ").takeIf { it.isNotEmpty() }
        val slot = slots.first()
        listOf(SubjectLine(
            name = slot.subject,
            detail = detail,
            isCancelled = slot.isCancelled,
            isMovedHere = slot.isMovedHere,
            substituteTeacher = slot.substituteTeacher,
        ))
    } else {
        slots.map { slot ->
            val detail = listOfNotNull(slot.teacher, slot.room).joinToString(" · ").takeIf { it.isNotEmpty() }
            SubjectLine(
                name = slot.subject,
                detail = detail,
                isCancelled = slot.isCancelled,
                isMovedHere = slot.isMovedHere,
                substituteTeacher = slot.substituteTeacher,
            )
        }
    }
}

@Composable
private fun rememberLessonStatus(now: LocalDateTime): LessonStatus {
    val lessons = TimetableState.lessons
    val movedLessons = TimetableState.movedLessons
    val substitutions = TimetableState.substitutions
    val teacherMap = TimetableState.teacherMap
    if (!TimetableState.timetableLoaded || lessons.isEmpty()) return LessonStatus.NoData

    val slots = buildTodaySlots(lessons, movedLessons, substitutions, teacherMap, now.date)
    if (slots.isEmpty()) return LessonStatus.NoData

    val time = now.time
    val currents = slots.filter { time >= it.start && time <= it.end }
    if (currents.isNotEmpty()) return LessonStatus.InClass(formatSlotGroupLines(currents))

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
    val substitutions = TimetableState.substitutions
    val teacherMap = TimetableState.teacherMap
    if (!TimetableState.timetableLoaded || lessons.isEmpty()) return emptyList()

    val slots = buildTodaySlots(lessons, movedLessons, substitutions, teacherMap, now.date)
    val time = now.time
    val future = slots.filter { it.start > time }

    val groups = future.groupBy { it.start }.entries.sortedBy { it.key }

    return groups.take(limit).map { (start, group) ->
        UpcomingLesson(formatSlotGroupLines(group), start.toString().take(5))
    }
}
