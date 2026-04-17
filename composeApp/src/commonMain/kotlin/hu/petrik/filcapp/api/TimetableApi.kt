package hu.petrik.filcapp.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

import hu.petrik.filcapp.models.Timetable

public class TimetableApi(private val client: HttpClient) {
    @RequiresAuth
    suspend fun postTimetableImport(body: Boolean): APIResult<Boolean> {
        return try {
            val response = client.post {
                url("/timetable/import")
                setBody(body)
                contentType(ContentType.Application.Json)
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

    @RequiresAuth
    suspend fun getTimetableTimetables(): APIResult<List<Timetable>> {
        return try {
            val response = client.get {
                url("/timetable/timetables")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<Timetable>>>()
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
    suspend fun getTimetableTimetablesLatestValid(): APIResult<Timetable> {
        return try {
            val response = client.get {
                url("/timetable/timetables/latestValid")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<Timetable>>()
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
    suspend fun getTimetableTimetablesValid(): APIResult<List<Timetable>> {
        return try {
            val response = client.get {
                url("/timetable/timetables/valid")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<Timetable>>>()
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
