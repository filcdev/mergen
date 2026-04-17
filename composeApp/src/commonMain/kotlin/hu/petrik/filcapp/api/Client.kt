package hu.petrik.filcapp.api.client

import hu.petrik.filcapp.apiBaseUrl
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
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

    defaultRequest {
        url("$apiBaseUrl/")
    }
}
