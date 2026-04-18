package hu.petrik.filcapp.api.client

import hu.petrik.filcapp.apiBaseUrl
import hu.petrik.filcapp.auth.SessionStore
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.api.*
import kotlinx.serialization.json.Json

val APIClient = HttpClient {
    install(Logging) {
        level = LogLevel.ALL
        logger = object : Logger {
            override fun log(message: String) {
                println("API_DEBUG: $message")
            }
        }
    }

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }

    // Prefix every request path with /api so generated API files can use bare paths.
    install(createClientPlugin("ApiPathPrefix") {
        onRequest { request, _ ->
            val segments = request.url.pathSegments.filter { it.isNotEmpty() }
            if (segments.firstOrNull() != "api") {
                request.url.pathSegments = listOf("api") + segments
            }
        }
    })

    defaultRequest {
        url(apiBaseUrl)
        val token = SessionStore.get()
        if (token != null) {
            headers.append("Cookie", "filc.session_token=$token")
        }
    }
}
