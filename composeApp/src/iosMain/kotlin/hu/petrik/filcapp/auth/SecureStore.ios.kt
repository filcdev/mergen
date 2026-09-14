package hu.petrik.filcapp.auth

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

/** Credentials in the iOS Keychain (`kSecClassGenericPassword`, service-scoped). */
@OptIn(ExperimentalSettingsImplementation::class)
internal actual class SecureStore actual constructor() {
    private val settings: Settings = KeychainSettings(service = KEYCHAIN_SERVICE)

    actual fun getString(key: String): String? = settings.getStringOrNull(key)

    actual fun putString(
        key: String,
        value: String,
    ) {
        settings.putString(key, value)
    }

    actual fun remove(key: String) {
        settings.remove(key)
    }

    private companion object {
        const val KEYCHAIN_SERVICE = "hu.petrik.filcapp.credentials"
    }
}
