package hu.petrik.filcapp.admin

import hu.petrik.filcapp.api.client.CohortApi
import hu.petrik.filcapp.api.client.PeriodApi
import hu.petrik.filcapp.api.client.SubjectApi
import hu.petrik.filcapp.api.infrastructure.ApiClient
import hu.petrik.filcapp.auth.Auth
import hu.petrik.filcapp.data.ChronosApi
import hu.petrik.filcapp.data.CohortDto
import hu.petrik.filcapp.data.LessonDto
import hu.petrik.filcapp.data.PeriodDto
import hu.petrik.filcapp.data.SubjectDto
import hu.petrik.filcapp.data.SubstitutionItemDto
import hu.petrik.filcapp.data.TeacherDto
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Deliberately uses Auth.httpClient: the same Better Auth cookie jar as the rest of Mergen. */
object SubstitutionAdminRepository {
    private val client get() = Auth.httpClient
    private val baseUrl get() = ApiClient.BASE_URL.trimEnd('/')
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Serializable
    data class CandidateTeacher(
        val hasH1: Boolean = false,
        val hasH2: Boolean = false,
        val teacher: TeacherDto,
    )

    @Serializable
    data class Candidates(
        val availableLessons: List<LessonDto> = emptyList(),
        val parallelLessons: List<LessonDto> = emptyList(),
        val substituteCandidates: List<CandidateTeacher> = emptyList(),
    )

    @Serializable
    private data class CandidateRequest(
        val date: String,
        val missingTeacherId: String,
        val selectedLessonIds: List<String>,
        val teacherIds: List<String>,
    )

    @Serializable
    private data class NormalRequest(
        val date: String,
        val lessonIds: List<String>,
        val substituter: String?,
        val comment: String?,
    )

    @Serializable
    private data class ManualRequest(
        val date: String,
        val teacherId: String,
        val periodId: String,
        val subjectId: String?,
        val cohortId: String,
        val substituter: String?,
        val comment: String?,
    )

    suspend fun list(): List<SubstitutionItemDto> = ChronosApi.getSubstitutions()

    suspend fun teachers(): List<TeacherDto> = ChronosApi.getTeachers().sortedBy { it.displayName }

    suspend fun cohorts(): List<CohortDto> {
        val response = CohortApi(baseUrl, client).getCohort()
        requireSuccess(response.status)
        return response.body().items.mapNotNull { item ->
            item.id?.let { CohortDto(it, item.name.orEmpty(), item.short.orEmpty()) }
        }.sortedBy { it.name }
    }

    suspend fun periods(): List<PeriodDto> {
        val response = PeriodApi(baseUrl, client).getTimetablePeriodsGetAll()
        requireSuccess(response.status)
        return response.body().items.mapNotNull { item ->
            item.id?.let {
                PeriodDto(it, item.period?.toInt() ?: 0, item.startTime.orEmpty(), item.endTime.orEmpty())
            }
        }.sortedBy { it.period }
    }

    suspend fun subjects(): List<SubjectDto> {
        val response = SubjectApi(baseUrl, client).getTimetableSubjects()
        requireSuccess(response.status)
        return response.body().items.mapNotNull { item ->
            item.id?.let { SubjectDto(it, item.name.orEmpty(), item.short.orEmpty()) }
        }.sortedBy { it.name }
    }

    suspend fun candidates(
        date: String,
        missingTeacherId: String,
        selectedLessonIds: List<String>,
        teacherIds: List<String>,
    ): Candidates {
        val payload = CandidateRequest(substitutionIsoDate(date), missingTeacherId, selectedLessonIds, teacherIds)
        val response =
            client.post("$baseUrl/timetable/lessons/getSubstitutionCandidates") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(payload))
            }
        return json.decodeFromString<Candidates>(response.checkedBody())
    }

    suspend fun create(
        date: String,
        lessonIds: List<String>,
        substituter: String?,
        comment: String?,
    ) {
        require(lessonIds.isNotEmpty()) { "Choose at least one lesson" }
        val response =
            client.post("$baseUrl/timetable/substitutions") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(NormalRequest(substitutionIsoDate(date), lessonIds.distinct(), substituter, comment)))
            }
        response.checkedBody()
    }

    suspend fun update(
        id: String,
        date: String,
        lessonIds: List<String>,
        substituter: String?,
        comment: String?,
    ) {
        require(lessonIds.isNotEmpty()) { "Choose at least one lesson" }
        val response =
            client.put("$baseUrl/timetable/substitutions/$id") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(NormalRequest(substitutionIsoDate(date), lessonIds.distinct(), substituter, comment)))
            }
        response.checkedBody()
    }

    suspend fun createManual(
        date: String,
        teacherId: String,
        periodId: String,
        subjectId: String?,
        cohortId: String,
        substituter: String?,
        comment: String?,
    ) {
        require(teacherId.isNotBlank() && periodId.isNotBlank() && cohortId.isNotBlank())
        val response =
            client.post("$baseUrl/timetable/substitutions/manual") {
                contentType(ContentType.Application.Json)
                setBody(
                    json.encodeToString(
                        ManualRequest(substitutionIsoDate(date), teacherId, periodId, subjectId, cohortId, substituter, comment),
                    ),
                )
            }
        response.checkedBody()
    }

    suspend fun delete(id: String) {
        client.delete("$baseUrl/timetable/substitutions/$id").checkedBody()
    }

    private fun requireSuccess(status: Int) {
        check(status in 200..299) { "Chronos HTTP $status" }
    }

    private suspend fun HttpResponse.checkedBody(): String {
        val body = bodyAsText()
        if (!status.isSuccess()) {
            val message =
                when (status.value) {
                    401 -> "A munkamenet lejárt. Jelentkezz be újra."
                    403 -> "Nincs jogosultságod ehhez a művelethez."
                    409 -> "Ütközés: a helyettesítő tanár már foglalt ebben az időpontban."
                    else -> "Chronos HTTP ${status.value}: ${body.take(250)}"
                }
            error(message)
        }
        return body
    }
}
