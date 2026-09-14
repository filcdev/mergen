package hu.petrik.filcapp.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hu.petrik.filcapp.network.CohortDto
import hu.petrik.filcapp.network.FilcPublicApi
import hu.petrik.filcapp.network.GroupDto
import hu.petrik.filcapp.network.TeacherDto

data class SchoolProfileDto(
    val cohort: CohortDto? = null,
    val groups: List<GroupDto> = emptyList(),
    val teacher: TeacherDto? = null,
)

/**
 * School-specific identity resolved from the CURRENT Chronos API.
 *
 * - cohort id comes from Better Auth's session user
 * - selected groups come from `/timetable/groups/getForCohort/:cohortId`
 * - teacher identity comes from `/timetable/teachers/me`
 */
object SchoolProfileState {
    var profile by mutableStateOf<SchoolProfileDto?>(null)
        private set

    var version by mutableIntStateOf(0)
        private set

    suspend fun refresh(user: AuthUser) {
        val teacher =
            runCatching {
                FilcPublicApi.getMyTeacher()
            }.getOrNull()

        val cohortId = user.cohortId
        val groups =
            if (cohortId == null) {
                emptyList()
            } else {
                runCatching {
                    FilcPublicApi
                        .getGroupsForCohort(cohortId)
                        .filter { it.selected }
                }.getOrDefault(emptyList())
            }

        val cohort =
            if (cohortId == null) {
                null
            } else {
                runCatching {
                    val latest = FilcPublicApi.getLatestValidTimetable()
                    FilcPublicApi
                        .getCohorts(latest.id)
                        .firstOrNull { it.id == cohortId }
                }.getOrNull() ?: CohortDto(
                    id = cohortId,
                    name = cohortId,
                )
            }

        profile =
            SchoolProfileDto(
                cohort = cohort,
                groups = groups,
                teacher = teacher,
            )
        version++
    }

    fun clear() {
        profile = null
        version++
    }
}

/**
 * Small compatibility facade for the timetable/settings branch.
 * Authentication itself is handled exclusively by [Auth].
 */
object AuthManager {
    suspend fun initialize() {
        Auth.bootstrap()
        AuthState.user?.let { SchoolProfileState.refresh(it) }
    }

    suspend fun signInWithMicrosoft() {
        Auth.signIn()
        AuthState.user?.let { SchoolProfileState.refresh(it) }
    }

    suspend fun refreshSession() {
        Auth.refreshSession()
        AuthState.user?.let { SchoolProfileState.refresh(it) }
    }

    suspend fun signOut() {
        Auth.signOut()
        SchoolProfileState.clear()
    }

    suspend fun updateUser(
        nickname: String? = AuthState.user?.nickname,
        cohortId: String? = AuthState.user?.cohortId,
    ) {
        Auth.updateUser(nickname = nickname, cohortId = cohortId)
        AuthState.user?.let { SchoolProfileState.refresh(it) }
    }

    suspend fun selectGroup(groupId: String) {
        FilcPublicApi.selectGroup(groupId)
        AuthState.user?.let { SchoolProfileState.refresh(it) }
    }

    suspend fun syncLanguage(language: String) {
        if (!AuthState.signedIn) {
            return
        }
        runCatching {
            FilcPublicApi.updateLanguage(language.lowercase())
        }
    }
}
