package hu.petrik.filcapp.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.auth.AuthState
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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private object TimetableSessionCache {
    var timetables: List<TimetableDto> = emptyList()
    var selectedTimetableId: String? = null
    var teachers: List<TeacherDto> = emptyList()
    var classrooms: List<NamedRefDto> = emptyList()
    var referenceLoaded = false
    var referenceError: String? = null
    val cohortsByTimetableId = mutableMapOf<String, List<CohortDto>>()
    val lessonsByKey = mutableMapOf<String, List<LessonDto>>()
}

private fun timetableCacheKey(
    filter: TimetableFilter,
    selectionId: String,
    timetableId: String,
): String = "${filter.name}|$selectionId|$timetableId"

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen() {
    var timetables by remember { mutableStateOf(TimetableSessionCache.timetables) }
    var selectedTimetableId by remember { mutableStateOf(TimetableSessionCache.selectedTimetableId) }
    var cohorts by remember {
        mutableStateOf(
            TimetableSessionCache.selectedTimetableId
                ?.let(TimetableSessionCache.cohortsByTimetableId::get)
                .orEmpty(),
        )
    }
    var teachers by remember { mutableStateOf(TimetableSessionCache.teachers) }
    var classrooms by remember { mutableStateOf(TimetableSessionCache.classrooms) }

    var filter by remember { mutableStateOf(TimetableFilter.COHORT) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var lessons by remember { mutableStateOf<List<LessonDto>>(emptyList()) }
    var selectedWeekId by remember { mutableStateOf<String?>(null) }

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var weekOffset by remember { mutableStateOf(0) }
    var selectedDayOrder by remember { mutableStateOf(defaultSchoolDay(today.dayOfWeek)) }
    var showTimetableSelector by remember { mutableStateOf(false) }

    var loadingReferenceData by remember { mutableStateOf(!TimetableSessionCache.referenceLoaded) }
    var loadingLessons by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(TimetableSessionCache.referenceError) }
    var reloadKey by remember { mutableStateOf(0) }
    var personalizedUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        if (reloadKey == 0 && TimetableSessionCache.referenceLoaded) {
            timetables = TimetableSessionCache.timetables
            teachers = TimetableSessionCache.teachers
            classrooms = TimetableSessionCache.classrooms
            selectedTimetableId = TimetableSessionCache.selectedTimetableId
            loadingReferenceData = false
            return@LaunchedEffect
        }

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

            TimetableSessionCache.timetables = timetables
            TimetableSessionCache.teachers = teachers
            TimetableSessionCache.classrooms = classrooms
            TimetableSessionCache.selectedTimetableId = latest.id
            TimetableSessionCache.referenceError = null

            if (reloadKey != 0) {
                TimetableSessionCache.cohortsByTimetableId.clear()
                TimetableSessionCache.lessonsByKey.clear()
            }
        } catch (throwable: Throwable) {
            error =
                throwable.message
                    ?: tr(
                        "Nem sikerült betölteni az órarendet.",
                        "Could not load timetable.",
                    )
            TimetableSessionCache.referenceError = error
        } finally {
            TimetableSessionCache.referenceLoaded = true
            loadingReferenceData = false
        }
    }

    LaunchedEffect(selectedTimetableId, reloadKey) {
        val timetableId = selectedTimetableId ?: return@LaunchedEffect

        val cachedCohorts =
            if (reloadKey == 0) {
                TimetableSessionCache.cohortsByTimetableId[timetableId]
            } else {
                null
            }

        if (cachedCohorts != null) {
            cohorts = cachedCohorts
            if (filter == TimetableFilter.COHORT && selectedId == null) {
                selectedId = cohorts.firstOrNull()?.id
            }
            return@LaunchedEffect
        }

        try {
            cohorts = FilcPublicApi.getCohorts(timetableId).sortedBy { it.name }
            TimetableSessionCache.cohortsByTimetableId[timetableId] = cohorts

            if (filter == TimetableFilter.COHORT && selectedId == null) {
                selectedId = cohorts.firstOrNull()?.id
            }
        } catch (throwable: Throwable) {
            error =
                throwable.message
                    ?: tr(
                        "Nem sikerült betölteni az osztályokat.",
                        "Could not load classes.",
                    )
        }
    }

    LaunchedEffect(AuthState.user?.id, AuthState.profile, cohorts, teachers) {
        val userId = AuthState.user?.id

        if (userId == null) {
            personalizedUserId = null
            return@LaunchedEffect
        }

        if (personalizedUserId == userId) {
            return@LaunchedEffect
        }

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

    LaunchedEffect(filter, selectedId, selectedTimetableId, reloadKey) {
        val timetableId = selectedTimetableId ?: return@LaunchedEffect
        val selectionId = selectedId ?: return@LaunchedEffect

        val cacheKey = timetableCacheKey(filter, selectionId, timetableId)
        val cachedLessons =
            if (reloadKey == 0) {
                TimetableSessionCache.lessonsByKey[cacheKey]
            } else {
                null
            }

        if (cachedLessons != null) {
            lessons = cachedLessons
            loadingLessons = false
            return@LaunchedEffect
        }

        loadingLessons = true
        error = null

        try {
            lessons =
                FilcPublicApi.getLessons(
                    filter = filter,
                    selectionId = selectionId,
                    timetableId = timetableId,
                )

            TimetableSessionCache.lessonsByKey[cacheKey] = lessons
        } catch (throwable: Throwable) {
            lessons = emptyList()
            error =
                throwable.message
                    ?: tr(
                        "Nem sikerült betölteni az órákat.",
                        "Could not load lessons.",
                    )
        } finally {
            loadingLessons = false
        }
    }

    val weekDefinitions =
        lessons
            .mapNotNull { it.weekDefinition }
            .distinctBy { it.id }
            .sortedBy { it.name }

    LaunchedEffect(weekDefinitions) {
        if (weekDefinitions.isEmpty()) {
            selectedWeekId = null
        } else if (selectedWeekId !in weekDefinitions.map { it.id }) {
            selectedWeekId = weekDefinitions.first().id
        }
    }

    val selectedWeek =
        weekDefinitions.firstOrNull { it.id == selectedWeekId }
            ?: weekDefinitions.firstOrNull()

    val visibleLessons =
        if (selectedWeek == null) {
            lessons
        } else {
            lessons.filter { it.weekDefinition?.id == selectedWeek.id }
        }

    val selectedDayLessons =
        visibleLessons
            .filter { dayOrder(it.day?.name.orEmpty()) == selectedDayOrder }
            .sortedWith(
                compareBy(
                    { it.period?.period ?: Int.MAX_VALUE },
                    { it.period?.startTime.orEmpty() },
                ),
            )

    val ownGroupIds = AuthState.profile?.groups?.map { it.id }?.toSet().orEmpty()

    val filterOptions =
        when (filter) {
            TimetableFilter.COHORT -> cohorts.map { it.id to it.name }
            TimetableFilter.TEACHER -> teachers.map { it.id to it.displayName }
            TimetableFilter.CLASSROOM -> classrooms.map { it.id to displayName(it) }
        }

    val hasPersonalProfile =
        when (filter) {
            TimetableFilter.COHORT -> AuthState.profile?.cohort?.id == selectedId
            TimetableFilter.TEACHER -> AuthState.profile?.teacher?.id == selectedId
            TimetableFilter.CLASSROOM -> false
        }

    val selectedTimetableLabel =
        when (filter) {
            TimetableFilter.COHORT ->
                cohorts
                    .firstOrNull { it.id == selectedId }
                    ?.let { it.short.ifBlank { it.name } }

            TimetableFilter.TEACHER ->
                teachers
                    .firstOrNull { it.id == selectedId }
                    ?.displayName

            TimetableFilter.CLASSROOM ->
                classrooms
                    .firstOrNull { it.id == selectedId }
                    ?.let(::displayName)
        } ?: tr("Válassz órarendet", "Select timetable")

    fun moveWeek(delta: Int) {
        weekOffset += delta

        if (weekDefinitions.size > 1) {
            val currentIndex =
                weekDefinitions
                    .indexOfFirst { it.id == selectedWeekId }
                    .takeIf { it >= 0 }
                    ?: 0

            val nextIndex =
                (currentIndex + delta)
                    .mod(weekDefinitions.size)

            selectedWeekId = weekDefinitions[nextIndex].id
        }

        selectedDayOrder =
            if (weekOffset == 0) {
                defaultSchoolDay(today.dayOfWeek)
            } else {
                1
            }
    }

    PullToRefreshBox(
        isRefreshing = loadingReferenceData || loadingLessons,
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
            TimetableWeekSwitcher(
                today = today,
                weekOffset = weekOffset,
                selectedWeek = selectedWeek,
                onPreviousWeek = { moveWeek(-1) },
                onNextWeek = { moveWeek(1) },
            )

            TimetableDaySelector(
                today = today,
                weekOffset = weekOffset,
                selectedDayOrder = selectedDayOrder,
                onDaySelected = { selectedDayOrder = it },
            )

            TimetableSelectionBar(
                filter = filter,
                selectedLabel = selectedTimetableLabel,
                onClick = { showTimetableSelector = true },
            )

            if (loadingReferenceData) {
                LoadingBlock()
                return@Column
            }

            error?.let { message ->
                ErrorBlock(
                    message = message,
                    onRetry = { reloadKey++ },
                )
            }

            when {
                loadingLessons -> LoadingBlock()

                selectedId != null && selectedDayLessons.isEmpty() && error == null -> {
                    EmptyDayBlock()
                }

                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectedDayLessons.forEach { lesson ->
                            FigmaLessonCard(
                                lesson = lesson,
                                ownGroupIds = ownGroupIds,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }

    if (showTimetableSelector) {
        TimetableSelectorSheet(
            filter = filter,
            filterOptions = filterOptions,
            selectedId = selectedId,
            timetables = timetables,
            selectedTimetableId = selectedTimetableId,
            onDismiss = { showTimetableSelector = false },
            onFilterChanged = { newFilter ->
                filter = newFilter
                selectedId =
                    when (newFilter) {
                        TimetableFilter.COHORT -> cohorts.firstOrNull()?.id
                        TimetableFilter.TEACHER -> teachers.firstOrNull()?.id
                        TimetableFilter.CLASSROOM -> classrooms.firstOrNull()?.id
                    }
            },
            onSelectionChanged = { selectedId = it },
            onTimetableChanged = { selectedTimetableId = it },
        )
    }
}

@Composable
private fun TimetableWeekSwitcher(
    today: LocalDate,
    weekOffset: Int,
    selectedWeek: WeekDefinitionDto?,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val monday = mondayOfWeek(today, weekOffset)
    val friday = LocalDate.fromEpochDays(monday.toEpochDays() + 4)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WeekArrowButton(
                    icon = Icons.Default.KeyboardArrowLeft,
                    onClick = onPreviousWeek,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = formatWeekRange(monday, friday),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Text(
                        text =
                            tr(
                                "${approximateTeachingWeek(monday)}. tanítási hét",
                                "School week ${approximateTeachingWeek(monday)}",
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                WeekArrowButton(
                    icon = Icons.Default.KeyboardArrowRight,
                    onClick = onNextWeek,
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )

                    Text(
                        text = weekBadgeLabel(selectedWeek),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekArrowButton(
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .size(32.dp)
                .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TimetableDaySelector(
    today: LocalDate,
    weekOffset: Int,
    selectedDayOrder: Int,
    onDaySelected: (Int) -> Unit,
) {
    val monday = mondayOfWeek(today, weekOffset)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        (1..5).forEach { dayIndex ->
            val date = LocalDate.fromEpochDays(monday.toEpochDays() + dayIndex - 1)
            val selected = selectedDayOrder == dayIndex

            Surface(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(53.dp)
                        .clickable {
                            onDaySelected(dayIndex)
                        },
                shape = RoundedCornerShape(12.dp),
                color =
                    if (selected) {
                        Color(0xFF4F46E5)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                shadowElevation = if (selected) 3.dp else 0.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = shortDayName(dayIndex),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (selected) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )

                    Text(
                        text = date.day.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color =
                            if (selected) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FigmaLessonCard(
    lesson: LessonDto,
    ownGroupIds: Set<String>,
) {
    val period = lesson.period
    val subjectName =
        lesson.subject?.name
            ?: lesson.subject?.short
            ?: tr("Ismeretlen tantárgy", "Unknown subject")

    val teachers =
        lesson.teachers
            .joinToString(", ") { displayName(it) }

    val classrooms =
        lesson.classrooms
            .joinToString(", ") { displayName(it) }

    val cohorts =
        lesson.cohorts
            .joinToString(", ") { it.short.ifBlank { it.name } }

    val isOwnGroup = lesson.groups.any { it.id in ownGroupIds }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color =
            if (isOwnGroup) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color =
                    if (isOwnGroup) {
                        Color(0xFF4F46E5)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = period?.period?.takeIf { it > 0 }?.toString() ?: "–",
                        style = MaterialTheme.typography.titleLarge,
                        color =
                            if (isOwnGroup) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (isOwnGroup) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                period?.let {
                    Text(
                        text = "${formatTime(it.startTime)} – ${formatTime(it.endTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                val metadata =
                    listOfNotNull(
                        classrooms.takeIf { it.isNotBlank() },
                        teachers.takeIf { it.isNotBlank() },
                    ).joinToString(" • ")

                if (metadata.isNotBlank()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (cohorts.isNotBlank() && !isOwnGroup) {
                    Text(
                        text = cohorts,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            lesson.weekDefinition?.let { week ->
                val label = week.short.ifBlank { week.name }

                if (label.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableSelectionBar(
    filter: TimetableFilter,
    selectedLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = filterIcon(filter),
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = tr("Megjelenített órarend", "Displayed timetable"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = filterTypeLabel(filter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableSelectorSheet(
    filter: TimetableFilter,
    filterOptions: List<Pair<String, String>>,
    selectedId: String?,
    timetables: List<TimetableDto>,
    selectedTimetableId: String?,
    onDismiss: () -> Unit,
    onFilterChanged: (TimetableFilter) -> Unit,
    onSelectionChanged: (String?) -> Unit,
    onTimetableChanged: (String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(999.dp),
                        ),
            )
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = tr("Órarend kiválasztása", "Choose timetable"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text =
                        tr(
                            "Válassz osztályt, tanárt vagy termet.",
                            "Choose a class, teacher or classroom.",
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TimetableFilterChips(
                selected = filter,
                onSelected = onFilterChanged,
            )

            SearchableSelection(
                options = filterOptions,
                selectedId = selectedId,
                label = filterLabel(filter),
                placeholder = tr("Kezdj el gépelni...", "Type to search..."),
                onSelected = onSelectionChanged,
            )

            if (timetables.size > 1) {
                SearchableSelection(
                    options = timetables.map { it.id to timetableLabel(it) },
                    selectedId = selectedTimetableId,
                    label = tr("Órarend verzió", "Timetable version"),
                    placeholder = tr("Válassz órarendet", "Select timetable"),
                    onSelected = onTimetableChanged,
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(tr("Kész", "Done"))
            }
        }
    }
}

private fun filterTypeLabel(filter: TimetableFilter): String =
    when (filter) {
        TimetableFilter.COHORT -> tr("Osztály", "Class")
        TimetableFilter.TEACHER -> tr("Tanár", "Teacher")
        TimetableFilter.CLASSROOM -> tr("Terem", "Classroom")
    }

private fun filterIcon(filter: TimetableFilter): ImageVector =
    when (filter) {
        TimetableFilter.COHORT -> Icons.Default.Groups
        TimetableFilter.TEACHER -> Icons.Default.Person
        TimetableFilter.CLASSROOM -> Icons.Default.MeetingRoom
    }

@Composable
private fun EmptyDayBlock() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = tr("Nincs óra ezen a napon", "No lessons on this day"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = tr(
                    "Válassz egy másik napot a fenti sávban.",
                    "Choose another day above.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = tr("Hiba történt", "Something went wrong"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(onClick = onRetry) {
                Text(tr("Újrapróbálás", "Retry"))
            }
        }
    }
}

private fun mondayOfWeek(
    date: LocalDate,
    weekOffset: Int,
): LocalDate {
    val daysSinceMonday =
        when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 6
        }

    return LocalDate.fromEpochDays(
        date.toEpochDays() - daysSinceMonday + weekOffset * 7,
    )
}

private fun defaultSchoolDay(day: DayOfWeek): Int =
    when (day) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 1
    }

private fun shortDayName(dayOrder: Int): String =
    when (dayOrder) {
        1 -> tr("Hét", "Mon")
        2 -> tr("Ked", "Tue")
        3 -> tr("Sze", "Wed")
        4 -> tr("Csü", "Thu")
        5 -> tr("Pén", "Fri")
        else -> ""
    }

private fun formatWeekRange(
    monday: LocalDate,
    friday: LocalDate,
): String {
    val startMonth = monthName(monday.monthNumber)
    val endMonth = monthName(friday.monthNumber)

    return if (monday.monthNumber == friday.monthNumber) {
        "${monday.year}. $startMonth ${monday.day} – ${friday.day}."
    } else {
        "${monday.year}. $startMonth ${monday.day} – $endMonth ${friday.day}."
    }
}

private fun monthName(month: Int): String =
    when (month) {
        1 -> "január"
        2 -> "február"
        3 -> "március"
        4 -> "április"
        5 -> "május"
        6 -> "június"
        7 -> "július"
        8 -> "augusztus"
        9 -> "szeptember"
        10 -> "október"
        11 -> "november"
        12 -> "december"
        else -> ""
    }

private fun approximateTeachingWeek(monday: LocalDate): Int {
    val schoolYear =
        if (monday.monthNumber >= 9) {
            monday.year
        } else {
            monday.year - 1
        }

    val schoolStart = LocalDate(schoolYear, 9, 1)
    val elapsedDays = monday.toEpochDays() - schoolStart.toEpochDays()

    return (((elapsedDays.coerceAtLeast(0L)) / 7L) + 1L).toInt()
}

private fun weekBadgeLabel(week: WeekDefinitionDto?): String {
    val raw =
        week
            ?.short
            ?.ifBlank { week.name }
            ?.trim()
            .orEmpty()

    if (raw.isBlank()) {
        return tr("AKTUÁLIS", "CURRENT")
    }

    return if (raw.contains("hét", ignoreCase = true)) {
        raw.uppercase()
    } else {
        "${raw.uppercase()} HÉT"
    }
}

private fun filterLabel(filter: TimetableFilter): String =
    when (filter) {
        TimetableFilter.COHORT -> tr("Osztály keresése", "Find class")
        TimetableFilter.TEACHER -> tr("Tanár keresése", "Find teacher")
        TimetableFilter.CLASSROOM -> tr("Terem keresése", "Find classroom")
    }

private fun timetableLabel(timetable: TimetableDto): String =
    timetable.name.ifBlank {
        timetable.validFrom?.let {
            tr(
                "Órarend – ${formatDate(it)}",
                "Timetable – ${formatDate(it)}",
            )
        } ?: tr("Órarend", "Timetable")
    }

private fun dayOrder(day: String): Int {
    val normalized = day.lowercase()

    return when {
        normalized.contains("hétf") ||
            normalized.contains("hetf") ||
            normalized.contains("monday") -> 1

        normalized.contains("kedd") ||
            normalized.contains("tuesday") -> 2

        normalized.contains("szerda") ||
            normalized.contains("wednesday") -> 3

        normalized.contains("csüt") ||
            normalized.contains("csut") ||
            normalized.contains("thursday") -> 4

        normalized.contains("pént") ||
            normalized.contains("pent") ||
            normalized.contains("friday") -> 5

        normalized.contains("szomb") ||
            normalized.contains("saturday") -> 6

        normalized.contains("vasár") ||
            normalized.contains("vasar") ||
            normalized.contains("sunday") -> 7

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

    return if (parts.size == 3) {
        "${parts[0]}. ${parts[1]}. ${parts[2]}."
    } else {
        value.take(10)
    }
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
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
        TimetableScreen()
    }
}
