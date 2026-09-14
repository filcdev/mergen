package hu.petrik.filcapp.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://filc.petrik.hu/api"

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
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
data class LessonDto(
    val id: String,
    val classrooms: List<NamedRefDto> = emptyList(),
    val cohorts: List<CohortDto> = emptyList(),
    val day: DayDefinitionDto? = null,
    val period: PeriodDto? = null,
    val subject: SubjectDto? = null,
    val teachers: List<NamedRefDto> = emptyList(),
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

enum class TimetableFilter {
    COHORT,
    TEACHER,
    CLASSROOM,
}

object ChronosPublicApi {
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

    suspend fun getLatestValidTimetable(): TimetableDto =
        client
            .get("$BASE_URL/timetable/timetables/latestValid")
            .body<ApiEnvelope<TimetableDto>>()
            .data

    suspend fun getCohorts(timetableId: String): List<CohortDto> =
        client
            .get("$BASE_URL/timetable/cohorts/getAllForTimetable/$timetableId")
            .body<ApiEnvelope<List<CohortDto>>>()
            .data

    suspend fun getTeachers(): List<TeacherDto> =
        client
            .get("$BASE_URL/timetable/teachers/getAll")
            .body<ApiEnvelope<List<TeacherDto>>>()
            .data

    suspend fun getClassrooms(): List<NamedRefDto> =
        client
            .get("$BASE_URL/timetable/classrooms/getAll")
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
                parameter("timetableId", timetableId)
            }.body<ApiEnvelope<List<LessonDto>>>()
            .data
    }

    suspend fun getSubstitutions(): List<SubstitutionItemDto> =
        client
            .get("$BASE_URL/timetable/substitutions")
            .body<ApiEnvelope<List<SubstitutionItemDto>>>()
            .data

    suspend fun getMovedLessons(): List<MovedLessonItemDto> =
        client
            .get("$BASE_URL/timetable/movedLessons")
            .body<ApiEnvelope<List<MovedLessonItemDto>>>()
            .data
}
