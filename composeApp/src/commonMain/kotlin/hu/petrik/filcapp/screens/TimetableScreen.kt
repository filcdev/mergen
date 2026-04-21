@file:OptIn(kotlin.time.ExperimentalTime::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.api.APIResult
import hu.petrik.filcapp.api.ClassroomApi
import hu.petrik.filcapp.api.CohortApi
import hu.petrik.filcapp.api.LessonApi
import hu.petrik.filcapp.api.MovedLessonApi
import hu.petrik.filcapp.api.SubstitutionApi
import hu.petrik.filcapp.api.TeacherApi
import hu.petrik.filcapp.api.client.APIClient
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.models.Classroom
import hu.petrik.filcapp.models.Cohort
import hu.petrik.filcapp.models.EnrichedLesson
import hu.petrik.filcapp.models.MovedLessonWithRelations
import hu.petrik.filcapp.models.SubstitutionWithRelations
import hu.petrik.filcapp.models.Teacher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.*

// ── Shared timetable state (readable by TopBar) ───────────────────────────────

enum class TimetableMode { Class, Room, Teacher }

object TimetableState {
    var cohorts by mutableStateOf<List<Cohort>>(emptyList())
    var cohortsLoaded by mutableStateOf(false)
    var cohortsError by mutableStateOf<String?>(null)
    var selectedCohortId by mutableStateOf<String?>(null)
    var ownCohortId by mutableStateOf<String?>(null)
    var lessons by mutableStateOf<List<EnrichedLesson>>(emptyList())
    var substitutions by mutableStateOf<List<SubstitutionWithRelations>>(emptyList())
    var movedLessons by mutableStateOf<List<MovedLessonWithRelations>>(emptyList())
    var teacherMap by mutableStateOf<Map<String, Teacher>>(emptyMap())
    var timetableLoaded by mutableStateOf(false)
    // Room / Teacher mode
    var timetableMode by mutableStateOf(TimetableMode.Class)
    var classrooms by mutableStateOf<List<Classroom>>(emptyList())
    var classroomsLoaded by mutableStateOf(false)
    var teacherList by mutableStateOf<List<Teacher>>(emptyList())
    var selectedRoomId by mutableStateOf<String?>(null)
    var selectedTeacherId by mutableStateOf<String?>(null)
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
                MovedLessonApi(APIClient),
                ClassroomApi(APIClient),
            )
        }

        LaunchedEffect(Unit) { model.loadCohorts() }

        LaunchedEffect(TimetableState.selectedCohortId) {
            val id = TimetableState.selectedCohortId
            if (id != null) model.loadTimetable(id)
        }

        LaunchedEffect(TimetableState.timetableMode) {
            if (TimetableState.timetableMode == TimetableMode.Room && !TimetableState.classroomsLoaded) {
                model.loadClassrooms()
            }
        }

        LaunchedEffect(TimetableState.selectedRoomId) {
            val id = TimetableState.selectedRoomId ?: return@LaunchedEffect
            model.loadRoomTimetable(id)
        }

        LaunchedEffect(TimetableState.selectedTeacherId) {
            val id = TimetableState.selectedTeacherId ?: return@LaunchedEffect
            model.loadTeacherTimetable(id)
        }

        val mode = TimetableState.timetableMode
        val lessons = when (mode) {
            TimetableMode.Class -> if (model.isViewingOwnCohort) TimetableState.lessons else model.localLessons
            TimetableMode.Room -> model.localRoomLessons
            TimetableMode.Teacher -> model.localTeacherLessons
        }
        val substitutions = if (mode == TimetableMode.Class) {
            if (model.isViewingOwnCohort) TimetableState.substitutions else model.localSubstitutions
        } else emptyList()
        val movedLessons = if (mode == TimetableMode.Class) {
            if (model.isViewingOwnCohort) TimetableState.movedLessons else model.localMovedLessons
        } else emptyList()

        val error = when {
            mode == TimetableMode.Class && TimetableState.selectedCohortId == null && TimetableState.cohortsLoaded ->
                "No cohort assigned to your account"
            mode == TimetableMode.Room && TimetableState.selectedRoomId == null ->
                "Select a room from the top bar"
            mode == TimetableMode.Teacher && TimetableState.selectedTeacherId == null ->
                "Select a teacher from the top bar"
            else -> model.error
        }

        TimetableScreen(
            mode = mode,
            isLoading = model.isLoading,
            lessons = lessons,
            substitutions = substitutions,
            movedLessons = movedLessons,
            teacherMap = TimetableState.teacherMap,
            error = error,
            onRefresh = {
                when (mode) {
                    TimetableMode.Class -> TimetableState.selectedCohortId?.let { model.loadTimetable(it) }
                    TimetableMode.Room -> TimetableState.selectedRoomId?.let { model.loadRoomTimetable(it) }
                    TimetableMode.Teacher -> TimetableState.selectedTeacherId?.let { model.loadTeacherTimetable(it) }
                }
            },
        )
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun TimetableScreen(
    mode: TimetableMode = TimetableMode.Class,
    isLoading: Boolean,
    lessons: List<EnrichedLesson>,
    substitutions: List<SubstitutionWithRelations>,
    movedLessons: List<MovedLessonWithRelations>,
    teacherMap: Map<String, Teacher>,
    error: String?,
    onRefresh: () -> Unit = {},
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var selectedDate by remember { mutableStateOf(today) }
    val weekDays = remember { weekOf(today) }
    var expandedLessonId by remember { mutableStateOf<String?>(null) }
    var cooldown by remember { mutableStateOf(0) }
    var currentTime by remember {
        mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time)
    }

    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000)
            cooldown--
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        }
    }

    val activeSubstitutions = remember(substitutions, selectedDate) {
        substitutions
            .filter { it.substitution.date.startsWith(selectedDate.toString()) }
            .flatMap { sub -> sub.lessons.map { lessonId -> lessonId to sub } }
            .toMap()
    }

    // movedToday: lessons moved TO this specific date (may not normally appear today)
    val movedToday = remember(movedLessons, selectedDate) {
        movedLessons
            .filter { it.movedLesson.date.startsWith(selectedDate.toString()) }
            .flatMap { ml -> ml.lessons.map { id -> id to ml } }
            .toMap()
    }
    // movedAway: lessons originally on today's day that were moved to a different date
    val movedAway = remember(movedLessons, selectedDate) {
        val todayIsoDay = selectedDate.dayOfWeek.isoDayNumber.toString()
        movedLessons
            .filter { ml ->
                !ml.movedLesson.date.startsWith(selectedDate.toString()) &&
                ml.dayDefinition?.days?.contains(todayIsoDay) == true
            }
            .flatMap { ml -> ml.lessons.map { id -> id to ml } }
            .toMap()
    }

    val lessonById = remember(lessons) { lessons.associateBy { it.id } }

    // Base timetable lessons for today + extra lessons moved to today from another day
    val displayLessons = remember(lessons, selectedDate, movedToday, movedAway) {
        val base = lessonsForDate(lessons, selectedDate)
        val extra = movedToday.keys
            .mapNotNull { id -> lessonById[id] }
            .filter { l -> !base.any { it.id == l.id } }
        (base + extra).sortedBy { l ->
            movedToday[l.id]?.period?.startTime ?: l.period?.startTime ?: ""
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Mode toggle ──────────────────────────────────────────────────────
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TimetableMode.entries.forEachIndexed { index, m ->
                SegmentedButton(
                    selected = mode == m,
                    onClick = { TimetableState.timetableMode = m },
                    shape = SegmentedButtonDefaults.itemShape(index, TimetableMode.entries.size),
                    label = { Text(m.name) },
                )
            }
        }

        // ── Lesson area ──────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = {
                    if (cooldown == 0) {
                        cooldown = 5
                        onRefresh()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    when {
                        error != null -> item {
                            Box(modifier = Modifier.fillParentMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        displayLessons.isEmpty() && !isLoading -> item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No lessons today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        else -> items(displayLessons, key = { it.id }) { lesson ->
                            val period = if (movedToday.containsKey(lesson.id))
                                movedToday[lesson.id]?.period ?: lesson.period
                            else lesson.period
                            val isActive = selectedDate == today && isLessonActive(period, currentTime)
                            LessonCard(
                                lesson = lesson,
                                substitution = activeSubstitutions[lesson.id],
                                movedLesson = movedToday[lesson.id] ?: movedAway[lesson.id],
                                isMovedHere = movedToday.containsKey(lesson.id),
                                isActive = isActive,
                                teacherMap = teacherMap,
                                onClick = { expandedLessonId = if (expandedLessonId == lesson.id) null else lesson.id },
                            )
                        }
                    }
                }
            }

            if (cooldown > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = "Refresh in ${cooldown}s",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // ── Lesson detail sheet ──────────────────────────────────────────────
        val expandedLesson = expandedLessonId?.let { id -> displayLessons.find { it.id == id } }
        if (expandedLesson != null) {
            ModalBottomSheet(
                onDismissRequest = { expandedLessonId = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                LessonDetailSheet(
                    lesson = expandedLesson,
                    substitution = activeSubstitutions[expandedLesson.id],
                    movedLesson = movedToday[expandedLesson.id] ?: movedAway[expandedLesson.id],
                    isMovedHere = movedToday.containsKey(expandedLesson.id),
                    teacherMap = teacherMap,
                )
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

// ── Room switcher (tap → bottom sheet with search) ──────────────────────────

@Composable
fun RoomSwitcher() {
    val classrooms = TimetableState.classrooms
    val selectedRoomId = TimetableState.selectedRoomId
    val selectedRoom = classrooms.find { it.id == selectedRoomId }
    var sheetOpen by remember { mutableStateOf(false) }

    SwitcherTapTarget(
        label = "Room",
        value = selectedRoom?.name ?: "Select room",
        hasSelection = selectedRoom != null,
        onClick = { sheetOpen = true },
    )

    if (sheetOpen) {
        SearchPickerSheet(
            title = "Select Room",
            items = classrooms,
            selectedId = selectedRoomId,
            itemId = { it.id },
            itemPrimary = { it.name },
            itemSecondary = { it.short },
            filterItem = { room, q -> room.name.contains(q, ignoreCase = true) || room.short.contains(q, ignoreCase = true) },
            onSelect = { TimetableState.selectedRoomId = it.id },
            onDismiss = { sheetOpen = false },
        )
    }
}

// ── Teacher switcher (tap → bottom sheet with search) ────────────────────────

@Composable
fun TeacherSwitcher() {
    val teachers = TimetableState.teacherList
    val selectedTeacherId = TimetableState.selectedTeacherId
    val selectedTeacher = teachers.find { it.id == selectedTeacherId }
    var sheetOpen by remember { mutableStateOf(false) }

    SwitcherTapTarget(
        label = "Teacher",
        value = selectedTeacher?.let { "${it.lastName} ${it.firstName}" } ?: "Select teacher",
        hasSelection = selectedTeacher != null,
        onClick = { sheetOpen = true },
    )

    if (sheetOpen) {
        SearchPickerSheet(
            title = "Select Teacher",
            items = teachers,
            selectedId = selectedTeacherId,
            itemId = { it.id },
            itemPrimary = { "${it.lastName} ${it.firstName}" },
            itemSecondary = { it.short },
            filterItem = { t, q ->
                "${t.lastName} ${t.firstName}".contains(q, ignoreCase = true) ||
                t.short.contains(q, ignoreCase = true)
            },
            onSelect = { TimetableState.selectedTeacherId = it.id },
            onDismiss = { sheetOpen = false },
        )
    }
}

// ── Shared tap target (matches CohortSwitcher style) ─────────────────────────

@Composable
private fun SwitcherTapTarget(
    label: String,
    value: String,
    hasSelection: Boolean,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (hasSelection) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Generic search picker bottom sheet ───────────────────────────────────────

@Composable
private fun <T> SearchPickerSheet(
    title: String,
    items: List<T>,
    selectedId: String?,
    itemId: (T) -> String,
    itemPrimary: (T) -> String,
    itemSecondary: (T) -> String,
    filterItem: (T, String) -> Boolean,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) {
        if (query.isBlank()) items else items.filter { filterItem(it, query) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search…", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp).clickable { query = "" },
                    )
                }
            }

            // Results
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No results", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(filtered, key = { itemId(it) }) { item ->
                        val isSelected = itemId(item) == selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(item)
                                    onDismiss()
                                }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    itemPrimary(item),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                val secondary = itemSecondary(item)
                                if (secondary.isNotEmpty()) {
                                    Text(
                                        secondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        "Selected",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// ── Lesson card ───────────────────────────────────────────────────────────────

@Composable
fun LessonCard(
    lesson: EnrichedLesson,
    substitution: SubstitutionWithRelations? = null,
    movedLesson: MovedLessonWithRelations? = null,
    isMovedHere: Boolean = false,
    isActive: Boolean = false,
    teacherMap: Map<String, Teacher> = emptyMap(),
    onClick: (() -> Unit)? = null,
) {
    // When a lesson was moved to today, display its new time/room; otherwise use the timetable values
    val displayPeriod = if (isMovedHere) movedLesson?.period ?: lesson.period else lesson.period
    val startTime = displayPeriod?.startTime?.take(5) ?: ""
    val endTime = displayPeriod?.endTime?.take(5) ?: ""
    val subjectName = lesson.subject?.name ?: "Unknown"
    val displayRoom = if (isMovedHere) movedLesson?.classroom?.name else lesson.classrooms.firstOrNull()?.name
    val roomName = displayRoom?.let { "Room $it" } ?: ""

    val isCancelled = substitution != null && substitution.teacher == null
    val isSubstituted = substitution != null && substitution.teacher != null
    val isMovedAway = movedLesson != null && !isMovedHere

    val cancelledColor = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
    val substitutedColor = Color(0xFFF59E0B)
    val movedColor = MaterialTheme.colorScheme.primary

    val textColor = when {
        isCancelled -> cancelledColor
        isMovedAway -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        else -> Color.Unspecified
    }
    val squareBorderColor = when {
        isCancelled -> cancelledColor
        isMovedHere -> movedColor
        else -> Color.Transparent
    }

    val substituterTeacher = substitution?.substitution?.substituter?.let { id -> teacherMap[id] }
    val displayTeacherName = substituterTeacher?.let { "${it.lastName} ${it.firstName}" }
    val originalTeacherName = lesson.teachers.firstOrNull()?.name ?: ""

    val movedToDate = if (isMovedAway) movedLesson?.movedLesson?.date?.take(10) else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Time column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp),
        ) {
            Text(startTime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = textColor)
            Text(endTime, style = MaterialTheme.typography.labelSmall, color = if (isCancelled) cancelledColor else MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.width(8.dp))

        // Subject thumbnail
        val thumbnailBackground = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        val thumbnailTextColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, squareBorderColor, RoundedCornerShape(10.dp))
                .background(thumbnailBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = lesson.subject?.short ?: "?",
                style = MaterialTheme.typography.labelMedium,
                color = thumbnailTextColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(4.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        // Info column
        Column(modifier = Modifier.weight(1f)) {
            Text(subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val hasChips = isCancelled || isMovedAway
            if (hasChips) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    if (isMovedAway) Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = if (movedToDate != null) "→ $movedToDate" else "Moved away",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (isSubstituted && displayTeacherName != null) {
                Text(displayTeacherName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = substitutedColor)
            } else if (originalTeacherName.isNotEmpty()) {
                Text(originalTeacherName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = textColor)
            }
            if (roomName.isNotEmpty()) {
                val roomColor = when {
                    isMovedHere -> Color(0xFF9C27B0)
                    else -> textColor
                }
                Spacer(Modifier.height(2.dp))
                Text(roomName, style = MaterialTheme.typography.bodySmall, color = roomColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ── Lesson detail bottom sheet ────────────────────────────────────────────────

@Composable
fun LessonDetailSheet(
    lesson: EnrichedLesson,
    substitution: SubstitutionWithRelations? = null,
    movedLesson: MovedLessonWithRelations? = null,
    isMovedHere: Boolean = false,
    teacherMap: Map<String, Teacher> = emptyMap(),
) {
    val isCancelled = substitution != null && substitution.teacher == null
    val isSubstituted = substitution != null && substitution.teacher != null
    val isMovedAway = movedLesson != null && !isMovedHere

    val cancelledColor = MaterialTheme.colorScheme.error
    val substitutedColor = Color(0xFFF59E0B)
    val movedColor = MaterialTheme.colorScheme.primary

    val displayPeriod = if (isMovedHere) movedLesson?.period ?: lesson.period else lesson.period
    val startTime = displayPeriod?.startTime?.take(5) ?: ""
    val endTime = displayPeriod?.endTime?.take(5) ?: ""
    val displayRoom = if (isMovedHere) movedLesson?.classroom?.name else lesson.classrooms.firstOrNull()?.name

    val substituterTeacher = substitution?.substitution?.substituter?.let { id -> teacherMap[id] }
    val displayTeacherName = substituterTeacher?.let { "${it.lastName} ${it.firstName}" }
    val originalTeacherName = lesson.teachers.firstOrNull()?.name ?: ""

    val movedToDate = if (isMovedAway) movedLesson?.movedLesson?.date?.take(10) else null
    val movedFromDay = if (isMovedHere) lesson.day?.name else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Subject name
        Text(
            text = lesson.subject?.name ?: "Unknown",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = when {
                isCancelled -> cancelledColor
                isMovedAway -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else -> Color.Unspecified
            },
        )

        // Status chips
        val hasStatus = isCancelled || isMovedHere || isMovedAway || isSubstituted
        if (hasStatus) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCancelled) Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text("Cancelled", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.SemiBold)
                }
                if (isMovedHere) Surface(shape = RoundedCornerShape(6.dp), color = movedColor.copy(alpha = 0.15f)) {
                    Text("Moved here", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = movedColor, fontWeight = FontWeight.SemiBold)
                }
                if (isMovedAway) Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text("Moved away", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
                if (isSubstituted) Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF59E0B).copy(alpha = 0.15f)) {
                    Text("Substituted", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = substitutedColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        HorizontalDivider()

        // Time
        DetailRow(label = "Time", value = if (startTime.isNotEmpty()) "$startTime – $endTime" else "—")

        // Room
        DetailRow(label = "Room", value = displayRoom ?: "—")

        // Subject short
        lesson.subject?.short?.let { short ->
            DetailRow(label = "Short", value = short)
        }

        // Teacher
        if (isSubstituted && displayTeacherName != null) {
            DetailRow(label = "Teacher", value = displayTeacherName, valueColor = substitutedColor)
            if (originalTeacherName.isNotEmpty()) {
                DetailRow(label = "Was", value = originalTeacherName)
            }
        } else if (originalTeacherName.isNotEmpty()) {
            DetailRow(label = "Teacher", value = originalTeacherName)
        }

        // Move info
        if (isMovedHere && movedFromDay != null) {
            DetailRow(label = "Originally", value = movedFromDay, valueColor = movedColor)
        }
        if (isMovedAway && movedToDate != null) {
            DetailRow(label = "Moved to", value = movedToDate, valueColor = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

// ── ScreenModel ───────────────────────────────────────────────────────────────

class TimetableScreenModel(
    private val lessonApi: LessonApi,
    private val cohortApi: CohortApi,
    private val teacherApi: TeacherApi,
    private val substitutionApi: SubstitutionApi,
    private val movedLessonApi: MovedLessonApi,
    private val classroomApi: ClassroomApi,
) : ScreenModel {
    var localLessons by mutableStateOf<List<EnrichedLesson>>(emptyList())
    var localSubstitutions by mutableStateOf<List<SubstitutionWithRelations>>(emptyList())
    var localMovedLessons by mutableStateOf<List<MovedLessonWithRelations>>(emptyList())
    var localRoomLessons by mutableStateOf<List<EnrichedLesson>>(emptyList())
    var localTeacherLessons by mutableStateOf<List<EnrichedLesson>>(emptyList())
    var isViewingOwnCohort by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    fun loadCohorts() {
        if (TimetableState.cohortsLoaded) return
        screenModelScope.launch(Dispatchers.Default) {
            launch {
                when (val result = teacherApi.getTimetableTeachersAll()) {
                    is APIResult.Success -> withContext(Dispatchers.Main) {
                        TimetableState.teacherMap = result.data.flatMap { t ->
                            listOfNotNull(t.id to t, t.userId?.let { uid -> uid to t })
                        }.toMap()
                        TimetableState.teacherList = result.data.sortedBy { "${it.lastName} ${it.firstName}" }
                    }
                    is APIResult.Failure -> { /* non-fatal */ }
                }
            }
            when (val result = cohortApi.getCohort()) {
                is APIResult.Success -> withContext(Dispatchers.Main) {
                    TimetableState.cohorts = result.data
                    if (TimetableState.selectedCohortId == null) {
                        val own = AuthState.cohortId ?: result.data.firstOrNull()?.id
                        TimetableState.selectedCohortId = own
                        TimetableState.ownCohortId = own
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
        val isOwn = cohortId == TimetableState.ownCohortId
        screenModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { isLoading = true; error = null; isViewingOwnCohort = isOwn }
            try {
                launch {
                    when (val result = substitutionApi.getTimetableSubstitutionsCohortByCohortId(cohortId)) {
                        is APIResult.Success -> withContext(Dispatchers.Main) {
                            if (isOwn) TimetableState.substitutions = result.data.substitutions
                            else localSubstitutions = result.data.substitutions
                        }
                        is APIResult.Failure -> { /* non-fatal */ }
                    }
                }
                launch {
                    when (val result = movedLessonApi.getTimetableMovedLessonsCohortByCohortId(cohortId)) {
                        is APIResult.Success -> withContext(Dispatchers.Main) {
                            if (isOwn) TimetableState.movedLessons = result.data
                            else localMovedLessons = result.data
                        }
                        is APIResult.Failure -> { /* non-fatal */ }
                    }
                }
                when (val result = lessonApi.getTimetableLessonsForCohortByCohortId(cohortId)) {
                    is APIResult.Success -> withContext(Dispatchers.Main) {
                        if (isOwn) { TimetableState.lessons = result.data; TimetableState.timetableLoaded = true }
                        else localLessons = result.data
                    }
                    is APIResult.Failure -> withContext(Dispatchers.Main) { error = result.error.toString() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { error = e.message ?: "Unknown error" }
            } finally {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    fun loadClassrooms() {
        if (TimetableState.classroomsLoaded) return
        screenModelScope.launch(Dispatchers.Default) {
            when (val result = classroomApi.getTimetableClassroomsAll()) {
                is APIResult.Success -> withContext(Dispatchers.Main) {
                    TimetableState.classrooms = result.data.sortedBy { it.name }
                    TimetableState.classroomsLoaded = true
                }
                is APIResult.Failure -> { /* non-fatal */ }
            }
        }
    }

    fun loadRoomTimetable(roomId: String) {
        screenModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { isLoading = true; error = null }
            when (val result = lessonApi.getTimetableLessonsForRoomByClassroomId(roomId)) {
                is APIResult.Success -> withContext(Dispatchers.Main) { localRoomLessons = result.data }
                is APIResult.Failure -> withContext(Dispatchers.Main) { error = result.error.toString() }
            }
            withContext(Dispatchers.Main) { isLoading = false }
        }
    }

    fun loadTeacherTimetable(teacherId: String) {
        screenModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) { isLoading = true; error = null }
            when (val result = lessonApi.getTimetableLessonsForTeacherByTeacherId(teacherId)) {
                is APIResult.Success -> withContext(Dispatchers.Main) { localTeacherLessons = result.data }
                is APIResult.Failure -> withContext(Dispatchers.Main) { error = result.error.toString() }
            }
            withContext(Dispatchers.Main) { isLoading = false }
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

internal fun isLessonActive(period: hu.petrik.filcapp.models.Period?, now: LocalTime): Boolean {
    val start = period?.startTime?.take(5)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return false
    val end = period.endTime?.take(5)?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return false
    return now >= start && now <= end
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
