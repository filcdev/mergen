package hu.petrik.filcapp.auth

import platform.Foundation.NSUserDefaults

actual object SessionStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    private const val KEY = "session_token"

    actual fun get(): String? = defaults.stringForKey(KEY)
    actual fun set(token: String) { defaults.setObject(token, KEY) }
    actual fun clear() { defaults.removeObjectForKey(KEY) }
}
