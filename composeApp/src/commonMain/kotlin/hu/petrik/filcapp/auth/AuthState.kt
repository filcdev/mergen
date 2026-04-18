package hu.petrik.filcapp.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AuthState {
    var sessionToken: String? by mutableStateOf(null)
        private set
    var cohortId: String? by mutableStateOf(null)
        private set
    var displayName: String? by mutableStateOf(null)
        private set
    var profileImage: String? by mutableStateOf(null)
        private set

    fun init() {
        sessionToken = SessionStore.get()
    }

    fun login(token: String) {
        SessionStore.set(token)
        sessionToken = token
    }

    suspend fun fetchSession(): Boolean {
        val user = fetchSessionUser()
        if (user == null) {
            logout()
            return false
        }
        cohortId = user.cohortId
        displayName = user.displayName.ifEmpty { user.name }
        profileImage = user.image
        return true
    }

    fun logout() {
        SessionStore.clear()
        sessionToken = null
        cohortId = null
        displayName = null
        profileImage = null
    }

    val isLoggedIn: Boolean get() = sessionToken != null
}
