package hu.petrik.filcapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
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
import hu.petrik.filcapp.settings.tr
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val scope = rememberCoroutineScope()
    var cohorts by remember { mutableStateOf<List<CohortDto>>(emptyList()) }
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var nickname by remember(AuthState.user?.id, AuthState.user?.nickname) {
        mutableStateOf(AuthState.user?.nickname.orEmpty())
    }
    var profileLoading by remember { mutableStateOf(false) }

    LaunchedEffect(AuthState.user?.id) {
        if (AuthState.user != null) {
            profileLoading = true
            runCatching {
                val latest = FilcPublicApi.getLatestValidTimetable()
                cohorts = FilcPublicApi.getCohorts(latest.id).sortedBy { it.name }
            }
            profileLoading = false
        } else {
            cohorts = emptyList()
            groups = emptyList()
        }
    }

    LaunchedEffect(AuthState.profile, AuthState.callbackVersion) {
        val cohortId = AuthState.profile?.cohort?.id
        groups =
            if (cohortId == null) {
                emptyList()
            } else {
                runCatching { FilcPublicApi.getGroupsForCohort(cohortId) }.getOrDefault(emptyList())
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tr("Beállítások", "Settings"), style = MaterialTheme.typography.headlineSmall)
            Text(
                tr(
                    "Fiók, saját osztály/csoport és alkalmazásbeállítások.",
                    "Account, class/group and application settings.",
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AccountSettingsCard(
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
            onGroupSelected = { groupId -> scope.launch { AuthManager.selectGroup(groupId) } },
        )

        AuthState.error?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Language, null)
                    Column {
                        Text(tr("Nyelv", "Language"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            tr("A módosítás azonnal életbe lép.", "Changes apply immediately."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                LanguageOption(
                    title = "Magyar",
                    selected = AppSettings.language.value == AppLanguage.HU,
                    onClick = {
                        AppSettings.setLanguage(AppLanguage.HU)
                        scope.launch { AuthManager.syncLanguage("hu") }
                    },
                )
                LanguageOption(
                    title = "English",
                    selected = AppSettings.language.value == AppLanguage.EN,
                    onClick = {
                        AppSettings.setLanguage(AppLanguage.EN)
                        scope.launch { AuthManager.syncLanguage("en") }
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountSettingsCard(
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.AccountCircle, null)
                Text(tr("Microsoft-fiók", "Microsoft account"), style = MaterialTheme.typography.titleMedium)
            }

            if (!AuthState.signedIn) {
                Text(
                    tr(
                        "Jelentkezz be a petrikes Microsoft-fiókoddal. Az OAuth-ot a Filc backend kezeli.",
                        "Sign in with your Petrik Microsoft account. OAuth is handled by the Filc backend.",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onSignIn, enabled = !AuthState.loading) {
                    Icon(Icons.Default.Login, null)
                    Text(
                        tr(" Bejelentkezés Microsofttal", " Sign in with Microsoft"),
                    )
                }
                if (AuthState.loading) CircularProgressIndicator()
                return@Column
            }

            val user = AuthState.user ?: return@Column
            Text(user.preferredName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AuthState.profile?.teacher?.let { teacher ->
                Text(
                    tr("Tanári profil: ${teacher.displayName}", "Teacher profile: ${teacher.displayName}"),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(tr("Megjelenő név", "Preferred name")) },
                singleLine = true,
            )
            OutlinedButton(onClick = onSaveNickname) {
                Text(tr("Név mentése", "Save name"))
            }

            if (loading) {
                CircularProgressIndicator()
            } else {
                SearchableSelection(
                    options = cohorts.map { it.id to it.name },
                    selectedId = AuthState.profile?.cohort?.id,
                    label = tr("Saját osztály", "My class"),
                    placeholder = tr("Válassz osztályt", "Select class"),
                    onSelected = { it?.let(onCohortSelected) },
                )
            }

            val selectableGroups = groups.filter { !it.entireClass && it.divisionTag != null }
            if (selectableGroups.isNotEmpty()) {
                Text(tr("Saját csoportok", "My groups"), style = MaterialTheme.typography.titleSmall)
                selectableGroups
                    .groupBy { it.divisionLabel ?: it.divisionTag ?: tr("Csoport", "Group") }
                    .forEach { (division, divisionGroups) ->
                        Text(division, style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            OutlinedButton(onClick = onSignOut) {
                Icon(Icons.Default.Logout, null)
                Text(tr(" Kijelentkezés", " Sign out"))
            }
        }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

object SettingsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = tr("Beállítások", "Settings")
            val icon = rememberVectorPainter(Icons.Default.Settings)
            return remember(title) {
                TabOptions(index = 4u, title = title, icon = icon)
            }
        }

    @Composable
    override fun Content() {
        SettingsScreen()
    }
}
