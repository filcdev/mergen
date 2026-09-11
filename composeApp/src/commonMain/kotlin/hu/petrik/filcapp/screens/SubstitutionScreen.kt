package hu.petrik.filcapp.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.filled.SwapCalls
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
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.components.SearchableSelection
import hu.petrik.filcapp.components.TimetableFilterChips
import hu.petrik.filcapp.network.CohortDto
import hu.petrik.filcapp.network.FilcPublicApi
import hu.petrik.filcapp.network.MovedLessonItemDto
import hu.petrik.filcapp.network.NamedRefDto
import hu.petrik.filcapp.network.SubstitutionItemDto
import hu.petrik.filcapp.network.SubstitutionLessonDto
import hu.petrik.filcapp.network.TeacherDto
import hu.petrik.filcapp.network.TimetableFilter
import hu.petrik.filcapp.settings.tr
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun SubstitutionScreen() {
    var substitutions by remember { mutableStateOf<List<SubstitutionItemDto>>(emptyList()) }
    var movedLessons by remember { mutableStateOf<List<MovedLessonItemDto>>(emptyList()) }
    var cohorts by remember { mutableStateOf<List<CohortDto>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<TeacherDto>>(emptyList()) }
    var classrooms by remember { mutableStateOf<List<NamedRefDto>>(emptyList()) }
    var filter by remember { mutableStateOf(TimetableFilter.COHORT) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var personalizedUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        try {
            val latest = FilcPublicApi.getLatestValidTimetable()
            cohorts = FilcPublicApi.getCohorts(latest.id).sortedBy { it.name }
            teachers = FilcPublicApi.getTeachers().sortedBy { it.displayName }
            classrooms = FilcPublicApi.getClassrooms().sortedBy { it.name }
            substitutions = FilcPublicApi.getSubstitutions().filter { isTodayOrFuture(it.substitution.date) }
            movedLessons = FilcPublicApi.getMovedLessons().filter { isTodayOrFuture(it.movedLesson.date) }
        } catch (throwable: Throwable) {
            error = throwable.message ?: tr("Nem sikerült betölteni a helyettesítéseket.", "Could not load substitutions.")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(AuthState.user?.id, AuthState.profile, cohorts, teachers) {
        val userId = AuthState.user?.id
        if (userId == null) {
            personalizedUserId = null
        } else if (personalizedUserId != userId) {
            val profile = AuthState.profile
            val teacherId = profile?.teacher?.id
            val cohortId = profile?.cohort?.id
            when {
                teacherId != null && teachers.any { it.id == teacherId } -> {
                    filter = TimetableFilter.TEACHER
                    selectedId = teacherId
                    personalizedUserId = userId
                }
                cohortId != null && cohorts.any { it.id == cohortId } -> {
                    filter = TimetableFilter.COHORT
                    selectedId = cohortId
                    personalizedUserId = userId
                }
            }
        }
    }

    val filterOptions =
        when (filter) {
            TimetableFilter.COHORT -> cohorts.map { it.id to it.name }
            TimetableFilter.TEACHER -> teachers.map { it.id to it.displayName }
            TimetableFilter.CLASSROOM -> classrooms.map { it.id to displayName(it) }
        }

    val filteredSubstitutions = filterSubstitutions(substitutions, filter, selectedId, cohorts)
    val filteredMovedLessons = filterMovedLessons(movedLessons, filter, selectedId)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.SwapCalls, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        tr("Helyettesítések", "Substitutions"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        tr(
                            "Aktuális helyettesítések, elmaradó és áthelyezett órák.",
                            "Current substitutions, cancelled and moved lessons.",
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        error?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(tr("Hiba történt", "Something went wrong"), style = MaterialTheme.typography.titleMedium)
                    Text(message)
                    Button(onClick = { reloadKey++ }) {
                        Text(tr("Újrapróbálás", "Retry"))
                    }
                }
            }
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
                        selectedId = null
                    },
                )
                SearchableSelection(
                    options = filterOptions,
                    selectedId = selectedId,
                    label = filterLabel(filter),
                    placeholder = tr("Szűrés nélkül minden látszik", "Without a filter everything is shown"),
                    onSelected = { selectedId = it },
                    allowClear = true,
                )
            }
        }

        if (selectedId != null) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = true,
                    onClick = { selectedId = null },
                    label = { Text(tr("Szűrés aktív – törlés", "Filter active – clear")) },
                )
            }
        }

        if (error == null && filteredSubstitutions.isEmpty() && filteredMovedLessons.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    tr(
                        "Nincs aktuális vagy közelgő helyettesítés ehhez a szűréshez.",
                        "No current or upcoming substitutions for this filter.",
                    ),
                    modifier = Modifier.padding(18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val dates =
            (filteredSubstitutions.map { normalizeDate(it.substitution.date) } +
                filteredMovedLessons.map { normalizeDate(it.movedLesson.date) })
                .distinct()
                .sorted()

        dates.forEach { date ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatDate(date), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                filteredSubstitutions
                    .filter { normalizeDate(it.substitution.date) == date }
                    .forEach { substitution -> SubstitutionCard(substitution) }

                filteredMovedLessons
                    .filter { normalizeDate(it.movedLesson.date) == date }
                    .forEach { movedLesson -> MovedLessonCard(movedLesson) }
            }
        }
    }
}

@Composable
private fun SubstitutionCard(item: SubstitutionItemDto) {
    val substitute = item.teacher?.displayName.orEmpty()
    val isCancelled = item.substitution.substituter == null || substitute.isBlank()

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (isCancelled) Icons.Default.Cancel else Icons.Default.SwapCalls,
                    contentDescription = null,
                    tint = if (isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        if (isCancelled) tr("Elmaradó óra", "Cancelled lesson") else tr("Helyettesítés", "Substitution"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!isCancelled) {
                        Text(
                            tr("Helyettesítő: $substitute", "Substitute: $substitute"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item.lessons.forEachIndexed { index, lesson ->
                SubstitutionLessonDetails(lesson)
                if (index != item.lessons.lastIndex) {
                    HorizontalDivider()
                }
            }

            item.substitution.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        tr("Megjegyzés: $comment", "Note: $comment"),
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubstitutionLessonDetails(lesson: SubstitutionLessonDto) {
    val subject = lesson.subject?.name ?: lesson.subject?.short ?: tr("Ismeretlen tantárgy", "Unknown subject")
    val originalTeachers = lesson.teachers.joinToString(", ") { displayName(it) }
    val classrooms = lesson.classrooms.joinToString(", ") { displayName(it) }
    val cohorts = lesson.cohorts.joinToString(", ")
    val period = lesson.period

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            period?.let { Text("${it.period}.", style = MaterialTheme.typography.labelLarge) }
        }
        period?.let {
            Text(
                "${formatTime(it.startTime)}–${formatTime(it.endTime)}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        if (cohorts.isNotBlank()) DetailLine(tr("Osztály", "Class"), cohorts)
        if (classrooms.isNotBlank()) DetailLine(tr("Terem", "Classroom"), classrooms)
        if (originalTeachers.isNotBlank()) DetailLine(tr("Eredeti tanár", "Original teacher"), originalTeachers)
    }
}

@Composable
private fun MovedLessonCard(item: MovedLessonItemDto) {
    val subjects = item.lessonNames.joinToString(", ").ifBlank { tr("Áthelyezett óra", "Moved lesson") }
    val room = item.classroom?.let(::displayName).orEmpty()

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.MoveUp, null, tint = MaterialTheme.colorScheme.tertiary)
                Text(tr("Áthelyezett óra", "Moved lesson"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(subjects, style = MaterialTheme.typography.titleSmall)
            item.dayDefinition?.name?.takeIf { it.isNotBlank() }?.let {
                DetailLine(tr("Nap", "Day"), it)
            }
            item.period?.let {
                DetailLine(
                    tr("Időpont", "Time"),
                    "${it.period}. • ${formatTime(it.startTime)}–${formatTime(it.endTime)}",
                )
            }
            if (room.isNotBlank()) DetailLine(tr("Terem", "Classroom"), room)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label:", fontWeight = FontWeight.Medium)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun filterSubstitutions(
    data: List<SubstitutionItemDto>,
    filter: TimetableFilter,
    selectionId: String?,
    cohorts: List<CohortDto>,
): List<SubstitutionItemDto> {
    if (selectionId == null) {
        return data
    }

    val cohortName = cohorts.firstOrNull { it.id == selectionId }?.name
    return data.filter { item ->
        if (filter == TimetableFilter.TEACHER && item.teacher?.id == selectionId) {
            return@filter true
        }
        item.lessons.any { lesson ->
            when (filter) {
                TimetableFilter.COHORT -> cohortName != null && lesson.cohorts.contains(cohortName)
                TimetableFilter.TEACHER -> lesson.teachers.any { it.id == selectionId }
                TimetableFilter.CLASSROOM -> lesson.classrooms.any { it.id == selectionId }
            }
        }
    }
}

private fun filterMovedLessons(
    data: List<MovedLessonItemDto>,
    filter: TimetableFilter,
    selectionId: String?,
): List<MovedLessonItemDto> {
    if (selectionId == null) {
        return data
    }
    if (filter != TimetableFilter.CLASSROOM) {
        return emptyList()
    }
    return data.filter { it.classroom?.id == selectionId }
}

private fun filterLabel(filter: TimetableFilter): String =
    when (filter) {
        TimetableFilter.COHORT -> tr("Osztály szűrése", "Filter by class")
        TimetableFilter.TEACHER -> tr("Tanár szűrése", "Filter by teacher")
        TimetableFilter.CLASSROOM -> tr("Terem szűrése", "Filter by classroom")
    }

private fun normalizeDate(value: String): String = value.take(10)

private fun formatDate(value: String): String {
    val parts = normalizeDate(value).split("-")
    return if (parts.size == 3) "${parts[0]}. ${parts[1]}. ${parts[2]}." else value
}

@OptIn(ExperimentalTime::class)
private fun isTodayOrFuture(value: String): Boolean =
    runCatching {
        val date = LocalDate.parse(normalizeDate(value))
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        date >= today
    }.getOrDefault(true)

private fun formatTime(value: String): String = value.take(5)

private fun displayName(item: NamedRefDto): String = item.short.ifBlank { item.name }

object SubstitutionTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = tr("Helyettesítés", "Substitutions")
            val icon = rememberVectorPainter(Icons.Default.SwapCalls)
            return remember(title) {
                TabOptions(index = 2u, title = title, icon = icon)
            }
        }

    @Composable
    override fun Content() {
        SubstitutionScreen()
    }
}
