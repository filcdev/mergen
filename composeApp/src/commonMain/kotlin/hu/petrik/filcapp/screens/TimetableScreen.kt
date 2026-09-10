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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.data.ChronosPublicApi
import hu.petrik.filcapp.data.CohortDto
import hu.petrik.filcapp.data.LessonDto
import hu.petrik.filcapp.data.NamedRefDto
import hu.petrik.filcapp.data.TeacherDto
import hu.petrik.filcapp.data.TimetableDto
import hu.petrik.filcapp.data.TimetableFilter

object TimetableTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Órarend"
            val icon = rememberVectorPainter(Icons.Default.CalendarMonth)
            return remember {
                TabOptions(index = 1u, title = title, icon = icon)
            }
        }

    @Composable
    override fun Content() {
        TimetableScreen()
    }
}

@Composable
fun TimetableScreen() {
    var timetable by remember { mutableStateOf<TimetableDto?>(null) }
    var cohorts by remember { mutableStateOf<List<CohortDto>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<TeacherDto>>(emptyList()) }
    var classrooms by remember { mutableStateOf<List<NamedRefDto>>(emptyList()) }
    var filter by remember { mutableStateOf(TimetableFilter.COHORT) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var lessons by remember { mutableStateOf<List<LessonDto>>(emptyList()) }
    var loadingReferenceData by remember { mutableStateOf(true) }
    var loadingLessons by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loadingReferenceData = true
        error = null
        try {
            val activeTimetable = ChronosPublicApi.getLatestValidTimetable()
            val loadedCohorts = ChronosPublicApi.getCohorts(activeTimetable.id)
            val loadedTeachers = ChronosPublicApi.getTeachers()
            val loadedClassrooms = ChronosPublicApi.getClassrooms()

            timetable = activeTimetable
            cohorts = loadedCohorts.sortedBy { it.name }
            teachers = loadedTeachers.sortedBy { it.displayName }
            classrooms = loadedClassrooms.sortedBy { it.name }
            selectedId = loadedCohorts.sortedBy { it.name }.firstOrNull()?.id
        } catch (throwable: Throwable) {
            error = throwable.message ?: "Nem sikerült betölteni az órarendet."
        } finally {
            loadingReferenceData = false
        }
    }

    LaunchedEffect(filter, selectedId, timetable?.id, reloadKey) {
        val activeTimetableId = timetable?.id ?: return@LaunchedEffect
        val activeSelectionId = selectedId ?: return@LaunchedEffect

        loadingLessons = true
        error = null
        try {
            lessons =
                ChronosPublicApi.getLessons(
                    filter = filter,
                    selectionId = activeSelectionId,
                    timetableId = activeTimetableId,
                )
        } catch (throwable: Throwable) {
            lessons = emptyList()
            error = throwable.message ?: "Nem sikerült betölteni az órákat."
        } finally {
            loadingLessons = false
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Órarend", style = MaterialTheme.typography.headlineSmall)
        timetable?.let {
            Text(
                text = it.name.ifBlank { "Aktív órarend" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (loadingReferenceData) {
            LoadingBlock()
            return@Column
        }

        error?.let { message ->
            ErrorBlock(message = message, onRetry = { reloadKey++ })
        }

        TimetableFilterRow(
            selected = filter,
            onSelected = { newFilter ->
                filter = newFilter
                selectedId =
                    when (newFilter) {
                        TimetableFilter.COHORT -> cohorts.firstOrNull()?.id
                        TimetableFilter.TEACHER -> teachers.firstOrNull()?.id
                        TimetableFilter.CLASSROOM -> classrooms.firstOrNull()?.id
                    }
            },
        )

        val options =
            when (filter) {
                TimetableFilter.COHORT -> cohorts.map { it.id to it.name }
                TimetableFilter.TEACHER -> teachers.map { it.id to it.displayName }
                TimetableFilter.CLASSROOM -> classrooms.map { it.id to displayName(it) }
            }

        SelectionDropdown(
            options = options,
            selectedId = selectedId,
            onSelected = { selectedId = it },
        )

        if (loadingLessons) {
            LoadingBlock()
        } else if (selectedId != null && lessons.isEmpty() && error == null) {
            Text(
                "Ehhez a kiválasztáshoz nincs megjeleníthető óra.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TimetableLessons(lessons)
        }
    }
}

@Composable
private fun TimetableFilterRow(
    selected: TimetableFilter,
    onSelected: (TimetableFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == TimetableFilter.COHORT,
            onClick = { onSelected(TimetableFilter.COHORT) },
            label = { Text("Osztály") },
        )
        FilterChip(
            selected = selected == TimetableFilter.TEACHER,
            onClick = { onSelected(TimetableFilter.TEACHER) },
            label = { Text("Tanár") },
        )
        FilterChip(
            selected = selected == TimetableFilter.CLASSROOM,
            onClick = { onSelected(TimetableFilter.CLASSROOM) },
            label = { Text("Terem") },
        )
    }
}

@Composable
private fun SelectionDropdown(
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = options.isNotEmpty(),
        ) {
            Text(selectedLabel ?: "Válassz...")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (id, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelected(id)
                    },
                )
            }
        }
    }
}

@Composable
private fun TimetableLessons(lessons: List<LessonDto>) {
    val grouped =
        lessons
            .sortedWith(
                compareBy<LessonDto>(
                    { dayOrder(it.day?.name.orEmpty()) },
                    { it.period?.period ?: Int.MAX_VALUE },
                    { it.period?.startTime.orEmpty() },
                ),
            ).groupBy { it.day?.name?.takeIf { name -> name.isNotBlank() } ?: "Egyéb" }

    grouped.forEach { (day, dayLessons) ->
        Text(day, style = MaterialTheme.typography.titleMedium)
        dayLessons.forEachIndexed { index, lesson ->
            LessonCard(lesson)
            if (index != dayLessons.lastIndex) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: LessonDto) {
    val period = lesson.period
    val subjectName = lesson.subject?.name ?: lesson.subject?.short ?: "Ismeretlen tantárgy"
    val teacherNames = lesson.teachers.joinToString(", ") { displayName(it) }
    val classroomNames = lesson.classrooms.joinToString(", ") { displayName(it) }
    val cohortNames = lesson.cohorts.joinToString(", ") { it.short.ifBlank { it.name } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(subjectName, style = MaterialTheme.typography.titleMedium)
                Text(
                    periodLabel(period?.period),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (period != null) {
                Text(
                    "${formatTime(period.startTime)}–${formatTime(period.endTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (teacherNames.isNotBlank()) {
                Text("Tanár: $teacherNames", style = MaterialTheme.typography.bodyMedium)
            }
            if (classroomNames.isNotBlank()) {
                Text("Terem: $classroomNames", style = MaterialTheme.typography.bodyMedium)
            }
            if (cohortNames.isNotBlank()) {
                Text("Osztály: $cohortNames", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
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
            Text("Hiba történt", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            Button(onClick = onRetry) {
                Text("Újrapróbálás")
            }
        }
    }
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

private fun periodLabel(period: Int?): String =
    if (period == null || period <= 0) "" else "$period. óra"

private fun formatTime(value: String): String = value.take(5)

private fun displayName(item: NamedRefDto): String = item.short.ifBlank { item.name }
