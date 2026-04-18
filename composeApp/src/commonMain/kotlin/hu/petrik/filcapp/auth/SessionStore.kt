package hu.petrik.filcapp.auth

expect object SessionStore {
    fun get(): String?
    fun set(token: String)
    fun clear()
}
