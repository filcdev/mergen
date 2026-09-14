package hu.petrik.filcapp.auth

import kotlinx.serialization.json.Json

/** Credentials persisted between launches: the Entra refresh token and any in-flight sign-in. */
internal class AuthStorage(
    private val store: SecureStore = SecureStore(),
    private val json: Json = AuthJson,
) {
    fun readRefreshToken(): String? = store.getString(REFRESH_TOKEN_KEY)

    fun writeRefreshToken(value: String?) {
        if (value == null) {
            store.remove(REFRESH_TOKEN_KEY)
        } else {
            store.putString(REFRESH_TOKEN_KEY, value)
        }
    }

    fun readPendingFlow(): PendingFlow? =
        store.getString(PENDING_FLOW_KEY)?.let { stored ->
            runCatching { json.decodeFromString<PendingFlow>(stored) }.getOrNull()
        }

    fun writePendingFlow(value: PendingFlow?) {
        if (value == null) {
            store.remove(PENDING_FLOW_KEY)
        } else {
            store.putString(PENDING_FLOW_KEY, json.encodeToString(value))
        }
    }

    fun clear() {
        writeRefreshToken(null)
        writePendingFlow(null)
    }

    private companion object {
        const val REFRESH_TOKEN_KEY = "entra.refreshToken"
        const val PENDING_FLOW_KEY = "entra.pendingFlow"
    }
}
