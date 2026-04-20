package hu.petrik.filcapp.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

import hu.petrik.filcapp.models.EnrichedLesson

public class LessonApi(private val client: HttpClient) {
    suspend fun getTimetableLessonsForCohortByCohortId(cohortId: String): APIResult<List<EnrichedLesson>> {
        return try {
            val response = client.get {
                url("/timetable/lessons/getForCohort/${cohortId}")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<EnrichedLesson>>>()
                APIResult.Success(envelope.data)
            } else {
                val errorBody = response.body<ApiErrorMessage>()
                APIResult.Failure(ApiError.BackendError(response.status.value, errorBody.message))
            }
        } catch (e: Exception) {
            APIResult.Failure(ApiError.Unknown(e))
        }
    }

    suspend fun getTimetableLessonsForIdByLessonId(lessonId: String): APIResult<EnrichedLesson> {
        return try {
            val response = client.get {
                url("/timetable/lessons/getForId/${lessonId}")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<EnrichedLesson>>()
                APIResult.Success(envelope.data)
            } else {
                val errorBody = response.body<ApiErrorMessage>()
                APIResult.Failure(ApiError.BackendError(response.status.value, errorBody.message))
            }
        } catch (e: Exception) {
            APIResult.Failure(ApiError.Unknown(e))
        }
    }

    suspend fun getTimetableLessonsForRoomByClassroomId(classroomId: String): APIResult<List<EnrichedLesson>> {
        return try {
            val response = client.get {
                url("/timetable/lessons/getForRoom/${classroomId}")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<EnrichedLesson>>>()
                APIResult.Success(envelope.data)
            } else {
                val errorBody = response.body<ApiErrorMessage>()
                APIResult.Failure(ApiError.BackendError(response.status.value, errorBody.message))
            }
        } catch (e: Exception) {
            APIResult.Failure(ApiError.Unknown(e))
        }
    }

    suspend fun getTimetableLessonsForTeacherByTeacherId(teacherId: String): APIResult<List<EnrichedLesson>> {
        return try {
            val response = client.get {
                url("/timetable/lessons/getForTeacher/${teacherId}")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<EnrichedLesson>>>()
                APIResult.Success(envelope.data)
            } else {
                val errorBody = response.body<ApiErrorMessage>()
                APIResult.Failure(ApiError.BackendError(response.status.value, errorBody.message))
            }
        } catch (e: Exception) {
            APIResult.Failure(ApiError.Unknown(e))
        }
    }

}
