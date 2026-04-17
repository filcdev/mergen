package hu.petrik.filcapp

actual val apiBaseUrl: String =
    System.getProperty("API_BASE_URL")
        ?: System.getenv("API_BASE_URL")
        ?: "http://localhost:3000"
