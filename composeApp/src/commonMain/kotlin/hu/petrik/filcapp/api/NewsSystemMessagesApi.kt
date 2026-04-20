package hu.petrik.filcapp.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

import hu.petrik.filcapp.models.SystemMessage

public class NewsSystemMessagesApi(private val client: HttpClient) {
    @RequiresAuth
    suspend fun getNewsSystemMessages(): APIResult<List<SystemMessage>> {
        return try {
            val response = client.get {
                url("/news/system-messages")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<SystemMessage>>>()
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
    suspend fun postNewsSystemMessages(body: SystemMessage): APIResult<SystemMessage> {
        return try {
            val response = client.post {
                url("/news/system-messages")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<SystemMessage>>()
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
    suspend fun getNewsSystemMessagesById(id: String): APIResult<SystemMessage> {
        return try {
            val response = client.get {
                url("/news/system-messages/${id}")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<SystemMessage>>()
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
    suspend fun deleteNewsSystemMessagesById(id: String): APIResult<Boolean> {
        return try {
            val response = client.delete {
                url("/news/system-messages/${id}")
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
    suspend fun patchNewsSystemMessagesById(id: String, body: SystemMessage): APIResult<SystemMessage> {
        return try {
            val response = client.patch {
                url("/news/system-messages/${id}")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<SystemMessage>>()
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
