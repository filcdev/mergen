package hu.petrik.filcapp.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

import hu.petrik.filcapp.models.MovedLesson
import hu.petrik.filcapp.models.MovedLessonWithRelations

public class MovedLessonApi(private val client: HttpClient) {
    suspend fun getTimetableMovedLessons(): APIResult<List<MovedLessonWithRelations>> {
        return try {
            val response = client.get {
                url("/timetable/movedLessons")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<MovedLessonWithRelations>>>()
                APIResult.Success(envelope.data)
            } else {
                val errorBody = response.body<ApiErrorMessage>()
                APIResult.Failure(ApiError.BackendError(response.status.value, errorBody.message))
            }
        } catch (e: Exception) {
            APIResult.Failure(ApiError.Unknown(e))
        }
    }

    @RequiresAuth
    suspend fun postTimetableMovedLessons(body: MovedLesson): APIResult<MovedLesson> {
        return try {
            val response = client.post {
                url("/timetable/movedLessons")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<MovedLesson>>()
                APIResult.Success(envelope.data)
            } else {
                val errorBody = response.body<ApiErrorMessage>()
                APIResult.Failure(ApiError.BackendError(response.status.value, errorBody.message))
            }
        } catch (e: Exception) {
            APIResult.Failure(ApiError.Unknown(e))
        }
    }

    suspend fun getTimetableMovedLessonsCohortByCohortId(cohortId: String): APIResult<List<MovedLessonWithRelations>> {
        return try {
            val response = client.get {
                url("/timetable/movedLessons/cohort/${cohortId}")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<MovedLessonWithRelations>>>()
                APIResult.Success(envelope.data)
            } else {
                val errorBody = response.body<ApiErrorMessage>()
                APIResult.Failure(ApiError.BackendError(response.status.value, errorBody.message))
            }
        } catch (e: Exception) {
            APIResult.Failure(ApiError.Unknown(e))
        }
    }

    suspend fun getTimetableMovedLessonsCohortByCohortIdRelevant(cohortId: String): APIResult<List<MovedLessonWithRelations>> {
        return try {
            val response = client.get {
                url("/timetable/movedLessons/cohort/${cohortId}/relevant")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<MovedLessonWithRelations>>>()
                APIResult.Success(envelope.data)
            } else {
                val errorBody = response.body<ApiErrorMessage>()
                APIResult.Failure(ApiError.BackendError(response.status.value, errorBody.message))
            }
        } catch (e: Exception) {
            APIResult.Failure(ApiError.Unknown(e))
        }
    }

    suspend fun getTimetableMovedLessonsRelevant(timetableId: String): APIResult<List<MovedLessonWithRelations>> {
        return try {
            val response = client.get {
                url("/timetable/movedLessons/relevant")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<MovedLessonWithRelations>>>()
                APIResult.Success(envelope.data)
            } else {
                val errorBody = response.body<ApiErrorMessage>()
                APIResult.Failure(ApiError.BackendError(response.status.value, errorBody.message))
            }
        } catch (e: Exception) {
            APIResult.Failure(ApiError.Unknown(e))
        }
    }

    @RequiresAuth
    suspend fun putTimetableMovedLessonsById(id: String, body: MovedLesson): APIResult<MovedLesson> {
        return try {
            val response = client.put {
                url("/timetable/movedLessons/${id}")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<MovedLesson>>()
                APIResult.Success(envelope.data)
            } else {
                val errorBody = response.body<ApiErrorMessage>()
                APIResult.Failure(ApiError.BackendError(response.status.value, errorBody.message))
            }
        } catch (e: Exception) {
            APIResult.Failure(ApiError.Unknown(e))
        }
    }

    @RequiresAuth
    suspend fun deleteTimetableMovedLessonsById(id: String): APIResult<Boolean> {
        return try {
            val response = client.delete {
                url("/timetable/movedLessons/${id}")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<Boolean>>()
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
