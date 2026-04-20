package hu.petrik.filcapp.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

import hu.petrik.filcapp.models.BlogPost

public class NewsBlogsApi(private val client: HttpClient) {
    @RequiresAuth
    suspend fun postNewsBlogs(body: BlogPost): APIResult<BlogPost> {
        return try {
            val response = client.post {
                url("/news/blogs")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<BlogPost>>()
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
    suspend fun deleteNewsBlogsById(id: String): APIResult<Boolean> {
        return try {
            val response = client.delete {
                url("/news/blogs/${id}")
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
    suspend fun patchNewsBlogsById(id: String, body: BlogPost): APIResult<BlogPost> {
        return try {
            val response = client.patch {
                url("/news/blogs/${id}")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<BlogPost>>()
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
    suspend fun postNewsBlogsByIdPublish(id: String, body: BlogPost): APIResult<BlogPost> {
        return try {
            val response = client.post {
                url("/news/blogs/${id}/publish")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<BlogPost>>()
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
    suspend fun postNewsBlogsByIdUnpublish(id: String, body: BlogPost): APIResult<BlogPost> {
        return try {
            val response = client.post {
                url("/news/blogs/${id}/unpublish")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<BlogPost>>()
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
