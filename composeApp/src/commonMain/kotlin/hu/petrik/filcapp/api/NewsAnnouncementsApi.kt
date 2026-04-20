package hu.petrik.filcapp.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

import hu.petrik.filcapp.models.Announcement

public class NewsAnnouncementsApi(private val client: HttpClient) {
    @RequiresAuth
    suspend fun getNewsAnnouncements(): APIResult<List<Announcement>> {
        return try {
            val response = client.get {
                url("/news/announcements")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<List<Announcement>>>()
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
    suspend fun postNewsAnnouncements(body: Announcement): APIResult<Announcement> {
        return try {
            val response = client.post {
                url("/news/announcements")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<Announcement>>()
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
    suspend fun getNewsAnnouncementsById(id: String): APIResult<Announcement> {
        return try {
            val response = client.get {
                url("/news/announcements/${id}")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<Announcement>>()
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
    suspend fun deleteNewsAnnouncementsById(id: String): APIResult<Boolean> {
        return try {
            val response = client.delete {
                url("/news/announcements/${id}")
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
    suspend fun patchNewsAnnouncementsById(id: String, body: Announcement): APIResult<Announcement> {
        return try {
            val response = client.patch {
                url("/news/announcements/${id}")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<Announcement>>()
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
