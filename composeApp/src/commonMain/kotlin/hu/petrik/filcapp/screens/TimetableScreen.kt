package hu.petrik.filcapp.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import hu.petrik.filcapp.components.SearchableSelection
import hu.petrik.filcapp.components.TimetableFilterChips
import hu.petrik.filcapp.network.CohortDto
import hu.petrik.filcapp.network.FilcPublicApi
import hu.petrik.filcapp.network.LessonDto
import hu.petrik.filcapp.network.NamedRefDto
import hu.petrik.filcapp.network.TeacherDto
import hu.petrik.filcapp.network.TimetableDto
import hu.petrik.filcapp.network.TimetableFilter
import hu.petrik.filcapp.network.WeekDefinitionDto
import hu.petrik.filcapp.settings.tr
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private enum class TimetableViewMode {
    LIST,
    WEEK,
}

@Composable
fun TimetableScreen() {
    var timetables by remember { mutableStateOf<List<TimetableDto>>(emptyList()) }
    var selectedTimetableId by remember { mutableStateOf<String?>(null) }
    var cohorts by remember { mutableStateOf<List<CohortDto>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<TeacherDto>>(emptyList()) }
    var classrooms by remember { mutableStateOf<List<NamedRefDto>>(emptyList()) }
    var filter by remember { mutableStateOf(TimetableFilter.COHORT) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var lessons by remember { mutableStateOf<List<LessonDto>>(emptyList()) }
    var selectedWeekId by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf(TimetableViewMode.LIST) }
    var loadingReferenceData by remember { mutableStateOf(true) }
    var loadingLessons by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loadingReferenceData = true
        error = null
        try {
            val latest = FilcPublicApi.getLatestValidTimetable()
            val allTimetables = FilcPublicApi.getTimetables().filter(::isVisibleTimetable)
            val loadedTeachers = FilcPublicApi.getTeachers()
            val loadedClassrooms = FilcPublicApi.getClassrooms()

            timetables =
                (allTimetables + latest)
                    .distinctBy { it.id }
                    .sortedBy { it.validFrom.orEmpty() }
            teachers = loadedTeachers.sortedBy { it.displayName }
            classrooms = loadedClassrooms.sortedBy { it.name }
            selectedTimetableId = latest.id
        } catch (throwable: Throwable) {
            error = throwable.message ?: tr("Nem sikerült betölteni az órarendet.", "Could not load timetable.")
        } finally {
            loadingReferenceData = false
        }
    }

    LaunchedEffect(selectedTimetableId, reloadKey) {
        val timetableId = selectedTimetableId ?: return@LaunchedEffect
        try {
            cohorts = FilcPublicApi.getCohorts(timetableId).sortedBy { it.name }
            if (filter == TimetableFilter.COHORT) {
                selectedId = cohorts.firstOrNull()?.id
            }
            selectedWeekId = null
        } catch (throwable: Throwable) {
            error = throwable.message ?: tr("Nem sikerült betölteni az osztályokat.", "Could not load classes.")
        }
    }

    LaunchedEffect(filter, selectedId, selectedTimetableId, reloadKey) {
        val timetableId = selectedTimetableId ?: return@LaunchedEffect
        val selectionId = selectedId ?: return@LaunchedEffect

        loadingLessons = true
        error = null
        try {
            lessons =
                FilcPublicApi.getLessons(
                    filter = filter,
                    selectionId = selectionId,
                    timetableId = timetableId,
                )
            val availableWeekIds = lessons.mapNotNull { it.weekDefinition?.id }.toSet()
            if (selectedWeekId != null && selectedWeekId !in availableWeekIds) {
                selectedWeekId = null
            }
        } catch (throwable: Throwable) {
            lessons = emptyList()
            error = throwable.message ?: tr("Nem sikerült betölteni az órákat.", "Could not load lessons.")
        } finally {
            loadingLessons = false
        }
    }

    val activeTimetable = timetables.firstOrNull { it.id == selectedTimetableId }
    val filterOptions =
        when (filter) {
            TimetableFilter.COHORT -> cohorts.map { it.id to it.name }
            TimetableFilter.TEACHER -> teachers.map { it.id to it.displayName }
            TimetableFilter.CLASSROOM -> classrooms.map { it.id to displayName(it) }
        }
    val weekDefinitions = lessons.mapNotNull { it.weekDefinition }.distinctBy { it.id }.sortedBy { it.name }
    val visibleLessons =
        selectedWeekId?.let { weekId -> lessons.filter { it.weekDefinition?.id == weekId } } ?: lessons

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TimetableHeader(activeTimetable)

        if (loadingReferenceData) {
            LoadingBlock()
            return@Column
        }

        error?.let { message ->
            ErrorBlock(message = message, onRetry = { reloadKey++ })
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimetableFilterChips(
                    selected = filter,
                    onSelected = { newFilter ->
                        filter = newFilter
                        selectedWeekId = null
                        selectedId =
                            when (newFilter) {
                                TimetableFilter.COHORT -> cohorts.firstOrNull()?.id
                                TimetableFilter.TEACHER -> teachers.firstOrNull()?.id
                                TimetableFilter.CLASSROOM -> classrooms.firstOrNull()?.id
                            }
                    },
                )

                SearchableSelection(
                    options = filterOptions,
                    selectedId = selectedId,
                    label = filterLabel(filter),
                    placeholder = tr("Kezdj el gépelni a kereséshez...", "Type to search..."),
                    onSelected = { selectedId = it },
                )

                if (timetables.size > 1) {
                    SearchableSelection(
                        options = timetables.map { it.id to timetableLabel(it) },
                        selectedId = selectedTimetableId,
                        label = tr("Órarend verzió", "Timetable version"),
                        placeholder = tr("Válassz órarendet", "Select timetable"),
                        onSelected = { id -> if (id != null) selectedTimetableId = id },
                    )
                }
            }
        }

        if (weekDefinitions.size > 1) {
            WeekSelector(
                weeks = weekDefinitions,
                selectedWeekId = selectedWeekId,
                onSelected = { selectedWeekId = it },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = viewMode == TimetableViewMode.LIST,
                onClick = { viewMode = TimetableViewMode.LIST },
                leadingIcon = { Icon(Icons.Default.TableRows, null) },
                label = { Text(tr("Lista", "List")) },
            )
            FilterChip(
                selected = viewMode == TimetableViewMode.WEEK,
                onClick = { viewMode = TimetableViewMode.WEEK },
                leadingIcon = { Icon(Icons.Default.ViewWeek, null) },
                label = { Text(tr("Heti nézet", "Week view")) },
            )
        }

        when {
            loadingLessons -> LoadingBlock()
            selectedId != null && visibleLessons.isEmpty() && error == null -> {
                EmptyTimetableBlock()
            }

            viewMode == TimetableViewMode.WEEK -> TimetableWeekView(visibleLessons)
            else -> TimetableListView(visibleLessons)
        }
    }
}

@Composable
private fun TimetableHeader(timetable: TimetableDto?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    tr("Órarend", "Timetable"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                timetable?.let {
                    Text(
                        it.name.ifBlank { tr("Aktív órarend", "Active timetable") },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    timetableValidity(it)?.let { validity ->
                        Text(
                            validity,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekSelector(
    weeks: List<WeekDefinitionDto>,
    selectedWeekId: String?,
    onSelected: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedWeekId == null,
            onClick = { onSelected(null) },
            label = { Text(tr("Minden hét", "All weeks")) },
        )
        weeks.forEach { week ->
            FilterChip(
                selected = selectedWeekId == week.id,
                onClick = { onSelected(week.id) },
                label = { Text(week.short.ifBlank { week.name }) },
            )
        }
    }
}

@Composable
private fun TimetableListView(lessons: List<LessonDto>) {
    val grouped = sortedLessons(lessons).groupBy { localizedDayName(it.day?.name.orEmpty()) }

    grouped.forEach { (day, dayLessons) ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(day, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            dayLessons.forEach { lesson -> LessonCard(lesson) }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TimetableWeekView(lessons: List<LessonDto>) {
    val grouped = sortedLessons(lessons).groupBy { localizedDayName(it.day?.name.orEmpty()) }

    if (grouped.isEmpty()) {
        EmptyTimetableBlock()
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        grouped.forEach { (day, dayLessons) ->
            Card(modifier = Modifier.width(240.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    dayLessons.forEach { lesson -> CompactLessonCard(lesson) }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: LessonDto) {
    val period = lesson.period
    val subjectName = lesson.subject?.name ?: lesson.subject?.short ?: tr("Ismeretlen tantárgy", "Unknown subject")
    val teachers = lesson.teachers.joinToString(", ") { displayName(it) }
    val classrooms = lesson.classrooms.joinToString(", ") { displayName(it) }
    val cohorts = lesson.cohorts.joinToString(", ") { it.short.ifBlank { it.name } }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                period?.let { PeriodBadge(it.period) }
            }
            period?.let {
                Text(
                    "${formatTime(it.startTime)}–${formatTime(it.endTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (teachers.isNotBlank()) DetailLine(tr("Tanár", "Teacher"), teachers)
            if (classrooms.isNotBlank()) DetailLine(tr("Terem", "Classroom"), classrooms)
            if (cohorts.isNotBlank()) DetailLine(tr("Osztály", "Class"), cohorts)
            lesson.weekDefinition?.let { week ->
                if (week.short.isNotBlank() || week.name.isNotBlank()) {
                    Text(
                        week.short.ifBlank { week.name },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactLessonCard(lesson: LessonDto) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    lesson.subject?.short?.ifBlank { lesson.subject?.name.orEmpty() }
                        ?: tr("Óra", "Lesson"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                lesson.period?.let { Text("${it.period}.", style = MaterialTheme.typography.labelMedium) }
            }
            lesson.period?.let {
                Text(
                    "${formatTime(it.startTime)}–${formatTime(it.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val room = lesson.classrooms.joinToString(", ") { displayName(it) }
            if (room.isNotBlank()) {
                Text(room, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PeriodBadge(period: Int) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            if (period > 0) "$period." else "–",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyTimetableBlock() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            tr("Ehhez a kiválasztáshoz nincs megjeleníthető óra.", "No lessons for this selection."),
            modifier = Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBlock(
    message: String,
    onRetry: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(tr("Hiba történt", "Something went wrong"), style = MaterialTheme.typography.titleMedium)
            Text(message)
            Button(onClick = onRetry) {
                Text(tr("Újrapróbálás", "Retry"))
            }
        }
    }
}

private fun sortedLessons(lessons: List<LessonDto>): List<LessonDto> =
    lessons.sortedWith(
        compareBy(
            { dayOrder(it.day?.name.orEmpty()) },
            { it.period?.period ?: Int.MAX_VALUE },
            { it.period?.startTime.orEmpty() },
        ),
    )

private fun filterLabel(filter: TimetableFilter): String =
    when (filter) {
        TimetableFilter.COHORT -> tr("Osztály keresése", "Find class")
        TimetableFilter.TEACHER -> tr("Tanár keresése", "Find teacher")
        TimetableFilter.CLASSROOM -> tr("Terem keresése", "Find classroom")
    }

private fun timetableLabel(timetable: TimetableDto): String =
    timetable.name.ifBlank {
        timetable.validFrom?.let { tr("Órarend – ${formatDate(it)}", "Timetable – ${formatDate(it)}") }
            ?: tr("Órarend", "Timetable")
    }

private fun timetableValidity(timetable: TimetableDto): String? {
    val from = timetable.validFrom?.let(::formatDate)
    val to = timetable.validTo?.let(::formatDate)
    return when {
        from != null && to != null -> "$from – $to"
        from != null -> tr("Érvényes ettől: $from", "Valid from: $from")
        to != null -> tr("Érvényes eddig: $to", "Valid until: $to")
        else -> null
    }
}

private fun localizedDayName(day: String): String =
    when (dayOrder(day)) {
        1 -> tr("Hétfő", "Monday")
        2 -> tr("Kedd", "Tuesday")
        3 -> tr("Szerda", "Wednesday")
        4 -> tr("Csütörtök", "Thursday")
        5 -> tr("Péntek", "Friday")
        6 -> tr("Szombat", "Saturday")
        7 -> tr("Vasárnap", "Sunday")
        else -> day.ifBlank { tr("Egyéb", "Other") }
    }

private fun dayOrder(day: String): Int {
    val normalized = day.lowercase()
    return when {
        normalized.contains("hétf") || normalized.contains("hetf") || normalized.contains("monday") -> 1
        normalized.contains("kedd") || normalized.contains("tuesday") -> 2
        normalized.contains("szerda") || normalized.contains("wednesday") -> 3
        normalized.contains("csüt") || normalized.contains("csut") || normalized.contains("thursday") -> 4
        normalized.contains("pént") || normalized.contains("pent") || normalized.contains("friday") -> 5
        normalized.contains("szomb") || normalized.contains("saturday") -> 6
        normalized.contains("vasár") || normalized.contains("vasar") || normalized.contains("sunday") -> 7
        else -> 99
    }
}

@OptIn(ExperimentalTime::class)
private fun isVisibleTimetable(timetable: TimetableDto): Boolean =
    runCatching {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val validTo = timetable.validTo?.let { LocalDate.parse(it.take(10)) }
        validTo == null || validTo >= today
    }.getOrDefault(true)

private fun formatDate(value: String): String {
    val parts = value.take(10).split("-")
    return if (parts.size == 3) "${parts[0]}. ${parts[1]}. ${parts[2]}." else value.take(10)
}

private fun formatTime(value: String): String = value.take(5)

private fun displayName(item: NamedRefDto): String = item.short.ifBlank { item.name }

object TimetableTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = tr("Órarend", "Timetable")
            val icon = rememberVectorPainter(Icons.Default.CalendarMonth)
            return remember(title) {
                TabOptions(index = 1u, title = title, icon = icon)
            }
        }

    @Composable
    override fun Content() {
        TimetableScreen()
    }
}
