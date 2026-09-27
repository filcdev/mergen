package hu.petrik.filcapp.screens
import androidx.compose.foundation.background

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

private object SubstitutionSessionCache {
    var substitutions: List<SubstitutionItemDto> = emptyList()
    var movedLessons: List<MovedLessonItemDto> = emptyList()
    var cohorts: List<CohortDto> = emptyList()
    var teachers: List<TeacherDto> = emptyList()
    var classrooms: List<NamedRefDto> = emptyList()
    var loaded = false
    var error: String? = null
}

@Composable
fun SubstitutionScreen() {
    var substitutions by remember { mutableStateOf(SubstitutionSessionCache.substitutions) }
    var movedLessons by remember { mutableStateOf(SubstitutionSessionCache.movedLessons) }
    var cohorts by remember { mutableStateOf(SubstitutionSessionCache.cohorts) }
    var teachers by remember { mutableStateOf(SubstitutionSessionCache.teachers) }
    var classrooms by remember { mutableStateOf(SubstitutionSessionCache.classrooms) }
    var filter by remember { mutableStateOf(TimetableFilter.COHORT) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(!SubstitutionSessionCache.loaded) }
    var error by remember { mutableStateOf(SubstitutionSessionCache.error) }
    var reloadKey by remember { mutableStateOf(0) }
    var personalizedUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        if (reloadKey == 0 && SubstitutionSessionCache.loaded) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            val latest = FilcPublicApi.getLatestValidTimetable()
            cohorts = FilcPublicApi.getCohorts(latest.id).sortedBy { it.name }
            teachers = FilcPublicApi.getTeachers().sortedBy { it.displayName }
            classrooms = FilcPublicApi.getClassrooms().sortedBy { it.name }
            substitutions = FilcPublicApi.getSubstitutions().filter { isTodayOrFuture(it.substitution.date) }
            movedLessons = FilcPublicApi.getMovedLessons().filter { isTodayOrFuture(it.movedLesson.date) }
            SubstitutionSessionCache.cohorts = cohorts
            SubstitutionSessionCache.teachers = teachers
            SubstitutionSessionCache.classrooms = classrooms
            SubstitutionSessionCache.substitutions = substitutions
            SubstitutionSessionCache.movedLessons = movedLessons
            SubstitutionSessionCache.error = null
        } catch (throwable: Throwable) {
            error = throwable.message ?: tr("Nem sikerült betölteni a helyettesítéseket.", "Could not load substitutions.")
            SubstitutionSessionCache.error = error
        } finally {
            SubstitutionSessionCache.loaded = true
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
    val filteredMovedLessons = filterMovedLessons(movedLessons, filter, selectedId, cohorts)

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
            SubstitutionOverviewHeader(
                changeCount = filteredSubstitutions.size + filteredMovedLessons.size,
            )

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

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                (
                    filteredSubstitutions.map { normalizeDate(it.substitution.date) } +
                        filteredMovedLessons.map { normalizeDate(it.movedLesson.date) }
                )
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
}

@OptIn(ExperimentalTime::class)
@Composable
private fun SubstitutionOverviewHeader(changeCount: Int) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = tr("HIVATALOS NAPLÓ", "OFFICIAL LOG"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatHungarianDate(today),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(7.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
                Text(
                    text =
                        if (changeCount == 1) {
                            tr("1 változás ma", "1 change today")
                        } else {
                            tr("$changeCount változás ma", "$changeCount changes today")
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SubstitutionCard(item: SubstitutionItemDto) {
    val substitute = item.teacher?.displayName.orEmpty()
    val isCancelled = item.substitution.substituter == null || substitute.isBlank()
    val firstLesson = item.lessons.firstOrNull()
    val period = firstLesson?.period
    val subject =
        firstLesson?.subject?.name
            ?: firstLesson?.subject?.short
            ?: tr("Ismeretlen tantárgy", "Unknown subject")
    val originalTeachers =
        firstLesson
            ?.teachers
            ?.joinToString(", ") { displayName(it) }
            .orEmpty()
    val classrooms =
        firstLesson
            ?.classrooms
            ?.joinToString(", ") { displayName(it) }
            .orEmpty()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = period?.period?.takeIf { it > 0 }?.let { "$it." } ?: "–",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color =
                                if (isCancelled) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    period?.let {
                        Text(
                            text = "${formatTime(it.startTime)} – ${formatTime(it.endTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                StatusPill(
                    cancelled = isCancelled,
                    label =
                        if (isCancelled) {
                            tr("Elmarad", "Cancelled")
                        } else {
                            tr("Helyettesítés", "Substitution")
                        },
                )
            }

            if (!isCancelled && substitute.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (originalTeachers.isNotBlank()) {
                            Text(
                                text = originalTeachers,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "→",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = substitute,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (classrooms.isNotBlank()) {
                Text(
                    text = classrooms,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (item.lessons.size > 1) {
                item.lessons.drop(1).forEach { lesson ->
                    HorizontalDivider()
                    SubstitutionLessonDetails(lesson)
                }
            }

            item.substitution.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    cancelled: Boolean,
    label: String,
) {
    val color =
        if (cancelled) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.tertiary
        }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
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
    val subjects =
        item.lessons
            .mapNotNull { lesson -> lesson.subject?.name ?: lesson.subject?.short }
            .distinct()
            .joinToString(", ")
            .ifBlank {
                item.lessonNames
                    .joinToString(", ")
                    .ifBlank { tr("Áthelyezett óra", "Moved lesson") }
            }
    val room = item.classroom?.let(::displayName).orEmpty()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.period?.period?.takeIf { it > 0 }?.let { "$it." } ?: "↕",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = subjects,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                item.period?.let {
                    Text(
                        text = "${formatTime(it.startTime)} – ${formatTime(it.endTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (room.isNotBlank()) {
                    Text(
                        text = room,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Text(
                    text = tr("Áthelyezve", "Moved"),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
) {
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
    cohorts: List<CohortDto>,
): List<MovedLessonItemDto> {
    if (selectionId == null) {
        return data
    }

    val cohortName = cohorts.firstOrNull { it.id == selectionId }?.name

    return data.filter { item ->
        when (filter) {
            TimetableFilter.COHORT ->
                cohortName != null &&
                    item.lessons.any { lesson ->
                        lesson.cohorts.contains(cohortName)
                    }

            TimetableFilter.TEACHER ->
                item.lessons.any { lesson ->
                    lesson.teachers.any { it.id == selectionId }
                }

            TimetableFilter.CLASSROOM ->
                item.classroom?.id == selectionId ||
                    item.lessons.any { lesson ->
                        lesson.classrooms.any { it.id == selectionId }
                    }
        }
    }
}

private fun filterLabel(filter: TimetableFilter): String =
    when (filter) {
        TimetableFilter.COHORT -> tr("Osztály szűrése", "Filter by class")
        TimetableFilter.TEACHER -> tr("Tanár szűrése", "Filter by teacher")
        TimetableFilter.CLASSROOM -> tr("Terem szűrése", "Filter by classroom")
    }

private fun formatHungarianDate(date: LocalDate): String {
    val dayName =
        when (date.dayOfWeek) {
            kotlinx.datetime.DayOfWeek.MONDAY -> "Hétfő"
            kotlinx.datetime.DayOfWeek.TUESDAY -> "Kedd"
            kotlinx.datetime.DayOfWeek.WEDNESDAY -> "Szerda"
            kotlinx.datetime.DayOfWeek.THURSDAY -> "Csütörtök"
            kotlinx.datetime.DayOfWeek.FRIDAY -> "Péntek"
            kotlinx.datetime.DayOfWeek.SATURDAY -> "Szombat"
            kotlinx.datetime.DayOfWeek.SUNDAY -> "Vasárnap"
        }

    return "${date.year}. ${date.monthNumber}. ${date.day}. $dayName"
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
