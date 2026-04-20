@file:OptIn(kotlin.time.ExperimentalTime::class)

package hu.petrik.filcapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.api.APIResult
import hu.petrik.filcapp.api.CohortApi
import hu.petrik.filcapp.api.LessonApi
import hu.petrik.filcapp.api.SubstitutionApi
import hu.petrik.filcapp.api.TeacherApi
import hu.petrik.filcapp.api.client.APIClient
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.models.Cohort
import hu.petrik.filcapp.models.EnrichedLesson
import hu.petrik.filcapp.models.SubstitutionWithRelations
import hu.petrik.filcapp.models.Teacher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.*

// ── Shared timetable state (readable by TopBar) ───────────────────────────────

object TimetableState {
    var cohorts by mutableStateOf<List<Cohort>>(emptyList())
    var cohortsLoaded by mutableStateOf(false)
    var cohortsError by mutableStateOf<String?>(null)
    var selectedCohortId by mutableStateOf<String?>(null)
}

// ── Tab entry point ──────────────────────────────────────────────────────────

object TimetableTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Default.CalendarMonth)
            return remember { TabOptions(index = 1u, title = "Timetable", icon = icon) }
        }

    @Composable
    override fun Content() {
        val model = rememberScreenModel {
            TimetableScreenModel(
                LessonApi(APIClient),
                CohortApi(APIClient),
                TeacherApi(APIClient),
                SubstitutionApi(APIClient),
            )
        }

        LaunchedEffect(Unit) { model.loadCohorts() }

        LaunchedEffect(TimetableState.selectedCohortId) {
            val id = TimetableState.selectedCohortId
            if (id != null) model.loadTimetable(id)
        }

        TimetableScreen(
            isLoading = model.isLoading,
            lessons = model.lessons,
            substitutions = model.substitutions,
            teacherMap = model.teacherMap,
            error = if (TimetableState.selectedCohortId == null && TimetableState.cohortsLoaded) {
                "No cohort assigned to your account"
            } else {
                model.error
            },
        )
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun TimetableScreen(
    isLoading: Boolean,
    lessons: List<EnrichedLesson>,
    substitutions: List<SubstitutionWithRelations>,
    teacherMap: Map<String, Teacher>,
    error: String?,
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var selectedDate by remember { mutableStateOf(today) }
    val weekDays = remember { weekOf(today) }
    val filtered = remember(lessons, selectedDate) { lessonsForDate(lessons, selectedDate) }
    val activeSubstitutions = remember(substitutions, selectedDate) {
        substitutions
            .filter { it.substitution.date.startsWith(selectedDate.toString()) }
            .flatMap { sub -> sub.lessons.map { lessonId -> lessonId to sub } }
            .toMap()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Lesson area ──────────────────────────────────────────────────────
        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (filtered.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No lessons today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            substitution = activeSubstitutions[lesson.id],
                            teacherMap = teacherMap,
                        )
                    }
                }
            }
        }

        // ── Week strip ───────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                weekDays.forEach { date ->
                    val isSelected = date == selectedDate
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedDate = date }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = date.dayOfWeek.name.take(2).lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Cohort switcher (used in TopBar when timetable tab is active) ─────────────

@Composable
fun CohortSwitcher() {
    val cohorts = TimetableState.cohorts
    val selectedCohortId = TimetableState.selectedCohortId
    val selectedCohort = cohorts.find { it.id == selectedCohortId }
    var expanded by remember { mutableStateOf(false) }

    val label = when {
        selectedCohort != null -> selectedCohort.name
        !TimetableState.cohortsLoaded -> "Loading…"
        TimetableState.cohortsError != null -> "Error"
        else -> "Select class"
    }

    Box {
        Column(
            modifier = Modifier.clickable(enabled = cohorts.isNotEmpty()) { expanded = true },
        ) {
            Text(
                text = "Class",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Switch class",
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            cohorts.forEach { cohort ->
                DropdownMenuItem(
                    text = {
                        Text(
                            cohort.name,
                            fontWeight = if (cohort.id == selectedCohortId) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        TimetableState.selectedCohortId = cohort.id
                        expanded = false
                    },
                )
            }
        }
    }
}

// ── Lesson card ───────────────────────────────────────────────────────────────

@Composable
fun LessonCard(
    lesson: EnrichedLesson,
    substitution: SubstitutionWithRelations? = null,
    teacherMap: Map<String, Teacher> = emptyMap(),
) {
    val startTime = lesson.period?.startTime?.take(5) ?: ""
    val endTime = lesson.period?.endTime?.take(5) ?: ""
    val subjectName = lesson.subject?.name ?: "Unknown"
    val roomName = lesson.classrooms.firstOrNull()?.name?.let { "Room $it" } ?: ""

    val isCancelled = substitution != null && substitution.teacher == null
    val isSubstituted = substitution != null && substitution.teacher != null

    val cancelledColor = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
    val substitutedColor = Color(0xFFF59E0B)

    val textColor = when {
        isCancelled -> cancelledColor
        else -> Color.Unspecified
    }
    val squareBorderColor = if (isCancelled) cancelledColor else Color.Transparent

    val substituterTeacher = substitution?.substitution?.substituter?.let { id -> teacherMap[id] }
    val displayTeacherName = substituterTeacher?.let { "${it.lastName} ${it.firstName}" }
    val originalTeacherName = lesson.teachers.firstOrNull()?.name ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Time column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(52.dp),
        ) {
            Text(startTime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = textColor)
            Spacer(Modifier.height(6.dp))
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(endTime, style = MaterialTheme.typography.labelSmall, color = if (isCancelled) cancelledColor else MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.width(10.dp))

        // Subject thumbnail
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, squareBorderColor, RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(lesson.subject?.short ?: "?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.width(14.dp))

        // Info column
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = textColor)
                if (isCancelled) Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = "Cancelled",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (isSubstituted && displayTeacherName != null) {
                Text("Teacher", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(displayTeacherName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = substitutedColor)
                if (originalTeacherName.isNotEmpty()) {
                    Text("was $originalTeacherName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (originalTeacherName.isNotEmpty()) {
                Text("Teacher", style = MaterialTheme.typography.labelSmall, color = if (isCancelled) cancelledColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(originalTeacherName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textColor)
            }
            if (roomName.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(roomName, style = MaterialTheme.typography.bodySmall, color = textColor)
            }
        }
    }
}

// ── ScreenModel ───────────────────────────────────────────────────────────────

class TimetableScreenModel(
    private val lessonApi: LessonApi,
    private val cohortApi: CohortApi,
    private val teacherApi: TeacherApi,
    private val substitutionApi: SubstitutionApi,
) : ScreenModel {
    var lessons by mutableStateOf<List<EnrichedLesson>>(emptyList())
    var substitutions by mutableStateOf<List<SubstitutionWithRelations>>(emptyList())
    var teacherMap by mutableStateOf<Map<String, Teacher>>(emptyMap())
    var error by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    fun loadCohorts() {
        if (TimetableState.cohortsLoaded) return
        screenModelScope.launch(Dispatchers.Default) {
            launch {
                when (val result = teacherApi.getTimetableTeachersAll()) {
                    is APIResult.Success -> withContext(Dispatchers.Main) {
                        teacherMap = result.data.flatMap { t ->
                            listOfNotNull(t.id to t, t.userId?.let { uid -> uid to t })
                        }.toMap()
                    }
                    is APIResult.Failure -> { /* non-fatal */ }
                }
            }
            when (val result = cohortApi.getCohort()) {
                is APIResult.Success -> withContext(Dispatchers.Main) {
                    TimetableState.cohorts = result.data
                    if (TimetableState.selectedCohortId == null) {
                        TimetableState.selectedCohortId = AuthState.cohortId
                            ?: result.data.firstOrNull()?.id
                    }
                    TimetableState.cohortsLoaded = true
                }
                is APIResult.Failure -> withContext(Dispatchers.Main) {
                    TimetableState.cohortsError = result.error.toString()
                    TimetableState.cohortsLoaded = true
                }
            }
        }
    }

    fun loadTimetable(cohortId: String) {
        screenModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { isLoading = true; error = null }
            try {
                launch {
                    when (val result = substitutionApi.getTimetableSubstitutionsCohortByCohortId(cohortId)) {
                        is APIResult.Success -> withContext(Dispatchers.Main) {
                            substitutions = result.data.substitutions
                        }
                        is APIResult.Failure -> { /* non-fatal */ }
                    }
                }
                when (val result = lessonApi.getTimetableLessonsForCohortByCohortId(cohortId)) {
                    is APIResult.Success -> withContext(Dispatchers.Main) { lessons = result.data }
                    is APIResult.Failure -> withContext(Dispatchers.Main) { error = result.error.toString() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { error = e.message ?: "Unknown error" }
            } finally {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }
}

// ── Date helpers ──────────────────────────────────────────────────────────────

private fun weekOf(date: LocalDate): List<LocalDate> {
    return if (date.dayOfWeek.ordinal >= 5) {
        val saturday = date.minus(date.dayOfWeek.ordinal - 5, DateTimeUnit.DAY)
        (0..1).map { saturday.plus(it, DateTimeUnit.DAY) } +
        (2..6).map { saturday.plus(it, DateTimeUnit.DAY) }
    } else {
        val monday = date.minus(date.dayOfWeek.ordinal, DateTimeUnit.DAY)
        (0..6).map { monday.plus(it, DateTimeUnit.DAY) }
    }
}

private fun lessonsForDate(lessons: List<EnrichedLesson>, date: LocalDate): List<EnrichedLesson> {
    val dayNum = date.dayOfWeek.isoDayNumber.toString()
    return lessons
        .filter { lesson ->
            val days = lesson.day?.days?.filterNotNull() ?: return@filter false
            days.any { it == dayNum }
        }
        .sortedBy { it.period?.startTime ?: "" }
}
