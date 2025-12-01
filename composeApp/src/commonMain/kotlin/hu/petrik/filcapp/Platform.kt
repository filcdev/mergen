package hu.petrik.filcapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform