package hu.petrik.filcapp.screens

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
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import hu.petrik.filcapp.data.MovedLessonItemDto
import hu.petrik.filcapp.data.NamedRefDto
import hu.petrik.filcapp.data.SubstitutionItemDto
import hu.petrik.filcapp.data.SubstitutionLessonDto
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.ExperimentalTime

@Composable
fun SubstitutionScreen() {
    var substitutions by remember { mutableStateOf<List<SubstitutionItemDto>>(emptyList()) }
    var movedLessons by remember { mutableStateOf<List<MovedLessonItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        try {
            substitutions = ChronosPublicApi.getSubstitutions().filter { isTodayOrFuture(it.substitution.date) }
            movedLessons = ChronosPublicApi.getMovedLessons().filter { isTodayOrFuture(it.movedLesson.date) }
        } catch (throwable: Throwable) {
            error = throwable.message ?: "Nem sikerült betölteni a helyettesítéseket."
        } finally {
            loading = false
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
        Text("Helyettesítések", style = MaterialTheme.typography.headlineSmall)

        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
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
                    Text("Hiba történt", style = MaterialTheme.typography.titleMedium)
                    Text(message)
                    Button(onClick = { reloadKey++ }) {
                        Text("Újrapróbálás")
                    }
                }
            }
        }

        if (error == null && substitutions.isEmpty() && movedLessons.isEmpty()) {
            Text(
                "Nincs aktuális vagy közelgő helyettesítés.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val dates =
            (substitutions.map { normalizeDate(it.substitution.date) } +
                movedLessons.map { normalizeDate(it.movedLesson.date) })
                .distinct()
                .sorted()

        dates.forEach { date ->
            Text(formatDate(date), style = MaterialTheme.typography.titleMedium)

            substitutions
                .filter { normalizeDate(it.substitution.date) == date }
                .forEach { substitution -> SubstitutionCard(substitution) }

            movedLessons
                .filter { normalizeDate(it.movedLesson.date) == date }
                .forEach { movedLesson -> MovedLessonCard(movedLesson) }
        }
    }
}

@Composable
private fun SubstitutionCard(item: SubstitutionItemDto) {
    val substitute = item.teacher?.displayName.orEmpty()
    val isCancelled = item.substitution.substituter == null || substitute.isBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (isCancelled) "Elmaradó óra" else "Helyettesítés: $substitute",
                style = MaterialTheme.typography.titleMedium,
                color =
                    if (isCancelled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )

            item.lessons.forEachIndexed { index, lesson ->
                SubstitutionLessonDetails(lesson)
                if (index != item.lessons.lastIndex) {
                    HorizontalDivider()
                }
            }

            item.substitution.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                Text("Megjegyzés: $comment", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SubstitutionLessonDetails(lesson: SubstitutionLessonDto) {
    val subject = lesson.subject?.name ?: lesson.subject?.short ?: "Ismeretlen tantárgy"
    val originalTeachers = lesson.teachers.joinToString(", ") { displayName(it) }
    val classrooms = lesson.classrooms.joinToString(", ") { displayName(it) }
    val cohorts = lesson.cohorts.joinToString(", ")
    val period = lesson.period

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(subject, style = MaterialTheme.typography.titleSmall)
            period?.let { Text("${it.period}. óra", style = MaterialTheme.typography.labelLarge) }
        }
        period?.let {
            Text("${formatTime(it.startTime)}–${formatTime(it.endTime)}")
        }
        if (cohorts.isNotBlank()) {
            Text("Osztály: $cohorts")
        }
        if (classrooms.isNotBlank()) {
            Text("Terem: $classrooms")
        }
        if (originalTeachers.isNotBlank()) {
            Text("Eredeti tanár: $originalTeachers")
        }
    }
}

@Composable
private fun MovedLessonCard(item: MovedLessonItemDto) {
    val subjects = item.lessonNames.joinToString(", ").ifBlank { "Áthelyezett óra" }
    val room = item.classroom?.let(::displayName).orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Áthelyezett óra", style = MaterialTheme.typography.titleMedium)
            Text(subjects, style = MaterialTheme.typography.titleSmall)
            item.dayDefinition?.name?.takeIf { it.isNotBlank() }?.let { Text("Nap: $it") }
            item.period?.let {
                Text("${it.period}. óra • ${formatTime(it.startTime)}–${formatTime(it.endTime)}")
            }
            if (room.isNotBlank()) {
                Text("Terem: $room")
            }
        }
    }
}

object SubstitutionTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Helyettesítés"
            val icon = rememberVectorPainter(Icons.Default.SwapCalls)
            return remember {
                TabOptions(index = 2u, title = title, icon = icon)
            }
        }

    @Composable
    override fun Content() {
        SubstitutionScreen()
    }
}

private fun normalizeDate(value: String): String = value.take(10)

private fun formatDate(value: String): String {
    val parts = normalizeDate(value).split("-")
    return if (parts.size == 3) {
        "${parts[0]}. ${parts[1]}. ${parts[2]}."
    } else {
        value
    }
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
