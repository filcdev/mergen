package hu.petrik.filcapp.auth

/**
 * Key/value storage for credentials that must survive app restarts.
 *
 * Android: Keystore-encrypted `SharedPreferences`.
 * iOS: Keychain (`kSecClassGenericPassword`).
 */
internal expect class SecureStore() {
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )

    fun remove(key: String)
}
