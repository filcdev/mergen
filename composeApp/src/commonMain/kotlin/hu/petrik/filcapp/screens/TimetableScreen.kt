@file:OptIn(kotlin.time.ExperimentalTime::class)

package hu.petrik.filcapp.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.api.APIResult
import hu.petrik.filcapp.api.CohortApi
import hu.petrik.filcapp.api.LessonApi
import hu.petrik.filcapp.api.client.APIClient
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.models.Cohort
import hu.petrik.filcapp.models.EnrichedLesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.*

// ── Tab entry point ──────────────────────────────────────────────────────────

object TimetableTab : Tab {
    override val options: TabOptions
        @Composable get() {
            val icon = rememberVectorPainter(Icons.Default.CalendarMonth)
            return remember { TabOptions(index = 1u, title = "Timetable", icon = icon) }
        }

    @Composable
    override fun Content() {
        val model = rememberScreenModel { TimetableScreenModel(LessonApi(APIClient), CohortApi(APIClient)) }

        LaunchedEffect(Unit) { model.load() }

        TimetableScreen(
            isLoading = model.isLoading,
            lessons = model.lessons,
            cohorts = model.cohorts,
            selectedCohortId = model.selectedCohortId,
            onCohortSelected = { model.selectCohort(it) },
            error = if (model.selectedCohortId == null) "No cohort assigned to your account" else model.error,
        )
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun TimetableScreen(
    isLoading: Boolean,
    lessons: List<EnrichedLesson>,
    cohorts: List<Cohort>,
    selectedCohortId: String?,
    onCohortSelected: (Cohort) -> Unit,
    error: String?,
) {
    var selectedDate by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    val weekDays = remember(selectedDate) { weekOf(selectedDate) }
    val filtered = remember(lessons, selectedDate) { lessonsForDate(lessons, selectedDate) }

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
                        LessonCard(lesson = lesson)
                    }
                }
            }
        }

        // ── Bottom panel (cohort switcher + week strip) ───────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column {
                CohortSwitcher(
                    cohorts = cohorts,
                    selectedCohortId = selectedCohortId,
                    onCohortSelected = onCohortSelected,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )

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
}

// ── Cohort switcher ───────────────────────────────────────────────────────────

@Composable
fun CohortSwitcher(
    cohorts: List<Cohort>,
    selectedCohortId: String?,
    onCohortSelected: (Cohort) -> Unit,
) {
    val selectedCohort = cohorts.find { it.id == selectedCohortId }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = cohorts.isNotEmpty()) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedCohort?.name ?: if (cohorts.isEmpty()) "Loading…" else "Select class",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Switch class",
                modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f },
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
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
                        onCohortSelected(cohort)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ── Lesson card ───────────────────────────────────────────────────────────────

@Composable
fun LessonCard(lesson: EnrichedLesson) {
    val startTime = lesson.period?.startTime?.take(5) ?: ""
    val endTime = lesson.period?.endTime?.take(5) ?: ""
    val subjectName = lesson.subject?.name ?: "Unknown"
    val teacherName = lesson.teachers.firstOrNull()?.name ?: ""
    val roomName = lesson.classrooms.firstOrNull()?.name?.let { "Room $it" } ?: ""

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
            Text(startTime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
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
            Text(endTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.width(10.dp))

        // Image placeholder
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 110.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(lesson.subject?.short ?: "?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.width(14.dp))

        // Info column
        Column(modifier = Modifier.weight(1f)) {
            Text(subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (teacherName.isNotEmpty()) {
                Text("Teacher", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(teacherName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            if (roomName.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(roomName, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── ScreenModel ───────────────────────────────────────────────────────────────

class TimetableScreenModel(
    private val lessonApi: LessonApi,
    private val cohortApi: CohortApi,
) : ScreenModel {
    var lessons by mutableStateOf<List<EnrichedLesson>>(emptyList())
    var cohorts by mutableStateOf<List<Cohort>>(emptyList())
    var selectedCohortId by mutableStateOf<String?>(AuthState.cohortId)
    var error by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    fun load() {
        screenModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { isLoading = true; error = null }
            try {
                when (val result = cohortApi.getCohort()) {
                    is APIResult.Success -> withContext(Dispatchers.Main) { cohorts = result.data }
                    is APIResult.Failure -> { /* non-fatal — switcher just stays empty */ }
                }
                val cohortId = selectedCohortId
                if (cohortId != null) {
                    when (val result = lessonApi.getTimetableLessonsForCohort(cohortId)) {
                        is APIResult.Success -> withContext(Dispatchers.Main) { lessons = result.data }
                        is APIResult.Failure -> withContext(Dispatchers.Main) { error = result.error.toString() }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { error = e.message ?: "Unknown error" }
            } finally {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    fun selectCohort(cohort: Cohort) {
        selectedCohortId = cohort.id
        screenModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { isLoading = true; error = null }
            try {
                when (val result = lessonApi.getTimetableLessonsForCohort(cohort.id)) {
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
    val monday = date.minus(date.dayOfWeek.ordinal, DateTimeUnit.DAY)
    return (0..6).map { monday.plus(it, DateTimeUnit.DAY) }
}

private fun lessonsForDate(lessons: List<EnrichedLesson>, date: LocalDate): List<EnrichedLesson> {
    val dayNum = date.dayOfWeek.isoDayNumber.toString() // "1"=Mon ... "7"=Sun
    return lessons
        .filter { lesson ->
            val days = lesson.day?.days?.filterNotNull() ?: return@filter false
            days.any { it == dayNum }
        }
        .sortedBy { it.period?.startTime ?: "" }
}
