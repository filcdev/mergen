package hu.petrik.filcapp.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

import hu.petrik.filcapp.models.Cohort

public class CohortApi(private val client: HttpClient) {
    suspend fun getCohort(): APIResult<List<Cohort>> {
        return try {
            val response = client.get {
                url("/cohort")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<Cohort>>>()
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
    suspend fun getTimetableCohortsAllForTimetable(timetableId: String): APIResult<List<Cohort>> {
        return try {
            val response = client.get {
                url("/timetable/cohorts/getAllForTimetable/${timetableId}")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<Cohort>>>()
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
