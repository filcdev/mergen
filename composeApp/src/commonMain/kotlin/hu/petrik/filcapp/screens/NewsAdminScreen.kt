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
import hu.petrik.filcapp.admin.NewsAdminItem
import hu.petrik.filcapp.admin.NewsAdminKiosk
import hu.petrik.filcapp.admin.NewsAdminRepository
import hu.petrik.filcapp.admin.NewsAdminType
import hu.petrik.filcapp.admin.can
import hu.petrik.filcapp.admin.canManageSubstitutions
import hu.petrik.filcapp.admin.canOpenAdmin
import hu.petrik.filcapp.admin.newsLocalDay
import hu.petrik.filcapp.admin.plainNewsBlock
import hu.petrik.filcapp.admin.plainNewsText
import hu.petrik.filcapp.admin.supportedNewsImageMime
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.data.CohortDto
import hu.petrik.filcapp.settings.tr
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** The existing substitution screen remains intact; this is just a permission-gated hub. */
@Composable
fun AdminHubScreen(onBack: () -> Unit) {
    val user = AuthState.user
    if (user?.canOpenAdmin() != true) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text(tr("Nincs adminisztrációs jogosultság.", "No administrative permission."))
            OutlinedButton(onClick = onBack) { Text(tr("Vissza", "Back")) }
        }
        return
    }
    var page by remember(user.id) { mutableStateOf(0) }
    when (page) {
        1 -> SubstitutionAdminScreen(onBack = { page = 0 })
        2 -> NewsAdminScreen(NewsAdminType.ANNOUNCEMENT, onBack = { page = 0 })
        3 -> NewsAdminScreen(NewsAdminType.SYSTEM_MESSAGE, onBack = { page = 0 })
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                tr("Adminisztráció", "Administration"),
                style = MaterialTheme.typography.headlineSmall
            )
            if (user.canManageSubstitutions()) {
                OutlinedButton(onClick = { page = 1 }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Helyettesítések kezelése", "Manage substitutions"))
                }
            }
            if (user.permissions.can(AdminPermissions.ANNOUNCEMENTS_CREATE)) {
                OutlinedButton(onClick = { page = 2 }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Hírek és közlemények", "News and announcements"))
                }
            }
            if (user.permissions.can(AdminPermissions.SYSTEM_MESSAGES_MANAGE)) {
                OutlinedButton(onClick = { page = 3 }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Rendszerüzenetek", "System messages"))
                }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Vissza", "Back"))
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun NewsAdminScreen(kind: NewsAdminType, onBack: () -> Unit) {
    val user = AuthState.user
    val permission = if (kind == NewsAdminType.ANNOUNCEMENT) {
        AdminPermissions.ANNOUNCEMENTS_CREATE
    } else {
        AdminPermissions.SYSTEM_MESSAGES_MANAGE
    }
    if (user == null || !user.permissions.can(permission)) {
        Column(Modifier.padding(20.dp)) {
            Text(tr("Nincs jogosultság", "Not authorized"))
            OutlinedButton(onClick = onBack) { Text(tr("Vissza", "Back")) }
        }
        return
    }
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var items by remember(kind, user.id) { mutableStateOf(emptyList<NewsAdminItem>()) }
    var cohorts by remember(kind, user.id) { mutableStateOf(emptyList<CohortDto>()) }
    var kiosks by remember(kind, user.id) { mutableStateOf(emptyList<NewsAdminKiosk>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var showExpired by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<NewsAdminItem?>(null) }
    var editorVisible by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<NewsAdminItem?>(null) }
    var deleteBusy by remember { mutableStateOf(false) }
    var pickedImage by remember { mutableStateOf<ByteArray?>(null) }
    var pickedName by remember { mutableStateOf<String?>(null) }
    var pickerError by remember { mutableStateOf<String?>(null) }
    val today = Clock.System.todayIn(TimeZone.of("Europe/Budapest")).toString()
    val canManageKiosks = user.permissions.can(AdminPermissions.KIOSKS_MANAGE)

    // Keep picker anchored to a persistent screen, not a ModalBottomSheet (important on iOS).
    val imagePicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file != null) {
            scope.launch {
                try {
                    val bytes = file.readBytes()
                    require(bytes.size <= 8 * 1024 * 1024) { "A kép legfeljebb 8 MiB lehet." }
                    require(supportedNewsImageMime(bytes) != null) { "Csak JPG, PNG vagy WebP tölthető fel." }
                    pickedImage = bytes
                    pickedName = file.name
                    pickerError = null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    pickerError =
                        e.message ?: tr("Nem sikerült beolvasni a képet.", "Could not read image.")
                }
            }
        }
    }

    LaunchedEffect(kind, user.id, reload) {
        loading = true
        error = null
        try {
            items = NewsAdminRepository.list(kind)
            cohorts = NewsAdminRepository.cohorts()
            if (kind == NewsAdminType.ANNOUNCEMENT && canManageKiosks) {
                kiosks = NewsAdminRepository.tvKiosks()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: tr("Betöltési hiba", "Loading failed")
        } finally {
            loading = false
        }
    }
    val shown = items.filter { item ->
        (showExpired || newsLocalDay(item.validUntil) >= today) &&
            (item.title.orEmpty() + " " + plainNewsText(item.content))
                .contains(search.trim(), ignoreCase = true)
    }.sortedByDescending { it.validFrom }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (kind == NewsAdminType.ANNOUNCEMENT) tr(
                    "Közlemények",
                    "Announcements"
                ) else tr("Rendszerüzenetek", "System messages"),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedButton(onClick = onBack) { Text(tr("Vissza", "Back")) }
        }
        Button(onClick = {
            editing = null
            pickedImage = null
            pickedName = null
            pickerError = null
            editorVisible = true
        }, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (kind == NewsAdminType.ANNOUNCEMENT) {
                    tr("+ Új közlemény", "+ New announcement")
                } else {
                    tr("+ Új rendszerüzenet", "+ New system message")
                },
            )
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text(tr("Keresés", "Search")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showExpired, onCheckedChange = { showExpired = it })
            Text(tr("Lejárt elemek megjelenítése", "Show expired items"))
        }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { reload++ }) { Text(tr("Újrapróbálás", "Retry")) }
        }
        if (loading) {
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (shown.isEmpty()) {
            Text(tr("Nincs megjeleníthető elem", "Nothing to display"))
        }
        shown.forEach { item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        item.title.orEmpty()
                            .ifBlank { tr("Cím nélküli közlemény", "Untitled announcement") },
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        plainNewsText(item.content).take(180),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${newsLocalDay(item.validFrom)} – ${newsLocalDay(item.validUntil)}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    val names = cohorts.filter { it.id in item.cohortIds }
                        .joinToString { it.short.ifBlank { it.name } }
                    Text(
                        "${tr("Célközönség", "Audience")}: ${
                            names.ifBlank {
                                tr(
                                    "Mindenki",
                                    "Everyone"
                                )
                            }
                        }"
                    )
                    if (item.imageKey != null) Text(
                        tr("Kép csatolva", "Image attached"),
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            editing = item
                            pickedImage = null
                            pickedName = null
                            pickerError = null
                            editorVisible = true
                        }) { Text(tr("Szerkesztés", "Edit")) }
                        OutlinedButton(onClick = { deleting = item }) {
                            Text(
                                tr(
                                    "Törlés",
                                    "Delete"
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    if (editorVisible) {
        NewsEditorSheet(
            kind = kind,
            item = editing,
            cohorts = cohorts,
            kiosks = kiosks,
            canManageKiosks = canManageKiosks,
            selectedImageName = pickedName,
            imageBytes = pickedImage,
            imageError = pickerError,
            onPickImage = { imagePicker.launch() },
            onRemoveSelectedImage = { pickedImage = null; pickedName = null },
            onDismiss = { editorVisible = false },
            onSaved = {
                editorVisible = false
                notice = tr("Sikeres mentés", "Saved successfully")
                reload++
            },
        )
    }
    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!deleteBusy) deleting = null },
            title = { Text(tr("Törlés", "Delete")) },
            text = { Text(tr("Biztosan törlöd?", "Delete permanently?")) },
            confirmButton = {
                Button(enabled = !deleteBusy, onClick = {
                    scope.launch {
                        deleteBusy = true
                        try {
                            NewsAdminRepository.delete(kind, target.id)
                            deleting = null
                            notice = tr("Az elem törölve.", "Item deleted.")
                            reload++
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            deleteBusy = false
                        }
                    }
                }) { Text(tr("Törlés", "Delete")) }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !deleteBusy,
                    onClick = { deleting = null }) { Text(tr("Mégse", "Cancel")) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun NewsEditorSheet(
    kind: NewsAdminType,
    item: NewsAdminItem?,
    cohorts: List<CohortDto>,
    kiosks: List<NewsAdminKiosk>,
    canManageKiosks: Boolean,
    selectedImageName: String?,
    imageBytes: ByteArray?,
    imageError: String?,
    onPickImage: () -> Unit,
    onRemoveSelectedImage: () -> Unit,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val today = Clock.System.todayIn(TimeZone.of("Europe/Budapest")).toString()
    var persistedId by remember(item?.id) { mutableStateOf(item?.id) }
    var title by remember(item?.id) { mutableStateOf(item?.title.orEmpty()) }
    var body by remember(item?.id) { mutableStateOf(item?.content?.let(::plainNewsText).orEmpty()) }
    var bodyTouched by remember(item?.id) { mutableStateOf(false) }
    var from by remember(item?.id) { mutableStateOf(item?.validFrom?.let(::newsLocalDay) ?: today) }
    var until by remember(item?.id) {
        mutableStateOf(
            item?.validUntil?.let(::newsLocalDay) ?: today
        )
    }
    var selectedCohorts by remember(item?.id) { mutableStateOf(item?.cohortIds.orEmpty().toSet()) }
    var highlighted by remember(item?.id) { mutableStateOf(item?.highlighted ?: false) }
    var kioskOnly by remember(item?.id) { mutableStateOf(item?.kioskOnly ?: false) }
    var selectedKiosks by remember(item?.id) { mutableStateOf(item?.kioskIds.orEmpty().toSet()) }
    var imagePresent by remember(item?.id) { mutableStateOf(item?.imageKey != null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cohortQuery by remember { mutableStateOf("") }
    val datesValid = runCatching {
        kotlinx.datetime.LocalDate.parse(from)
        kotlinx.datetime.LocalDate.parse(until)
        until >= from
    }.getOrDefault(false)
    val valid = datesValid && (kind != NewsAdminType.SYSTEM_MESSAGE || title.isNotBlank()) &&
        !(kind == NewsAdminType.ANNOUNCEMENT && !item?.title.isNullOrBlank() && title.isBlank()) &&
        !(kind == NewsAdminType.ANNOUNCEMENT && (highlighted || imageBytes != null || imagePresent) && title.isBlank()) &&
        (body.isNotBlank() || (item != null && !bodyTouched))

    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (item == null) tr(
                    "Új elem létrehozása",
                    "Create new item"
                ) else tr("Elem szerkesztése", "Edit item"),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = {
                    Text(
                        if (kind == NewsAdminType.SYSTEM_MESSAGE) tr(
                            "Cím *",
                            "Title *"
                        ) else tr("Cím", "Title")
                    )
                },
                modifier = Modifier.fillMaxWidth(), enabled = !saving, singleLine = true
            )
            OutlinedTextField(
                value = body, onValueChange = { body = it; bodyTouched = true },
                label = { Text(tr("Tartalom", "Content")) }, modifier = Modifier.fillMaxWidth(),
                minLines = 4, enabled = !saving
            )
            OutlinedTextField(
                value = from, onValueChange = { from = it },
                label = { Text(tr("Kezdőnap (ÉÉÉÉ-HH-NN)", "Start date (YYYY-MM-DD)")) },
                modifier = Modifier.fillMaxWidth(), enabled = !saving, singleLine = true
            )
            OutlinedTextField(
                value = until, onValueChange = { until = it },
                label = { Text(tr("Zárónap (ÉÉÉÉ-HH-NN)", "End date (YYYY-MM-DD)")) },
                modifier = Modifier.fillMaxWidth(), enabled = !saving, singleLine = true
            )
            Text(
                tr(
                    "Célközönség (ha nincs kijelölés: mindenki)",
                    "Audience (none selected: everyone)"
                ), fontWeight = FontWeight.Medium
            )
            OutlinedTextField(
                value = cohortQuery,
                onValueChange = { cohortQuery = it },
                label = { Text(tr("Osztály keresése", "Search classes")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedButton(onClick = { selectedCohorts = emptySet() }, enabled = !saving) {
                Text(tr("Mindenki", "Everyone"))
            }
            cohorts.filter {
                it.name.contains(cohortQuery, true) || it.short.contains(
                    cohortQuery,
                    true
                )
            }
                .forEach { cohort ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = cohort.id in selectedCohorts, enabled = !saving,
                            onCheckedChange = { checked ->
                                selectedCohorts =
                                    if (checked) selectedCohorts + cohort.id else selectedCohorts - cohort.id
                            })
                        Text(cohort.short.ifBlank { cohort.name })
                    }
                }
            if (kind == NewsAdminType.ANNOUNCEMENT) {
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = highlighted,
                        onCheckedChange = { highlighted = it },
                        enabled = !saving
                    )
                    Text(
                        tr("Kiemelt közlemény", "Highlighted announcement"),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = kioskOnly,
                        onCheckedChange = { kioskOnly = it },
                        enabled = !saving
                    )
                    Text(
                        tr("Csak kioszkokon", "Kiosk only"),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (canManageKiosks) {
                    Text(tr("Érintett TV-kioszkok (üres = mind)", "TV kiosks (none = all)"))
                    kiosks.forEach { kiosk ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = kiosk.id in selectedKiosks, enabled = !saving,
                                onCheckedChange = { checked ->
                                    selectedKiosks =
                                        if (checked) selectedKiosks + kiosk.id else selectedKiosks - kiosk.id
                                })
                            Text(kiosk.name)
                        }
                    }
                }
                OutlinedButton(onClick = onPickImage, enabled = !saving) {
                    Text(tr("Kép kiválasztása (JPG, PNG, WebP)", "Choose image (JPG, PNG, WebP)"))
                }
                selectedImageName?.let {
                    Text("${tr("Kiválasztott kép", "Selected image")}: $it")
                    OutlinedButton(onClick = onRemoveSelectedImage, enabled = !saving) {
                        Text(tr("Kiválasztás törlése", "Clear selection"))
                    }
                }
                imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (imagePresent && persistedId != null) {
                    OutlinedButton(enabled = !saving, onClick = {
                        scope.launch {
                            saving = true
                            try {
                                NewsAdminRepository.deleteImage(persistedId!!)
                                imagePresent = false
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                saving = false
                            }
                        }
                    }) { Text(tr("Meglévő kép eltávolítása", "Remove existing image")) }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!datesValid) Text(
                tr("Ellenőrizd a dátumokat!", "Check the dates!"),
                color = MaterialTheme.colorScheme.error
            )
            Button(enabled = valid && !saving, modifier = Modifier.fillMaxWidth(), onClick = {
                scope.launch {
                    saving = true
                    error = null
                    try {
                        val content =
                            if (item != null && !bodyTouched && item.content is kotlinx.serialization.json.JsonArray) {
                                item.content
                            } else {
                                plainNewsBlock(body)
                            }
                        val saved = NewsAdminRepository.save(
                            kind = kind,
                            existingId = persistedId,
                            title = title,
                            content = content,
                            validFrom = from,
                            validUntil = until,
                            cohortIds = selectedCohorts.toList(),
                            highlighted = highlighted,
                            kioskOnly = kioskOnly,
                            kioskIds = selectedKiosks.toList(),
                        )
                        // Keep ID after a successful creation so retrying a failed upload updates, not duplicates, the row.
                        persistedId = saved.id
                        if (kind == NewsAdminType.ANNOUNCEMENT && imageBytes != null) {
                            NewsAdminRepository.uploadImage(
                                saved.id,
                                selectedImageName ?: "image.jpg",
                                imageBytes
                            )
                        }
                        onSaved()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = e.message ?: tr("Nem sikerült a mentés.", "Save failed.")
                    } finally {
                        saving = false
                    }
                }
            }) {
                if (saving) CircularProgressIndicator() else Text(tr("Mentés", "Save"))
            }
            OutlinedButton(
                enabled = !saving,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("Mégse", "Cancel")) }
        }
    }
}
