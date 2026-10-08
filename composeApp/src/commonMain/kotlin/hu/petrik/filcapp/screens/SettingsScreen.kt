package hu.petrik.filcapp.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import hu.petrik.filcapp.auth.AuthManager
import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.components.SearchableSelection
import hu.petrik.filcapp.network.CohortDto
import hu.petrik.filcapp.network.FilcPublicApi
import hu.petrik.filcapp.network.GroupDto
import hu.petrik.filcapp.settings.AppLanguage
import hu.petrik.filcapp.settings.AppSettings
import hu.petrik.filcapp.settings.AppThemeMode
import hu.petrik.filcapp.settings.tr
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private object SettingsSessionCache {
    var userId: String? = null
    var cohorts: List<CohortDto> = emptyList()
    var cohortsLoaded = false
    var cohortId: String? = null
    var groups: List<GroupDto> = emptyList()
    var groupsLoaded = false

    fun clear() {
        userId = null
        cohorts = emptyList()
        cohortsLoaded = false
        cohortId = null
        groups = emptyList()
        groupsLoaded = false
    }
}

@Composable
fun SettingsScreen() {
    val scope = rememberCoroutineScope()
    val currentUserId = AuthState.user?.id
    val cachedProfile = SettingsSessionCache.userId == currentUserId

    var cohorts by remember(currentUserId) {
        mutableStateOf(
            if (cachedProfile) {
                SettingsSessionCache.cohorts
            } else {
                emptyList()
            },
        )
    }

    var groups by remember(currentUserId) {
        mutableStateOf(
            if (cachedProfile) {
                SettingsSessionCache.groups
            } else {
                emptyList()
            },
        )
    }

    var nickname by remember(AuthState.user?.id, AuthState.user?.nickname) {
        mutableStateOf(AuthState.user?.nickname.orEmpty())
    }

    var profileLoading by remember(currentUserId) {
        mutableStateOf(
            currentUserId != null &&
                !(cachedProfile && SettingsSessionCache.cohortsLoaded),
        )
    }

    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(AuthState.user?.id, reloadKey) {
        val userId = AuthState.user?.id

        if (userId != null) {
            if (
                reloadKey == 0 &&
                SettingsSessionCache.userId == userId &&
                SettingsSessionCache.cohortsLoaded
            ) {
                cohorts = SettingsSessionCache.cohorts
                profileLoading = false
                return@LaunchedEffect
            }

            profileLoading = true

            runCatching {
                val latest = FilcPublicApi.getLatestValidTimetable()
                FilcPublicApi.getCohorts(latest.id).sortedBy { it.name }
            }.onSuccess { loaded ->
                cohorts = loaded
                SettingsSessionCache.userId = userId
                SettingsSessionCache.cohorts = loaded
                SettingsSessionCache.cohortsLoaded = true
            }

            profileLoading = false
        } else {
            SettingsSessionCache.clear()
            cohorts = emptyList()
            groups = emptyList()
        }
    }

    LaunchedEffect(AuthState.profile, AuthState.callbackVersion, reloadKey) {
        val userId = AuthState.user?.id
        val cohortId = AuthState.profile?.cohort?.id

        if (cohortId == null) {
            groups = emptyList()
            SettingsSessionCache.cohortId = null
            SettingsSessionCache.groups = emptyList()
            SettingsSessionCache.groupsLoaded = true
            return@LaunchedEffect
        }

        if (
            reloadKey == 0 &&
            SettingsSessionCache.userId == userId &&
            SettingsSessionCache.cohortId == cohortId &&
            SettingsSessionCache.groupsLoaded
        ) {
            groups = SettingsSessionCache.groups
            return@LaunchedEffect
        }

        groups =
            runCatching {
                FilcPublicApi.getGroupsForCohort(cohortId)
            }.getOrDefault(emptyList())

        SettingsSessionCache.userId = userId
        SettingsSessionCache.cohortId = cohortId
        SettingsSessionCache.groups = groups
        SettingsSessionCache.groupsLoaded = true
    }

    PullToRefreshBox(
        isRefreshing = profileLoading,
        onRefresh = { reloadKey++ },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsHeader()

            ProfileSettingsCard(
                nickname = nickname,
                onNicknameChange = { nickname = it },
                cohorts = cohorts,
                groups = groups,
                loading = profileLoading,
                onSignIn = { scope.launch { AuthManager.signInWithMicrosoft() } },
                onSignOut = { scope.launch { AuthManager.signOut() } },
                onSaveNickname = {
                    scope.launch {
                        AuthManager.updateUser(
                            nickname = nickname.trim().ifBlank { null },
                            cohortId = AuthState.user?.cohortId,
                        )
                    }
                },
                onCohortSelected = { cohortId ->
                    scope.launch {
                        AuthManager.updateUser(
                            nickname = AuthState.user?.nickname,
                            cohortId = cohortId,
                        )
                    }
                },
                onGroupSelected = { groupId ->
                    scope.launch {
                        AuthManager.selectGroup(groupId)
                    }
                },
            )

            AuthState.error?.let { message ->
                SettingsSurface {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            SettingsSectionTitle(
                icon = Icons.Default.Palette,
                title = tr("Megjelenés", "Appearance"),
                subtitle =
                    tr(
                        "A módosítások azonnal megjelennek az egész alkalmazásban.",
                        "Changes are applied across the app instantly.",
                    ),
            )

            ThemeModeCard()
            PrimaryColorCard()

            SettingsSectionTitle(
                icon = Icons.Default.Language,
                title = tr("Nyelv", "Language"),
                subtitle =
                    tr(
                        "Az alkalmazás nyelve azonnal változik.",
                        "The app language changes immediately.",
                    ),
            )

            LanguageCard(
                onHungarian = {
                    AppSettings.setLanguage(AppLanguage.HU)
                    scope.launch { AuthManager.syncLanguage("hu") }
                },
                onEnglish = {
                    AppSettings.setLanguage(AppLanguage.EN)
                    scope.launch { AuthManager.syncLanguage("en") }
                },
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = tr("PROFIL ÉS BEÁLLÍTÁSOK", "PROFILE & SETTINGS"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = tr("Beállítások", "Settings"),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ProfileSettingsCard(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    cohorts: List<CohortDto>,
    groups: List<GroupDto>,
    loading: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSaveNickname: () -> Unit,
    onCohortSelected: (String) -> Unit,
    onGroupSelected: (String) -> Unit,
) {
    SettingsSurface {
        if (!AuthState.signedIn) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsIconBox(Icons.Default.AccountCircle)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Microsoft-fiók", "Microsoft account"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text =
                            tr(
                                "Jelentkezz be a petrikes Microsoft-fiókoddal.",
                                "Sign in with your Petrik Microsoft account.",
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onSignIn,
                enabled = !AuthState.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(tr("Bejelentkezés Microsofttal", "Sign in with Microsoft"))
            }

            if (AuthState.loading) {
                CircularProgressIndicator()
            }

            return@SettingsSurface
        }

        val user = AuthState.user ?: return@SettingsSurface
        val displayName =
            user.nickname
                ?.takeIf { it.isNotBlank() }
                ?: user.preferredName

        val profileLabel =
            AuthState.profile?.teacher?.displayName
                ?: AuthState.profile?.cohort?.name
                ?: tr("Petrik profil", "Petrik profile")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(54.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            shape = CircleShape,
                        )
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "P",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = profileLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("Megjelenő név", "Preferred name")) },
            singleLine = true,
        )

        OutlinedButton(
            onClick = onSaveNickname,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(tr("Név mentése", "Save name"))
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            SearchableSelection(
                options = cohorts.map { it.id to it.name },
                selectedId = AuthState.profile?.cohort?.id,
                label = tr("Saját osztály", "My class"),
                placeholder = tr("Válassz osztályt", "Select class"),
                onSelected = { it?.let(onCohortSelected) },
            )
        }

        val selectableGroups =
            groups.filter {
                !it.entireClass && it.divisionTag != null
            }

        if (selectableGroups.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = tr("Saját csoportok", "My groups"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                selectableGroups
                    .groupBy {
                        it.divisionLabel
                            ?: it.divisionTag
                            ?: tr("Csoport", "Group")
                    }.forEach { (division, divisionGroups) ->
                        Text(
                            text = division,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            divisionGroups.forEach { group ->
                                FilterChip(
                                    selected = group.selected,
                                    onClick = { onGroupSelected(group.id) },
                                    label = { Text(group.name) },
                                )
                            }
                        }
                    }
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(tr("Kijelentkezés", "Sign out"))
        }
    }
}

@Composable
private fun ThemeModeCard() {
    SettingsSurface {
        Text(
            text = tr("Téma", "Theme"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeModeButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.BrightnessAuto,
                title = tr("Rendszer", "System"),
                selected = AppSettings.themeMode.value == AppThemeMode.SYSTEM,
                onClick = { AppSettings.setThemeMode(AppThemeMode.SYSTEM) },
            )

            ThemeModeButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LightMode,
                title = tr("Világos", "Light"),
                selected = AppSettings.themeMode.value == AppThemeMode.LIGHT,
                onClick = { AppSettings.setThemeMode(AppThemeMode.LIGHT) },
            )

            ThemeModeButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DarkMode,
                title = tr("Sötét", "Dark"),
                selected = AppSettings.themeMode.value == AppThemeMode.DARK,
                onClick = { AppSettings.setThemeMode(AppThemeMode.DARK) },
            )
        }
    }
}

@Composable
private fun ThemeModeButton(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            modifier
                .height(78.dp)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        border =
            if (selected) {
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                )
            } else {
                null
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PrimaryColorCard() {
    val initialColor = Color(AppSettings.primaryColorArgb.value.toInt())
    val initialHsv = remember { colorToHsv(initialColor) }

    var hue by remember { mutableFloatStateOf(initialHsv.hue) }
    var saturation by remember { mutableFloatStateOf(initialHsv.saturation) }
    var value by remember { mutableFloatStateOf(initialHsv.value) }

    fun updatePrimary(
        newHue: Float = hue,
        newSaturation: Float = saturation,
        newValue: Float = value,
        persist: Boolean = false,
    ) {
        hue = newHue.coerceIn(0f, 360f)
        saturation = newSaturation.coerceIn(0f, 1f)
        value = newValue.coerceIn(0f, 1f)

        AppSettings.setPrimaryColor(
            value = colorToArgbLong(hsvToColor(hue, saturation, value)),
            persist = persist,
        )
    }

    SettingsSurface {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBox(Icons.Default.Palette)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Elsődleges szín", "Primary color"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text =
                        tr(
                            "Húzd a jelölőt – az egész alkalmazás azonnal követi.",
                            "Drag the selector – the whole app updates instantly.",
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f),
                            CircleShape,
                        ),
            )
        }

        val hueColor = hsvToColor(hue, 1f, 1f)
        val currentColor = hsvToColor(hue, saturation, value)

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        fun updateFromOffset(offset: Offset) {
                            val newSaturation =
                                (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val newValue =
                                (1f - offset.y / size.height.toFloat()).coerceIn(0f, 1f)

                            updatePrimary(
                                newSaturation = newSaturation,
                                newValue = newValue,
                            )
                        }

                        detectTapGestures { offset ->
                            updateFromOffset(offset)
                            AppSettings.persistPrimaryColor()
                        }
                    }.pointerInput(Unit) {
                        fun updateFromOffset(offset: Offset) {
                            val newSaturation =
                                (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val newValue =
                                (1f - offset.y / size.height.toFloat()).coerceIn(0f, 1f)

                            updatePrimary(
                                newSaturation = newSaturation,
                                newValue = newValue,
                            )
                        }

                        detectDragGestures(
                            onDragStart = { offset ->
                                updateFromOffset(offset)
                            },
                            onDragEnd = {
                                AppSettings.persistPrimaryColor()
                            },
                            onDragCancel = {
                                AppSettings.persistPrimaryColor()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                updateFromOffset(change.position)
                            },
                        )
                    },
        ) {
            drawRect(
                brush =
                    Brush.horizontalGradient(
                        colors = listOf(Color.White, hueColor),
                    ),
            )

            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                    ),
            )

            val center =
                Offset(
                    x = size.width * saturation,
                    y = size.height * (1f - value),
                )

            drawCircle(
                color = Color.White,
                radius = 11.dp.toPx(),
                center = center,
            )

            drawCircle(
                color = currentColor,
                radius = 8.dp.toPx(),
                center = center,
            )

            drawCircle(
                color = Color.Black.copy(alpha = 0.28f),
                radius = 11.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .pointerInput(Unit) {
                        fun updateHue(offset: Offset) {
                            val newHue =
                                (offset.x / size.width.toFloat())
                                    .coerceIn(0f, 1f) * 360f

                            updatePrimary(newHue = newHue)
                        }

                        detectTapGestures { offset ->
                            updateHue(offset)
                            AppSettings.persistPrimaryColor()
                        }
                    }.pointerInput(Unit) {
                        fun updateHue(offset: Offset) {
                            val newHue =
                                (offset.x / size.width.toFloat())
                                    .coerceIn(0f, 1f) * 360f

                            updatePrimary(newHue = newHue)
                        }

                        detectDragGestures(
                            onDragStart = { offset ->
                                updateHue(offset)
                            },
                            onDragEnd = {
                                AppSettings.persistPrimaryColor()
                            },
                            onDragCancel = {
                                AppSettings.persistPrimaryColor()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                updateHue(change.position)
                            },
                        )
                    },
        ) {
            drawRect(
                brush =
                    Brush.horizontalGradient(
                        colors =
                            listOf(
                                Color.Red,
                                Color.Yellow,
                                Color.Green,
                                Color.Cyan,
                                Color.Blue,
                                Color.Magenta,
                                Color.Red,
                            ),
                    ),
            )

            val center =
                Offset(
                    x = size.width * (hue / 360f),
                    y = size.height / 2f,
                )

            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = center,
            )

            drawCircle(
                color = hsvToColor(hue, 1f, 1f),
                radius = 7.dp.toPx(),
                center = center,
            )
        }

        Text(
            text = tr("Gyors színek", "Quick colors"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            themeColorPresets().forEach { preset ->
                val presetColor = Color(preset.toInt())

                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(presetColor)
                            .border(
                                width =
                                    if (AppSettings.primaryColorArgb.value == preset) {
                                        3.dp
                                    } else {
                                        1.dp
                                    },
                                color =
                                    if (AppSettings.primaryColorArgb.value == preset) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                    },
                                shape = CircleShape,
                            )
                            .clickable {
                                val presetHsv = colorToHsv(presetColor)
                                updatePrimary(
                                    newHue = presetHsv.hue,
                                    newSaturation = presetHsv.saturation,
                                    newValue = presetHsv.value,
                                    persist = true,
                                )
                            },
                )
            }
        }

        OutlinedButton(
            onClick = {
                AppSettings.resetPrimaryColor()
                val resetColor = Color(AppSettings.primaryColorArgb.value.toInt())
                val resetHsv = colorToHsv(resetColor)
                hue = resetHsv.hue
                saturation = resetHsv.saturation
                value = resetHsv.value
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(tr("Alapértelmezett Filc szín", "Default Filc color"))
        }
    }
}

@Composable
private fun LanguageCard(
    onHungarian: () -> Unit,
    onEnglish: () -> Unit,
) {
    SettingsSurface {
        LanguageOption(
            title = "Magyar",
            selected = AppSettings.language.value == AppLanguage.HU,
            onClick = onHungarian,
        )

        LanguageOption(
            title = "English",
            selected = AppSettings.language.value == AppLanguage.EN,
            onClick = onEnglish,
        )
    }
}

@Composable
private fun LanguageOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        border =
            if (selected) {
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                )
            } else {
                null
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )

            if (selected) {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBox(icon)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsIconBox(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SettingsSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
)

private fun hsvToColor(
    hue: Float,
    saturation: Float,
    value: Float,
): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)

    val chroma = v * s
    val x = chroma * (1f - abs((h / 60f % 2f) - 1f))
    val m = v - chroma

    val (r1, g1, b1) =
        when {
            h < 60f -> Triple(chroma, x, 0f)
            h < 120f -> Triple(x, chroma, 0f)
            h < 180f -> Triple(0f, chroma, x)
            h < 240f -> Triple(0f, x, chroma)
            h < 300f -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }

    return Color(
        red = r1 + m,
        green = g1 + m,
        blue = b1 + m,
        alpha = 1f,
    )
}

private fun colorToHsv(color: Color): HsvColor {
    val r = color.red
    val g = color.green
    val b = color.blue

    val maxValue = max(r, max(g, b))
    val minValue = min(r, min(g, b))
    val delta = maxValue - minValue

    val hue =
        when {
            delta == 0f -> 0f
            maxValue == r -> 60f * (((g - b) / delta) % 6f)
            maxValue == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let {
            if (it < 0f) it + 360f else it
        }

    val saturation =
        if (maxValue == 0f) {
            0f
        } else {
            delta / maxValue
        }

    return HsvColor(
        hue = hue,
        saturation = saturation,
        value = maxValue,
    )
}

private fun colorToArgbLong(color: Color): Long {
    val alpha = (color.alpha * 255f).roundToInt().coerceIn(0, 255)
    val red = (color.red * 255f).roundToInt().coerceIn(0, 255)
    val green = (color.green * 255f).roundToInt().coerceIn(0, 255)
    val blue = (color.blue * 255f).roundToInt().coerceIn(0, 255)

    return (alpha.toLong() shl 24) or
        (red.toLong() shl 16) or
        (green.toLong() shl 8) or
        blue.toLong()
}

private fun themeColorPresets(): List<Long> =
    listOf(
        0xFF009869L,
        0xFF15BA81L,
        0xFF3AD198L,
        0xFF4285F4L,
        0xFFFFB703L,
        0xFFEA4C89L,
    )

object SettingsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = tr("Beállítások", "Settings")
            val icon = rememberVectorPainter(Icons.Default.Settings)

            return remember(title) {
                TabOptions(
                    index = 4u,
                    title = title,
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
        SettingsScreen()
    }
}
