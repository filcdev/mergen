package hu.petrik.filcapp.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

import hu.petrik.filcapp.models.CardListResponse
import hu.petrik.filcapp.models.CardResponse
import hu.petrik.filcapp.models.DeviceListResponse
import hu.petrik.filcapp.models.DeviceResponse
import hu.petrik.filcapp.models.DeviceStatsResponse
import hu.petrik.filcapp.models.DoorlockActivationResponse
import hu.petrik.filcapp.models.DoorlockLogListResponse
import hu.petrik.filcapp.models.DoorlockStatsResponse
import hu.petrik.filcapp.models.DoorlockUserListResponse

public class DoorlockApi(private val client: HttpClient) {
    @RequiresAuth
    suspend fun getDoorlockCards(): APIResult<CardListResponse> {
        return try {
            val response = client.get {
                url("/doorlock/cards")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<CardListResponse>>()
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
    suspend fun postDoorlockCards(body: CardResponse): APIResult<CardResponse> {
        return try {
            val response = client.post {
                url("/doorlock/cards")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<CardResponse>>()
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
    suspend fun getDoorlockCardsUsers(): APIResult<DoorlockUserListResponse> {
        return try {
            val response = client.get {
                url("/doorlock/cards/users")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<DoorlockUserListResponse>>()
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
    suspend fun putDoorlockCardsById(id: String, body: CardResponse): APIResult<CardResponse> {
        return try {
            val response = client.put {
                url("/doorlock/cards/${id}")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<CardResponse>>()
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
    suspend fun deleteDoorlockCardsById(id: String): APIResult<Boolean> {
        return try {
            val response = client.delete {
                url("/doorlock/cards/${id}")
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
    suspend fun getDoorlockDevices(): APIResult<DeviceListResponse> {
        return try {
            val response = client.get {
                url("/doorlock/devices")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<DeviceListResponse>>()
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
    suspend fun postDoorlockDevices(body: DeviceResponse): APIResult<DeviceResponse> {
        return try {
            val response = client.post {
                url("/doorlock/devices")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<DeviceResponse>>()
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
    suspend fun putDoorlockDevicesById(id: String, body: DeviceResponse): APIResult<DeviceResponse> {
        return try {
            val response = client.put {
                url("/doorlock/devices/${id}")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<DeviceResponse>>()
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
    suspend fun deleteDoorlockDevicesById(id: String): APIResult<Boolean> {
        return try {
            val response = client.delete {
                url("/doorlock/devices/${id}")
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
    suspend fun getDoorlockDevicesByIdStats(id: String): APIResult<DeviceStatsResponse> {
        return try {
            val response = client.get {
                url("/doorlock/devices/${id}/stats")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<DeviceStatsResponse>>()
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
    suspend fun getDoorlockLogs(): APIResult<DoorlockLogListResponse> {
        return try {
            val response = client.get {
                url("/doorlock/logs")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<DoorlockLogListResponse>>()
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
    suspend fun getDoorlockSelfCards(): APIResult<CardListResponse> {
        return try {
            val response = client.get {
                url("/doorlock/self/cards")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<CardListResponse>>()
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
    suspend fun postDoorlockSelfCardsByIdActivate(id: String, body: DoorlockActivationResponse): APIResult<DoorlockActivationResponse> {
        return try {
            val response = client.post {
                url("/doorlock/self/cards/${id}/activate")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<DoorlockActivationResponse>>()
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
    suspend fun putDoorlockSelfCardsByIdFrozen(id: String, body: CardResponse): APIResult<CardResponse> {
        return try {
            val response = client.put {
                url("/doorlock/self/cards/${id}/frozen")
                setBody(body)
                contentType(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<CardResponse>>()
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
    suspend fun getDoorlockStatsOverview(): APIResult<DoorlockStatsResponse> {
        return try {
            val response = client.get {
                url("/doorlock/stats/overview")
            }
            if (response.status.isSuccess()) {
                val envelope = response.body<ApiEnvelope<DoorlockStatsResponse>>()
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
