package hu.petrik.filcapp.auth

import java.util.prefs.Preferences

actual object SessionStore {
    private val prefs = Preferences.userRoot().node("hu/petrik/filcapp")

    actual fun get(): String? = prefs.get("session_token", null)
    actual fun set(token: String) { prefs.put("session_token", token) }
    actual fun clear() { prefs.remove("session_token") }
}
