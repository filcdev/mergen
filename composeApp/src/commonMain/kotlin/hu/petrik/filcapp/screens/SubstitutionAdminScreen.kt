package hu.petrik.filcapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.admin.AdminPermissions
import hu.petrik.filcapp.admin.SubstitutionAdminRepository
import hu.petrik.filcapp.admin.can
import hu.petrik.filcapp.admin.canManageSubstitutions
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.components.SearchableSelection
import hu.petrik.filcapp.data.CohortDto
import hu.petrik.filcapp.data.PeriodDto
import hu.petrik.filcapp.data.SubjectDto
import hu.petrik.filcapp.data.SubstitutionItemDto
import hu.petrik.filcapp.data.TeacherDto
import hu.petrik.filcapp.data.cohortLabels
import hu.petrik.filcapp.settings.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Shared Compose Multiplatform admin UI, backed by the same Chronos endpoints as FILC web. */
@OptIn(ExperimentalTime::class)
@Composable
fun SubstitutionAdminScreen(onBack: () -> Unit) {
    val user = AuthState.user
    if (user?.canManageSubstitutions() != true) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("Nincs jogosultság", "No permission"), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = onBack) { Text(tr("Vissza", "Back")) }
        }
        return
    }
    val canCreate = user.permissions.can(AdminPermissions.SUBSTITUTION_CREATE)
    val canUpdate = user.permissions.can(AdminPermissions.SUBSTITUTION_UPDATE)
    val canDelete = user.permissions.can(AdminPermissions.SUBSTITUTION_DELETE)
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<SubstitutionItemDto>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<TeacherDto>>(emptyList()) }
    var cohorts by remember { mutableStateOf<List<CohortDto>>(emptyList()) }
    var periods by remember { mutableStateOf<List<PeriodDto>>(emptyList()) }
    var subjects by remember { mutableStateOf<List<SubjectDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var showPast by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var editedItem by remember { mutableStateOf<SubstitutionItemDto?>(null) }
    var deleteItem by remember { mutableStateOf<SubstitutionItemDto?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()).toString() }

    LaunchedEffect(user.id, reload) {
        loading = true
        error = null
        try {
            items = SubstitutionAdminRepository.list()
            teachers = SubstitutionAdminRepository.teachers()
            // The manual dialog needs the same reference data as FILC web.
            if (canCreate) {
                cohorts = SubstitutionAdminRepository.cohorts()
                periods = SubstitutionAdminRepository.periods()
                subjects = SubstitutionAdminRepository.subjects()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: tr("Nem sikerült betölteni az adatokat.", "Could not load data.")
        } finally {
            loading = false
        }
    }
    val filtered =
        items.filter { item ->
            val dateOk = showPast || item.substitution.date.take(10) >= today
            val text =
                listOf(
                    item.teacher?.displayName.orEmpty(),
                    item.substitution.date,
                    item.substitution.comment.orEmpty(),
                    item.lessons.joinToString(" ") { lesson ->
                        "${lesson.subject?.name.orEmpty()} ${lesson.cohorts.cohortLabels()} ${lesson.teachers.joinToString { it.name }}"
                    },
                ).joinToString(" ")
            dateOk && text.contains(query.trim(), ignoreCase = true)
        }.sortedWith(compareBy<SubstitutionItemDto> { it.substitution.date }.thenBy { it.lessons.firstOrNull()?.period?.period ?: 0 })

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Helyettesítések kezelése", "Manage substitutions"), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = onBack) { Text(tr("Vissza", "Back")) }
        }
        if (canCreate) {
            Button(onClick = {
                editedItem = null
                showEditor = true
            }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("+ Új helyettesítés", "+ New substitution"))
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(tr("Keresés", "Search")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showPast, onCheckedChange = { showPast = it })
            Text(tr("Korábbi helyettesítések megjelenítése", "Show past substitutions"))
        }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { reload++ }) { Text(tr("Újra", "Retry")) }
        }
        if (loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (filtered.isEmpty()) {
            Text(tr("Nincs megjeleníthető helyettesítés.", "No matching substitutions."))
        }
        filtered.forEach { item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val firstLesson = item.lessons.firstOrNull()
                    Text(
                        "${item.substitution.date.take(10)}  •  ${firstLesson?.period?.period ?: "–"}. ${tr("óra", "period")}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        item.lessons.joinToString(", ") { lesson ->
                            lesson.subject?.short?.ifBlank { lesson.subject?.name.orEmpty() }
                                ?: lesson.subject?.name.orEmpty()
                        },
                    )
                    val cohortLabels = item.lessons.flatMap { it.cohorts }.cohortLabels()
                    Text("${tr("Osztály", "Class")}: ${cohortLabels.ifBlank { "–" }}")
                    val absent = item.lessons.flatMap { it.teachers }.map { it.name }.distinct().joinToString(", ")
                    Text("${tr("Hiányzó tanár", "Absent teacher")}: ${absent.ifBlank { "–" }}")
                    Text(
                        "${tr("Helyettesítő", "Substitute")}: ${item.teacher?.displayName ?: tr("Elmarad", "Cancelled")}",
                        color = MaterialTheme.colorScheme.primary,
                    )
                    item.substitution.comment?.takeIf(String::isNotBlank)?.let { Text(it) }
                    if (canUpdate || canDelete) {
                        HorizontalDivider()
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (canUpdate) {
                                OutlinedButton(onClick = {
                                    editedItem = item
                                    showEditor = true
                                }) { Text(tr("Szerkesztés", "Edit")) }
                            }
                            if (canDelete) {
                                OutlinedButton(onClick = { deleteItem = item }) { Text(tr("Törlés", "Delete")) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        SubstitutionEditorSheet(
            item = editedItem,
            teachers = teachers,
            cohorts = cohorts,
            periods = periods,
            subjects = subjects,
            onDismiss = { showEditor = false },
            onSaved = {
                showEditor = false
                notice = tr("A helyettesítés mentve.", "Substitution saved.")
                reload++
            },
        )
    }
    deleteItem?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!deleting) deleteItem = null },
            title = { Text(tr("Helyettesítés törlése", "Delete substitution")) },
            text = { Text(tr("Biztosan törlöd ezt a helyettesítést?", "Delete this substitution permanently?")) },
            confirmButton = {
                Button(enabled = !deleting, onClick = {
                    if (!canDelete) return@Button
                    scope.launch {
                        deleting = true
                        try {
                            SubstitutionAdminRepository.delete(target.substitution.id)
                            deleteItem = null
                            notice = tr("A helyettesítés törölve.", "Substitution deleted.")
                            reload++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            deleting = false
                        }
                    }
                }) { Text(tr("Törlés", "Delete")) }
            },
            dismissButton = {
                OutlinedButton(enabled = !deleting, onClick = { deleteItem = null }) { Text(tr("Mégse", "Cancel")) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun SubstitutionEditorSheet(
    item: SubstitutionItemDto?,
    teachers: List<TeacherDto>,
    cohorts: List<CohortDto>,
    periods: List<PeriodDto>,
    subjects: List<SubjectDto>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val editing = item != null
    var manual by remember(item?.substitution?.id) { mutableStateOf(false) }
    var date by remember(item?.substitution?.id) {
        mutableStateOf(item?.substitution?.date?.take(10) ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toString())
    }
    var comment by remember(item?.substitution?.id) { mutableStateOf(item?.substitution?.comment.orEmpty()) }
    var absentTeacher by remember(item?.substitution?.id) { mutableStateOf(item?.lessons?.firstOrNull()?.teachers?.firstOrNull()?.id) }
    var lessonIds by remember(item?.substitution?.id) { mutableStateOf(item?.lessons?.map { it.id }?.toSet().orEmpty()) }
    var substituter by remember(item?.substitution?.id) { mutableStateOf(item?.substitution?.substituter) }
    var manualTeacher by remember { mutableStateOf<String?>(null) }
    var manualCohort by remember { mutableStateOf<String?>(null) }
    var manualPeriod by remember { mutableStateOf<String?>(null) }
    var manualSubject by remember { mutableStateOf<String?>(null) }
    var manualSubstituter by remember { mutableStateOf<String?>(null) }
    var candidates by remember { mutableStateOf(SubstitutionAdminRepository.Candidates()) }
    var candidateLoading by remember { mutableStateOf(false) }
    var candidateError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val validDate = remember(date) { runCatching { LocalDate.parse(date) }.isSuccess }

    // The server returns eligible lessons, parallel co-teachers and H1/H2 substitution suggestions.
    LaunchedEffect(manual, date, absentTeacher, lessonIds, teachers) {
        candidates = SubstitutionAdminRepository.Candidates()
        candidateError = null
        if (!manual && validDate && !absentTeacher.isNullOrBlank() && teachers.isNotEmpty()) {
            candidateLoading = true
            try {
                candidates =
                    SubstitutionAdminRepository.candidates(
                        date,
                        absentTeacher.orEmpty(),
                        lessonIds.toList(),
                        teachers.map { it.id },
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                candidateError = e.message
            } finally {
                candidateLoading = false
            }
        }
    }
    val visibleLessons =
        (
            candidates.availableLessons +
                item?.lessons.orEmpty().map { original ->
                    hu.petrik.filcapp.data.LessonDto(
                        id = original.id,
                        classrooms = original.classrooms,
                        cohorts = original.cohorts,
                        day = original.day,
                        period = original.period,
                        subject = original.subject,
                        teachers = original.teachers,
                    )
                }
        ).distinctBy { it.id }.sortedBy { it.period?.period ?: 0 }
    val selectedSubjectIds = visibleLessons.filter { it.id in lessonIds }.mapNotNull { it.subject?.id }.toSet()
    val merged =
        (
            visibleLessons.filter { it.id in lessonIds }.flatMap { it.teachers } +
                candidates.parallelLessons.filter { it.subject?.id in selectedSubjectIds }.flatMap { it.teachers }
        ).filter { it.id != absentTeacher }.distinctBy { it.id }
    val sortedCandidates =
        candidates.substituteCandidates.sortedWith(
            compareByDescending<SubstitutionAdminRepository.CandidateTeacher> { it.hasH1 }
                .thenByDescending { it.hasH2 }
                .thenBy { it.teacher.displayName },
        )
    val substituteOptions =
        (
            merged.map { it.id to "${tr("Összevonás", "Merged")}: ${it.name}" } +
                sortedCandidates.filter { it.teacher.id != absentTeacher }.map { candidate ->
                    val tag =
                        when {
                            candidate.hasH1 && candidate.hasH2 -> "H1/H2"
                            candidate.hasH1 -> "H1"
                            candidate.hasH2 -> "H2"
                            else -> ""
                        }
                    candidate.teacher.id to "${candidate.teacher.displayName} ${if (tag.isNotEmpty()) "($tag)" else ""}"
                }
        ).distinctBy { it.first }

    val valid =
        validDate &&
            if (manual) {
                !manualTeacher.isNullOrBlank() && !manualPeriod.isNullOrBlank() && !manualCohort.isNullOrBlank()
            } else {
                lessonIds.isNotEmpty() && !absentTeacher.isNullOrBlank() && !candidateLoading && candidateError == null
            }

    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (editing) tr("Helyettesítés módosítása", "Edit substitution") else tr("Új helyettesítés", "New substitution"),
                style = MaterialTheme.typography.titleLarge,
            )
            if (!editing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tr("Kézi rögzítés", "Manual entry"))
                    Switch(checked = manual, enabled = !saving, onCheckedChange = { manual = it })
                }
            }
            OutlinedTextField(
                value = date,
                onValueChange = {
                    date = it
                    lessonIds = emptySet()
                    substituter = null
                },
                label = { Text(tr("Dátum (ÉÉÉÉ-HH-NN)", "Date (YYYY-MM-DD)")) },
                isError = !validDate,
                enabled = !saving,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (manual) {
                SearchableSelection(
                    options = teachers.map { it.id to "${it.displayName} (${it.short})" },
                    selectedId = manualTeacher,
                    label = tr("Hiányzó tanár", "Absent teacher"),
                    placeholder = tr("Válassz tanárt", "Select teacher"),
                    onSelected = {
                        manualTeacher = it
                        manualSubstituter = null
                    },
                )
                SearchableSelection(
                    options = periods.map { it.id to "${it.period}. (${it.startTime.take(5)}–${it.endTime.take(5)})" },
                    selectedId = manualPeriod,
                    label = tr("Óra", "Period"),
                    placeholder = tr("Válassz órát", "Select period"),
                    onSelected = { manualPeriod = it },
                )
                SearchableSelection(
                    options = subjects.map { it.id to "${it.name} (${it.short})" },
                    selectedId = manualSubject,
                    label = tr("Tantárgy (nem kötelező)", "Subject (optional)"),
                    placeholder = tr("Válassz tantárgyat", "Select subject"),
                    allowClear = true,
                    onSelected = { manualSubject = it },
                )
                SearchableSelection(
                    options = cohorts.map { it.id to it.name },
                    selectedId = manualCohort,
                    label = tr("Osztály", "Class"),
                    placeholder = tr("Válassz osztályt", "Select class"),
                    onSelected = { manualCohort = it },
                )
                SearchableSelection(
                    options = teachers.filter { it.id != manualTeacher }.map { it.id to it.displayName },
                    selectedId = manualSubstituter,
                    label = tr("Helyettesítő (üres = elmarad)", "Substitute (empty = cancelled)"),
                    placeholder = tr("Elmarad / válassz tanárt", "Cancelled / select teacher"),
                    allowClear = true,
                    onSelected = { manualSubstituter = it },
                )
            } else {
                SearchableSelection(
                    options = teachers.map { it.id to "${it.displayName} (${it.short})" },
                    selectedId = absentTeacher,
                    label = tr("Hiányzó tanár", "Absent teacher"),
                    placeholder = tr("Válassz tanárt", "Select teacher"),
                    onSelected = {
                        absentTeacher = it
                        lessonIds = emptySet()
                        substituter = null
                    },
                )
                Text(tr("Érintett órák", "Lessons to substitute"), fontWeight = FontWeight.SemiBold)
                if (candidateLoading) CircularProgressIndicator()
                candidateError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Column(Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    if (visibleLessons.isEmpty() && !candidateLoading) {
                        Text(tr("Nem található óra.", "No lessons available."))
                    }
                    visibleLessons.forEach { lesson ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = lesson.id in lessonIds,
                                enabled = !saving,
                                onCheckedChange = { checked ->
                                    lessonIds = if (checked) lessonIds + lesson.id else lessonIds - lesson.id
                                    substituter = null
                                },
                            )
                            val lessonLabel =
                                "${lesson.period?.period ?: "–"}. • " +
                                    "${lesson.subject?.short ?: lesson.subject?.name.orEmpty()} • " +
                                    lesson.cohorts.cohortLabels()
                            Text(lessonLabel, modifier = Modifier.weight(1f))
                        }
                    }
                }
                SearchableSelection(
                    options = substituteOptions,
                    selectedId = substituter,
                    label = tr("Helyettesítő (üres = elmarad)", "Substitute (empty = cancelled)"),
                    placeholder = tr("Elmarad / válassz tanárt", "Cancelled / select teacher"),
                    allowClear = true,
                    onSelected = { substituter = it },
                )
                Text(
                    tr(
                        "A helyettesítők listáját a Chronos számítja (H1/H2, összevonás, ütközések).",
                        "Suggestions are calculated by Chronos (H1/H2, merge, conflicts).",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(tr("Megjegyzés", "Comment")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                minLines = 2,
            )
            saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !saving, onClick = onDismiss) { Text(tr("Mégse", "Cancel")) }
                Button(enabled = valid && !saving, onClick = {
                    scope.launch {
                        saving = true
                        saveError = null
                        try {
                            if (manual) {
                                SubstitutionAdminRepository.createManual(
                                    date,
                                    manualTeacher.orEmpty(),
                                    manualPeriod.orEmpty(),
                                    manualSubject,
                                    manualCohort.orEmpty(),
                                    manualSubstituter,
                                    comment.ifBlank { null },
                                )
                            } else if (item != null) {
                                SubstitutionAdminRepository.update(
                                    item.substitution.id,
                                    date,
                                    lessonIds.toList(),
                                    substituter,
                                    comment.ifBlank { null },
                                )
                            } else {
                                SubstitutionAdminRepository.create(date, lessonIds.toList(), substituter, comment.ifBlank { null })
                            }
                            onSaved()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            saveError = e.message ?: tr("Nem sikerült menteni.", "Save failed.")
                        } finally {
                            saving = false
                        }
                    }
                }) { Text(if (saving) tr("Mentés…", "Saving…") else tr("Mentés", "Save")) }
            }
        }
    }
}
