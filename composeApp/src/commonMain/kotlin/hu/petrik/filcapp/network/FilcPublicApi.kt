package hu.petrik.filcapp.network

import hu.petrik.filcapp.auth.AuthState
import hu.petrik.filcapp.auth.SchoolProfileDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

private const val BASE_URL = "https://filc.petrik.hu/api"
private const val APP_ORIGIN = "mergen://"

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = true,
    val data: T,
)

@Serializable
data class TimetableDto(
    val id: String,
    val name: String = "",
    val validFrom: String? = null,
    val validTo: String? = null,
)

@Serializable
data class CohortDto(
    val id: String,
    val name: String,
    val short: String = "",
)

@Serializable
data class TeacherDto(
    val id: String,
    val firstName: String = "",
    val lastName: String = "",
    val short: String = "",
) {
    val displayName: String
        get() = "$firstName $lastName".trim().ifBlank { short }
}

@Serializable
data class NamedRefDto(
    val id: String,
    val name: String,
    val short: String = "",
)

@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val cohortId: String? = null,
    val divisionTag: String? = null,
    val divisionLabel: String? = null,
    val entireClass: Boolean = false,
    val selected: Boolean = false,
)

@Serializable
data class DayDefinitionDto(
    val id: String = "",
    val name: String = "",
    val short: String = "",
)

@Serializable
data class PeriodDto(
    val id: String = "",
    val period: Int = 0,
    val startTime: String = "",
    val endTime: String = "",
)

@Serializable
data class SubjectDto(
    val id: String = "",
    val name: String = "",
    val short: String = "",
)

@Serializable
data class WeekDefinitionDto(
    val id: String = "",
    val name: String = "",
    val short: String = "",
    val weeks: List<String> = emptyList(),
)

@Serializable
data class LessonDto(
    val id: String,
    val classrooms: List<NamedRefDto> = emptyList(),
    val cohorts: List<CohortDto> = emptyList(),
    val day: DayDefinitionDto? = null,
    val groups: List<GroupDto> = emptyList(),
    val period: PeriodDto? = null,
    val subject: SubjectDto? = null,
    val teachers: List<NamedRefDto> = emptyList(),
    val weekDefinition: WeekDefinitionDto? = null,
)

@Serializable
data class SubstitutionDto(
    val id: String,
    val date: String,
    val comment: String? = null,
    val substituter: String? = null,
)

@Serializable
data class SubstitutionLessonDto(
    val id: String,
    val classrooms: List<NamedRefDto> = emptyList(),
    val cohorts: List<String> = emptyList(),
    val day: DayDefinitionDto? = null,
    val period: PeriodDto? = null,
    val subject: SubjectDto? = null,
    val teachers: List<NamedRefDto> = emptyList(),
)

@Serializable
data class SubstitutionItemDto(
    val lessons: List<SubstitutionLessonDto> = emptyList(),
    val substitution: SubstitutionDto,
    val teacher: TeacherDto? = null,
)

@Serializable
data class MovedLessonDto(
    val id: String,
    val date: String,
)

@Serializable
data class MovedLessonItemDto(
    val classroom: NamedRefDto? = null,
    val dayDefinition: DayDefinitionDto? = null,
    val lessonNames: List<String> = emptyList(),
    val lessons: List<String> = emptyList(),
    val movedLesson: MovedLessonDto,
    val period: PeriodDto? = null,
)

@Serializable
data class AnnouncementDto(
    val id: String,
    val title: String? = null,
    val content: JsonElement = JsonNull,
    val validFrom: String,
    val validUntil: String,
)

@Serializable
private data class SelectGroupRequest(val groupId: String)

@Serializable
private data class LanguageUpdateRequest(val language: String)

enum class TimetableFilter {
    COHORT,
    TEACHER,
    CLASSROOM,
}

object FilcPublicApi {
    private val client =
        HttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }

    suspend fun getTimetables(): List<TimetableDto> =
        client.get("$BASE_URL/timetable/timetables") { addAuthHeaders() }
            .body<ApiEnvelope<List<TimetableDto>>>()
            .data

    suspend fun getLatestValidTimetable(): TimetableDto =
        client.get("$BASE_URL/timetable/timetables/latestValid") { addAuthHeaders() }
            .body<ApiEnvelope<TimetableDto>>()
            .data

    suspend fun getCohorts(timetableId: String): List<CohortDto> =
        client.get("$BASE_URL/timetable/cohorts/getAllForTimetable/$timetableId") { addAuthHeaders() }
            .body<ApiEnvelope<List<CohortDto>>>()
            .data

    suspend fun getTeachers(): List<TeacherDto> =
        client.get("$BASE_URL/timetable/teachers/getAll") { addAuthHeaders() }
            .body<ApiEnvelope<List<TeacherDto>>>()
            .data

    suspend fun getClassrooms(): List<NamedRefDto> =
        client.get("$BASE_URL/timetable/classrooms/getAll") { addAuthHeaders() }
            .body<ApiEnvelope<List<NamedRefDto>>>()
            .data

    suspend fun getLessons(
        filter: TimetableFilter,
        selectionId: String,
        timetableId: String,
    ): List<LessonDto> {
        val path =
            when (filter) {
                TimetableFilter.COHORT -> "getForCohort/$selectionId"
                TimetableFilter.TEACHER -> "getForTeacher/$selectionId"
                TimetableFilter.CLASSROOM -> "getForRoom/$selectionId"
            }

        return client
            .get("$BASE_URL/timetable/lessons/$path") {
                addAuthHeaders()
                parameter("timetableId", timetableId)
            }.body<ApiEnvelope<List<LessonDto>>>()
            .data
    }

    suspend fun getSubstitutions(): List<SubstitutionItemDto> =
        client.get("$BASE_URL/timetable/substitutions") { addAuthHeaders() }
            .body<ApiEnvelope<List<SubstitutionItemDto>>>()
            .data

    suspend fun getMovedLessons(): List<MovedLessonItemDto> =
        client.get("$BASE_URL/timetable/movedLessons") { addAuthHeaders() }
            .body<ApiEnvelope<List<MovedLessonItemDto>>>()
            .data

    suspend fun getAnnouncements(): List<AnnouncementDto> =
        client.get("$BASE_URL/news/announcements") { addAuthHeaders() }
            .body<ApiEnvelope<List<AnnouncementDto>>>()
            .data

    suspend fun getMyProfile(): SchoolProfileDto =
        client.get("$BASE_URL/users/me/profile") { addAuthHeaders() }
            .body<ApiEnvelope<SchoolProfileDto>>()
            .data

    suspend fun getGroupsForCohort(cohortId: String): List<GroupDto> =
        client.get("$BASE_URL/timetable/groups/getForCohort/$cohortId") { addAuthHeaders() }
            .body<ApiEnvelope<List<GroupDto>>>()
            .data

    suspend fun selectGroup(groupId: String) {
        client.post("$BASE_URL/timetable/groups/select") {
            addAuthHeaders()
            contentType(ContentType.Application.Json)
            setBody(SelectGroupRequest(groupId))
        }.body<ApiEnvelope<JsonElement>>()
    }

    suspend fun updateLanguage(language: String) {
        client.patch("$BASE_URL/notifications/settings") {
            addAuthHeaders()
            contentType(ContentType.Application.Json)
            setBody(LanguageUpdateRequest(language))
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.addAuthHeaders() {
        AuthState.cookie?.let { header(HttpHeaders.Cookie, it) }
        header("mergen-origin", APP_ORIGIN)
        header("x-skip-oauth-proxy", "true")
    }
}
