package hu.petrik.filcapp.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

import hu.petrik.filcapp.models.Substitution
import hu.petrik.filcapp.models.SubstitutionWithRelations
import hu.petrik.filcapp.models.SubstitutionsByCohort

public class SubstitutionApi(private val client: HttpClient) {
    @RequiresAuth
    suspend fun getTimetableSubstitutions(): APIResult<List<SubstitutionWithRelations>> {
        return try {
            val response = client.get {
                url("/timetable/substitutions")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<SubstitutionWithRelations>>>()
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
    suspend fun postTimetableSubstitutions(body: Substitution): APIResult<Substitution> {
        return try {
            val response = client.post {
                url("/timetable/substitutions")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<Substitution>>()
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
    suspend fun getTimetableSubstitutionsCohortByCohortId(cohortId: String): APIResult<SubstitutionsByCohort> {
        return try {
            val response = client.get {
                url("/timetable/substitutions/cohort/${cohortId}")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<SubstitutionsByCohort>>()
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
    suspend fun getTimetableSubstitutionsRelevant(): APIResult<List<SubstitutionWithRelations>> {
        return try {
            val response = client.get {
                url("/timetable/substitutions/relevant")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<SubstitutionWithRelations>>>()
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
    suspend fun putTimetableSubstitutionsById(id: String, body: Substitution): APIResult<Substitution> {
        return try {
            val response = client.put {
                url("/timetable/substitutions/${id}")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<Substitution>>()
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
    suspend fun deleteTimetableSubstitutionsById(id: String): APIResult<Boolean> {
        return try {
            val response = client.delete {
                url("/timetable/substitutions/${id}")
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
