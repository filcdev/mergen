package hu.petrik.filcapp.data

import hu.petrik.filcapp.api.client.ClassroomApi
import hu.petrik.filcapp.api.client.CohortApi
import hu.petrik.filcapp.api.client.GroupApi
import hu.petrik.filcapp.api.client.LessonApi
import hu.petrik.filcapp.api.client.MovedLessonApi
import hu.petrik.filcapp.api.client.NewsAnnouncementsApi
import hu.petrik.filcapp.api.client.NotificationsApi
import hu.petrik.filcapp.api.client.SubstitutionApi
import hu.petrik.filcapp.api.client.TeacherApi
import hu.petrik.filcapp.api.client.TimetableApi
import hu.petrik.filcapp.api.infrastructure.ApiClient
import hu.petrik.filcapp.api.infrastructure.HttpResponse
import hu.petrik.filcapp.api.model.GetCohort200ResponseItemsInner
import hu.petrik.filcapp.api.model.GetNewsAnnouncements200ResponseItemsInner
import hu.petrik.filcapp.api.model.GetTimetableLessonsGetForCohortByCohortId200ResponseItemsInnerClassroomsInner
import hu.petrik.filcapp.api.model.GetTimetableLessonsGetForCohortByCohortId200ResponseItemsInnerCohortsInner
import hu.petrik.filcapp.api.model.GetTimetableMovedLessons200ResponseItemsInner
import hu.petrik.filcapp.api.model.GetTimetableSubstitutions200ResponseItemsInner
import hu.petrik.filcapp.api.model.GetTimetableTeachersGetAll200ResponseItemsInner
import hu.petrik.filcapp.api.model.GetTimetableTimetables200ResponseItemsInner
import hu.petrik.filcapp.auth.Auth
import kotlinx.serialization.Serializable

private class ApiException(
    val status: Int,
    message: String,
) : RuntimeException("$status $message")

private suspend fun <T : Any> HttpResponse<T>.unwrap(): T {
    if (!success) throw ApiException(status, "request failed")
    return body()
}

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
    val groups: List<NamedRefDto> = emptyList(),
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
    val cohorts: List<CohortDto> = emptyList(),
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
    val lessons: List<LessonDto> = emptyList(),
    val movedLesson: MovedLessonDto,
    val period: PeriodDto? = null,
)

@Serializable
data class AnnouncementDto(
    val id: String,
    val title: String? = null,
    val contentText: String = "",
    val validFrom: String,
    val validUntil: String,
    val authorName: String? = null,
    val highlighted: Boolean = false,
)

enum class TimetableFilter {
    COHORT,
    TEACHER,
    CLASSROOM,
}

// --- mappers: generated models -> screen DTOs ---

private fun refNamed(ref: GetTimetableLessonsGetForCohortByCohortId200ResponseItemsInnerCohortsInner): NamedRefDto =
    NamedRefDto(
        id = ref.id.orEmpty(),
        name = ref.name.orEmpty(),
        short = ref.short.orEmpty(),
    )

private fun refCohort(ref: GetTimetableLessonsGetForCohortByCohortId200ResponseItemsInnerCohortsInner): CohortDto =
    CohortDto(
        id = ref.id.orEmpty(),
        name = ref.name.orEmpty(),
        short = ref.short.orEmpty(),
    )

private fun refNamed(ref: GetTimetableLessonsGetForCohortByCohortId200ResponseItemsInnerClassroomsInner): NamedRefDto =
    NamedRefDto(
        id = ref.id.orEmpty(),
        name = ref.name.orEmpty(),
        short = ref.short.orEmpty(),
    )

private fun subject(ref: hu.petrik.filcapp.api.model.GetTimetableLessonsGetForCohortByCohortId200ResponseItemsInnerSubject?): SubjectDto? =
    ref?.let {
        SubjectDto(
            id = it.id.orEmpty(),
            name = it.name.orEmpty(),
            short = it.short.orEmpty(),
        )
    }

private fun period(ref: hu.petrik.filcapp.api.model.GetTimetableLessonsGetForCohortByCohortId200ResponseItemsInnerPeriod?): PeriodDto? =
    ref?.let {
        PeriodDto(
            id = it.id,
            period = it.period.toInt(),
            startTime = it.startTime,
            endTime = it.endTime,
        )
    }

private fun weekDefinition(
    ref: hu.petrik.filcapp.api.model.GetTimetableLessonsGetForCohortByCohortId200ResponseItemsInnerWeekDefinition?,
): WeekDefinitionDto? =
    ref?.let {
        WeekDefinitionDto(
            id = it.id.orEmpty(),
            name = it.name.orEmpty(),
            short = it.short.orEmpty(),
            weeks = it.weeks,
        )
    }

private fun weekDefinition(
    ref: hu.petrik.filcapp.api.model.GetTimetableLessonsGetForIdByLessonId200ResponseWeekDefinition?,
): WeekDefinitionDto? =
    ref?.let {
        WeekDefinitionDto(
            id = it.id,
            name = it.name,
            short = it.short,
            weeks = it.weeks,
        )
    }

private fun timetable(ref: GetTimetableTimetables200ResponseItemsInner): TimetableDto =
    TimetableDto(
        id = ref.id,
        name = ref.name,
        validFrom = ref.validFrom,
        validTo = ref.validTo,
    )

private fun cohort(ref: GetCohort200ResponseItemsInner): CohortDto =
    CohortDto(
        id = ref.id.orEmpty(),
        name = ref.name.orEmpty(),
        short = ref.short.orEmpty(),
    )

private fun teacher(ref: GetTimetableTeachersGetAll200ResponseItemsInner): TeacherDto =
    TeacherDto(
        id = ref.id.orEmpty(),
        firstName = ref.firstName.orEmpty(),
        lastName = ref.lastName.orEmpty(),
        short = ref.short.orEmpty(),
    )

private fun group(ref: hu.petrik.filcapp.api.model.GetTimetableGroupsGetForCohortByCohortId200ResponseItemsInner): GroupDto =
    GroupDto(
        id = ref.id.orEmpty(),
        name = ref.name.orEmpty(),
        divisionTag = ref.divisionTag,
        divisionLabel = ref.divisionLabel,
        entireClass = ref.entireClass,
        selected = ref.selected,
    )

private fun substitutionItem(ref: GetTimetableSubstitutions200ResponseItemsInner): SubstitutionItemDto =
    SubstitutionItemDto(
        lessons =
            ref.lessons.map { lesson ->
                SubstitutionLessonDto(
                    id = lesson.id,
                    classrooms = lesson.classrooms.map(::refNamed),
                    cohorts = lesson.cohorts.map(::refCohort),
                    period = period(lesson.period),
                    subject = subject(lesson.subject),
                    teachers = lesson.teachers.map(::refNamed),
                )
            },
        substitution =
            hu.petrik.filcapp.data.SubstitutionDto(
                id = ref.substitution.id.orEmpty(),
                date = ref.substitution.date.orEmpty(),
                comment = ref.substitution.comment,
                substituter = ref.substitution.substituter,
            ),
        teacher =
            ref.teacher?.let {
                TeacherDto(
                    id = it.id.orEmpty(),
                    firstName = it.firstName.orEmpty(),
                    lastName = it.lastName.orEmpty(),
                    short = it.short.orEmpty(),
                )
            },
    )

private fun movedLessonItem(ref: GetTimetableMovedLessons200ResponseItemsInner): MovedLessonItemDto =
    MovedLessonItemDto(
        classroom = ref.classroom?.let { NamedRefDto(id = it.id, name = it.name, short = it.short) },
        dayDefinition = null,
        lessons =
            ref.lessons.map { lesson ->
                LessonDto(
                    id = lesson.id,
                    classrooms = lesson.classrooms.map(::refNamed),
                    cohorts = lesson.cohorts.map(::refCohort),
                    period = period(lesson.period),
                    subject = subject(lesson.subject),
                    teachers = lesson.teachers.map(::refNamed),
                )
            },
        movedLesson =
            hu.petrik.filcapp.data.MovedLessonDto(
                id = ref.movedLesson.id.orEmpty(),
                date = ref.movedLesson.date.orEmpty(),
            ),
        period =
            ref.period?.let {
                PeriodDto(
                    id = it.id.orEmpty(),
                    period = it.period ?: 0,
                    startTime = it.startTime.orEmpty(),
                    endTime = it.endTime.orEmpty(),
                )
            },
    )

private fun announcement(ref: GetNewsAnnouncements200ResponseItemsInner): AnnouncementDto {
    // `content` is a recursive anyOf (string/number/bool/array/object) the generator models as an
    // opaque serializable object; pull the text out differently per runtime using its toString.
    val contentText =
        ref.content
            ?.let { node ->
                when {
                    node.toString().startsWith("\"") -> node.toString().trim('"')
                    else -> node.toString()
                }
            }.orEmpty()
    return AnnouncementDto(
        id = ref.id.orEmpty(),
        title = ref.title,
        contentText = contentText,
        validFrom = ref.validFrom.orEmpty(),
        validUntil = ref.validUntil.orEmpty(),
        authorName = ref.author?.name,
        highlighted = ref.highlighted,
    )
}

/** Chronos API facade: the generated oRPC-OpenAPI client, mapped onto the screen DTOs. */
object ChronosApi {
    private val timetable = TimetableApi(ApiClient.BASE_URL, Auth.httpClient)
    private val cohort = CohortApi(ApiClient.BASE_URL, Auth.httpClient)
    private val teacher = TeacherApi(ApiClient.BASE_URL, Auth.httpClient)
    private val classroom = ClassroomApi(ApiClient.BASE_URL, Auth.httpClient)
    private val lessonApi = LessonApi(ApiClient.BASE_URL, Auth.httpClient)
    private val substitution = SubstitutionApi(ApiClient.BASE_URL, Auth.httpClient)
    private val movedLesson = MovedLessonApi(ApiClient.BASE_URL, Auth.httpClient)
    private val news = NewsAnnouncementsApi(ApiClient.BASE_URL, Auth.httpClient)
    private val group = GroupApi(ApiClient.BASE_URL, Auth.httpClient)
    private val notifications = NotificationsApi(ApiClient.BASE_URL, Auth.httpClient)

    suspend fun getLatestValidTimetable(): TimetableDto =
        timetable.getTimetableTimetablesLatestValid().unwrap().let {
            TimetableDto(id = it.id.orEmpty(), name = it.name.orEmpty(), validFrom = it.validFrom, validTo = it.validTo)
        }

    suspend fun getTimetables(): List<TimetableDto> =
        timetable
            .getTimetableTimetables()
            .unwrap()
            .items
            .orEmpty()
            .map(::timetable)

    suspend fun getCohorts(timetableId: String): List<CohortDto> =
        cohort
            .getTimetableCohortsGetAllForTimetableByTimetableId(timetableId)
            .unwrap()
            .items
            .orEmpty()
            .map(::cohort)

    suspend fun getTeachers(): List<TeacherDto> =
        teacher
            .getTimetableTeachersGetAll()
            .unwrap()
            .items
            .orEmpty()
            .map(::teacher)

    suspend fun getMyTeacher(): TeacherDto? =
        teacher.getTimetableTeachersMe().unwrap().let {
            TeacherDto(id = it.id, firstName = it.firstName, lastName = it.lastName, short = it.short)
        }

    suspend fun getClassrooms(): List<NamedRefDto> =
        classroom.getTimetableClassroomsGetAll().unwrap().items.orEmpty().map {
            NamedRefDto(id = it.id.orEmpty(), name = it.name.orEmpty(), short = it.short.orEmpty())
        }

    suspend fun getLessons(
        filter: TimetableFilter,
        selectionId: String,
        timetableId: String,
    ): List<LessonDto> {
        val response =
            when (filter) {
                TimetableFilter.COHORT -> {
                    lessonApi.getTimetableLessonsGetForCohortByCohortId(cohortId = selectionId, timetableId = timetableId)
                }

                TimetableFilter.TEACHER -> {
                    lessonApi.getTimetableLessonsGetForTeacherByTeacherId(teacherId = selectionId, timetableId = timetableId)
                }

                TimetableFilter.CLASSROOM -> {
                    lessonApi.getTimetableLessonsGetForRoomByClassroomId(classroomId = selectionId, timetableId = timetableId)
                }
            }

        return response.unwrap().items.orEmpty().map { lesson ->
            LessonDto(
                id = lesson.id.orEmpty(),
                classrooms = lesson.classrooms.map(::refNamed),
                cohorts = lesson.cohorts.map(::refCohort),
                day =
                    lesson.day?.let { day ->
                        DayDefinitionDto(id = day.id.orEmpty(), name = day.name.orEmpty(), short = day.short.orEmpty())
                    },
                groups =
                    lesson.groups.map { group ->
                        NamedRefDto(id = group.id.orEmpty(), name = group.name.orEmpty(), short = "")
                    },
                period = period(lesson.period),
                subject = subject(lesson.subject),
                teachers = lesson.teachers.map(::refNamed),
                weekDefinition = weekDefinition(lesson.weekDefinition),
            )
        }
    }

    suspend fun getSubstitutions(): List<SubstitutionItemDto> =
        substitution
            .getTimetableSubstitutions()
            .unwrap()
            .items
            .orEmpty()
            .map(::substitutionItem)

    suspend fun getMovedLessons(): List<MovedLessonItemDto> =
        movedLesson
            .getTimetableMovedLessons()
            .unwrap()
            .items
            .orEmpty()
            .map(::movedLessonItem)

    suspend fun getAnnouncements(): List<AnnouncementDto> =
        news
            .getNewsAnnouncements(limit = 50, includeKioskOnly = "false")
            .unwrap()
            .items
            .orEmpty()
            .map(::announcement)

    suspend fun getGroupsForCohort(cohortId: String): List<GroupDto> =
        group
            .getTimetableGroupsGetForCohortByCohortId(cohortId)
            .unwrap()
            .items
            .orEmpty()
            .map(::group)

    suspend fun selectGroup(groupId: String) {
        group
            .postTimetableGroupsSelect(
                hu.petrik.filcapp.api.model
                    .PostTimetableGroupsSelectRequest(groupId = groupId),
            ).unwrap()
    }

    suspend fun updateLanguage(language: String) {
        notifications
            .patchNotificationsSettings(
                hu.petrik.filcapp.api.model
                    .PatchNotificationsSettingsRequest(language = language),
            ).unwrap()
    }
}
