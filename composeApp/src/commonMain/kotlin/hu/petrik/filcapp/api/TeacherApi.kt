package hu.petrik.filcapp.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

import hu.petrik.filcapp.models.Teacher

public class TeacherApi(private val client: HttpClient) {
    suspend fun getTimetableTeachersAll(): APIResult<List<Teacher>> {
        return try {
            val response = client.get {
                url("/timetable/teachers/getAll")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<Teacher>>>()
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
