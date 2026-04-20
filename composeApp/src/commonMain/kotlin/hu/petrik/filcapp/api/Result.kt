package hu.petrik.filcapp.api

import kotlinx.serialization.Serializable

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresAuth

@Serializable
public data class ApiErrorMessage(
    val success: Boolean,
    val message: String
)

@Serializable
public data class ApiEnvelope<out T>(
    val success: Boolean,
    val data: T
)

public sealed interface APIResult<out T> {
    data class Success<T>(val data: T) : APIResult<T>
    data class Failure(val error: ApiError) : APIResult<Nothing>
}

public fun <T> APIResult<T>.getOrThrow(): T {
    return when (this) {
        is APIResult.Success -> this.data
        is APIResult.Failure -> throw UnwrappedResultException(this.error)
    }
}

public fun <T> APIResult<T>.getOrElse(default: (error: ApiError) -> T): T {
    return when (this) {
        is APIResult.Success -> this.data
        is APIResult.Failure -> default(this.error)
    }
}

public inline fun <T, R> APIResult<T>.map(transform: (T) -> R): APIResult<R> {
    return when (this) {
        is APIResult.Success -> APIResult.Success(transform(this.data))
        is APIResult.Failure -> this
    }
}

public inline fun <T, R> APIResult<T>.flatMap(transform: (T) -> APIResult<R>): APIResult<R> {
    return when (this) {
        is APIResult.Success -> transform(this.data)
        is APIResult.Failure -> this
    }
}

public inline fun <T, R> APIResult<T>.fold(
    onFailure: (error: ApiError) -> R,
    onSuccess: (value: T) -> R
): R {
    return when (this) {
        is APIResult.Failure -> onFailure(this.error)
        is APIResult.Success -> onSuccess(this.data)
    }
}

public inline fun <T> APIResult<T>.mapError(transform: (ApiError) -> ApiError): APIResult<T> {
    return when (this) {
        is APIResult.Success -> this
        is APIResult.Failure -> APIResult.Failure(transform(this.error))
    }
}

public class UnwrappedResultException(val apiError: ApiError) :
    RuntimeException("Attempted to unwrap a Result that contained an error: $apiError")

public sealed interface ApiError {
    data class NetworkError(val cause: Throwable) : ApiError
    data class BackendError(val httpCode: Int, val message: String) : ApiError
    data class SerializationError(val details: String) : ApiError
    data class Unknown(val cause: Throwable) : ApiError
}
