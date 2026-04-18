package hu.petrik.filcapp.auth

import android.content.SharedPreferences

actual object SessionStore {
    internal lateinit var prefs: SharedPreferences

    actual fun get(): String? = prefs.getString("session_token", null)
    actual fun set(token: String) { prefs.edit().putString("session_token", token).apply() }
    actual fun clear() { prefs.edit().remove("session_token").apply() }
}
