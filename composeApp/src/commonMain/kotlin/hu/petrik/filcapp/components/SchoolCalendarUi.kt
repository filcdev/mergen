package hu.petrik.filcapp.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.calendar.SchoolCalendarEvent
import hu.petrik.filcapp.settings.tr
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun SchoolCalendarCard(
    events: List<SchoolCalendarEvent>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.now().toLocalDateTime(PETRIK_TIME_ZONE).date }
    var weekStart by remember { mutableStateOf(startOfWeek(today)) }
    var selectedDate by remember { mutableStateOf(today) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = tr("Iskolai naptár", "School calendar"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            TextButton(onClick = onOpenCalendar) {
                Text(tr("Összes", "All"))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 7.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WeekNavigation(
                    weekStart = weekStart,
                    onPrevious = {
                        weekStart = addDays(weekStart, -7)
                        selectedDate = weekStart
                    },
                    onNext = {
                        weekStart = addDays(weekStart, 7)
                        selectedDate = weekStart
                    },
                )

                WeekStrip(
                    weekStart = weekStart,
                    selectedDate = selectedDate,
                    today = today,
                    events = events,
                    onSelect = { selectedDate = it },
                )

                when {
                    loading -> CalendarLoading()
                    error != null && events.isEmpty() -> CalendarError(error, onRetry)
                    else -> {
                        val selectedEvents =
                            remember(events, selectedDate) {
                                events
                                    .filter { it.occursOn(selectedDate) }
                                    .sortedBy { it.start }
                            }

                        Text(
                            text = formatSelectedDate(selectedDate),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (selectedEvents.isEmpty()) {
                            EmptyDayMessage(events = events, selectedDate = selectedDate)
                        } else {
                            selectedEvents.take(3).forEach { event ->
                                CalendarEventRow(event = event, compact = true)
                            }

                            if (selectedEvents.size > 3) {
                                TextButton(onClick = onOpenCalendar) {
                                    Text(
                                        tr(
                                            "+${selectedEvents.size - 3} további esemény",
                                            "+${selectedEvents.size - 3} more events",
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun SchoolCalendarScreen(
    events: List<SchoolCalendarEvent>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.now().toLocalDateTime(PETRIK_TIME_ZONE).date }
    var month by remember { mutableStateOf(MonthCursor(today.year, today.monthNumber)) }
    var selectedDate by remember { mutableStateOf(today) }

    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { onRetry() },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = tr("Vissza", "Back"))
                }
                Column {
                    Text(
                        text = tr("Iskolai naptár", "School calendar"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = tr("Petrik események", "Petrik events"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FilcPanel(modifier = Modifier.fillMaxWidth()) {
                MonthNavigation(
                    cursor = month,
                    onPrevious = {
                        month = month.previous()
                        selectedDate = LocalDate(month.year, month.month, 1)
                    },
                    onNext = {
                        month = month.next()
                        selectedDate = LocalDate(month.year, month.month, 1)
                    },
                )

                MonthGrid(
                    cursor = month,
                    selectedDate = selectedDate,
                    today = today,
                    events = events,
                    onSelect = { selectedDate = it },
                )
            }

            when {
                loading -> CalendarLoading()
                error != null && events.isEmpty() -> CalendarError(error, onRetry)
                else -> {
                    val selectedEvents =
                        remember(events, selectedDate) {
                            events
                                .filter { it.occursOn(selectedDate) }
                                .sortedBy { it.start }
                        }

                    FilcPanel(
                        modifier = Modifier.fillMaxWidth(),
                        title = formatSelectedDate(selectedDate),
                    ) {
                        if (selectedEvents.isEmpty()) {
                            EmptyDayMessage(events = events, selectedDate = selectedDate)
                        } else {
                            selectedEvents.forEach { event ->
                                CalendarEventRow(event = event, compact = false)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WeekNavigation(
    weekStart: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = tr("Előző hét", "Previous week"))
        }

        Text(
            text = weekRangeLabel(weekStart),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = tr("Következő hét", "Next week"))
        }
    }
}

@Composable
private fun WeekStrip(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    events: List<SchoolCalendarEvent>,
    onSelect: (LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(7) { offset ->
            val date = addDays(weekStart, offset)
            val selected = date == selectedDate
            val hasEvent = events.any { it.occursOn(date) }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .background(
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            shape = RoundedCornerShape(12.dp),
                        ).clickable { onSelect(date) }
                        .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = shortWeekday(date.dayOfWeek),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = date.day.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (date == today || selected) FontWeight.Bold else FontWeight.Normal,
                    color =
                        if (date == today || selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Box(
                    modifier =
                        Modifier
                            .size(5.dp)
                            .background(
                                color =
                                    if (hasEvent) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                shape = CircleShape,
                            ),
                )
            }
        }
    }
}

@Composable
private fun MonthNavigation(
    cursor: MonthCursor,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = tr("Előző hónap", "Previous month"))
        }
        Text(
            text = "${cursor.year}. ${monthName(cursor.month)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = tr("Következő hónap", "Next month"))
        }
    }
}

@Composable
private fun MonthGrid(
    cursor: MonthCursor,
    selectedDate: LocalDate,
    today: LocalDate,
    events: List<SchoolCalendarEvent>,
    onSelect: (LocalDate) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
        ).forEach { day ->
            Text(
                text = shortWeekday(day),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    val first = LocalDate(cursor.year, cursor.month, 1)
    val leading = weekdayIndex(first.dayOfWeek)
    val days = daysInMonth(cursor.year, cursor.month)
    val cellCount = ((leading + days + 6) / 7) * 7

    repeat(cellCount / 7) { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(7) { column ->
                val index = week * 7 + column
                val day = index - leading + 1
                if (day !in 1..days) {
                    Spacer(modifier = Modifier.weight(1f).height(52.dp))
                } else {
                    val date = LocalDate(cursor.year, cursor.month, day)
                    MonthDayCell(
                        date = date,
                        selected = date == selectedDate,
                        today = date == today,
                        hasEvent = events.any { it.occursOn(date) },
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .height(52.dp)
                .padding(2.dp)
                .background(
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    shape = RoundedCornerShape(10.dp),
                ).clickable(onClick = onClick)
                .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = date.day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal,
            color =
                if (today || selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
        Box(
            modifier =
                Modifier
                    .size(5.dp)
                    .background(
                        color =
                            if (hasEvent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        shape = CircleShape,
                    ),
        )
    }
}

@Composable
private fun CalendarEventRow(
    event: SchoolCalendarEvent,
    compact: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.width(64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = eventTimeLabel(event),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (compact) 2 else 4,
                    overflow = TextOverflow.Ellipsis,
                )

                event.location?.takeIf { it.isNotBlank() }?.let { location ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (!compact) {
                    event.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayMessage(
    events: List<SchoolCalendarEvent>,
    selectedDate: LocalDate,
) {
    val nextEvent =
        remember(events, selectedDate) {
            events.firstOrNull { it.start.date > selectedDate }
        }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = tr("Erre a napra nincs esemény.", "There are no events on this day."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        nextEvent?.let { event ->
            Text(
                text =
                    tr(
                        "Következő: ${formatShortDate(event.start.date)} – ${event.title}",
                        "Next: ${formatShortDate(event.start.date)} – ${event.title}",
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CalendarLoading() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        Text(
            text = tr("Naptár betöltése…", "Loading calendar…"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CalendarError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = tr("A naptár most nem tölthető be.", "The calendar could not be loaded."),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text(tr(" Újra", " Retry"))
        }
    }
}

@Composable
private fun eventTimeLabel(event: SchoolCalendarEvent): String {
    if (event.allDay) return tr("Egész nap", "All day")

    val start = "${event.start.hour.twoDigits()}:${event.start.minute.twoDigits()}"
    val end = event.end
    if (end == null || end.date != event.start.date) return start

    return "$start–${end.hour.twoDigits()}:${end.minute.twoDigits()}"
}

@Composable
private fun formatSelectedDate(date: LocalDate): String =
    tr(
        "${date.year}. ${monthName(date.monthNumber)} ${date.day}. (${longWeekday(date.dayOfWeek)})",
        "${longWeekday(date.dayOfWeek)}, ${monthName(date.monthNumber)} ${date.day}, ${date.year}",
    )

@Composable
private fun weekRangeLabel(weekStart: LocalDate): String {
    val weekEnd = addDays(weekStart, 6)
    return if (weekStart.monthNumber == weekEnd.monthNumber) {
        "${monthName(weekStart.monthNumber)} ${weekStart.day}–${weekEnd.day}."
    } else {
        "${monthName(weekStart.monthNumber)} ${weekStart.day}. – ${monthName(weekEnd.monthNumber)} ${weekEnd.day}."
    }
}

@Composable
private fun monthName(month: Int): String =
    when (month) {
        1 -> tr("január", "January")
        2 -> tr("február", "February")
        3 -> tr("március", "March")
        4 -> tr("április", "April")
        5 -> tr("május", "May")
        6 -> tr("június", "June")
        7 -> tr("július", "July")
        8 -> tr("augusztus", "August")
        9 -> tr("szeptember", "September")
        10 -> tr("október", "October")
        11 -> tr("november", "November")
        12 -> tr("december", "December")
        else -> ""
    }

@Composable
private fun shortWeekday(day: DayOfWeek): String =
    when (day) {
        DayOfWeek.MONDAY -> tr("H", "M")
        DayOfWeek.TUESDAY -> tr("K", "T")
        DayOfWeek.WEDNESDAY -> tr("Sze", "W")
        DayOfWeek.THURSDAY -> tr("Cs", "T")
        DayOfWeek.FRIDAY -> tr("P", "F")
        DayOfWeek.SATURDAY -> tr("Szo", "S")
        DayOfWeek.SUNDAY -> tr("V", "S")
    }

@Composable
private fun longWeekday(day: DayOfWeek): String =
    when (day) {
        DayOfWeek.MONDAY -> tr("hétfő", "Monday")
        DayOfWeek.TUESDAY -> tr("kedd", "Tuesday")
        DayOfWeek.WEDNESDAY -> tr("szerda", "Wednesday")
        DayOfWeek.THURSDAY -> tr("csütörtök", "Thursday")
        DayOfWeek.FRIDAY -> tr("péntek", "Friday")
        DayOfWeek.SATURDAY -> tr("szombat", "Saturday")
        DayOfWeek.SUNDAY -> tr("vasárnap", "Sunday")
    }

private data class MonthCursor(
    val year: Int,
    val month: Int,
) {
    fun previous(): MonthCursor = if (month == 1) MonthCursor(year - 1, 12) else MonthCursor(year, month - 1)

    fun next(): MonthCursor = if (month == 12) MonthCursor(year + 1, 1) else MonthCursor(year, month + 1)
}

private fun formatShortDate(date: LocalDate): String = "${date.year}.${date.monthNumber.twoDigits()}.${date.day.twoDigits()}."

private fun Int.twoDigits(): String = toString().padStart(2, '0')

private fun startOfWeek(date: LocalDate): LocalDate = addDays(date, -weekdayIndex(date.dayOfWeek))

private fun weekdayIndex(day: DayOfWeek): Int =
    when (day) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }

private fun addDays(
    source: LocalDate,
    amount: Int,
): LocalDate {
    var result = source
    if (amount >= 0) {
        repeat(amount) { result = nextDay(result) }
    } else {
        repeat(-amount) { result = previousDay(result) }
    }
    return result
}

private fun nextDay(date: LocalDate): LocalDate {
    val lastDay = daysInMonth(date.year, date.monthNumber)
    return when {
        date.day < lastDay -> LocalDate(date.year, date.monthNumber, date.day + 1)
        date.monthNumber < 12 -> LocalDate(date.year, date.monthNumber + 1, 1)
        else -> LocalDate(date.year + 1, 1, 1)
    }
}

private fun previousDay(date: LocalDate): LocalDate =
    when {
        date.day > 1 -> LocalDate(date.year, date.monthNumber, date.day - 1)
        date.monthNumber > 1 -> {
            val month = date.monthNumber - 1
            LocalDate(date.year, month, daysInMonth(date.year, month))
        }
        else -> LocalDate(date.year - 1, 12, 31)
    }

private fun daysInMonth(
    year: Int,
    month: Int,
): Int =
    when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }

private fun isLeapYear(year: Int): Boolean = year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)

private val PETRIK_TIME_ZONE = TimeZone.of("Europe/Budapest")
