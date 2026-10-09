package hu.petrik.filcapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

@androidx.compose.runtime.Composable
expect fun appVersionLabel(): String
