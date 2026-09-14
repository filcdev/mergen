package hu.petrik.filcapp.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val AuthJson: Json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

/**
 * Chronos / Better Auth client. The session lives in a [PersistentCookieStorage] cookie jar, so
 * every request made with this client is authenticated once the user has signed in - including
 * requests issued by the generated API client when it is constructed with this instance.
 */
internal fun createAuthHttpClient(cookies: PersistentCookieStorage): HttpClient =
    HttpClient {
        install(ContentNegotiation) { json(AuthJson) }
        install(HttpCookies) { storage = cookies }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        }
        expectSuccess = false
        defaultRequest {
            accept(ContentType.Application.Json)
        }
    }

private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val CONNECT_TIMEOUT_MILLIS = 15_000L
