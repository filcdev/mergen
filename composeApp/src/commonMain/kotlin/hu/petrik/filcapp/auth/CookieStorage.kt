package hu.petrik.filcapp.auth

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.fillDefaults
import io.ktor.client.plugins.cookies.matches
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Serializable projection of a Ktor [Cookie]: `GMTDate` is not serializable, so an absolute
 * expiry is stored as a duration instead (equivalent for eviction, which is all we use it for).
 */
@Serializable
internal data class StoredCookie(
    val name: String,
    val value: String,
    val createdAt: Long,
    val domain: String? = null,
    val expiresInMillis: Long? = null,
    val httpOnly: Boolean = false,
    val path: String? = null,
    val rawEncoding: Boolean = false,
    val secure: Boolean = false,
) {
    fun toCookie(): Cookie =
        Cookie(
            name = name,
            value = value,
            encoding = if (rawEncoding) CookieEncoding.RAW else CookieEncoding.URI_ENCODING,
            domain = domain,
            path = path,
            secure = secure,
            httpOnly = httpOnly,
        )

    fun expiresAt(): Long? = expiresInMillis?.let { createdAt + it }
}

/**
 * [CookiesStorage] backed by [SecureStore] so the Chronos session cookie survives restarts.
 * Eviction mirrors Ktor's in-memory `AcceptAllCookiesStorage`: cookies are replaced by
 * name per matching URL and dropped once `maxAge`/`expires` has passed.
 */
internal class PersistentCookieStorage(
    private val store: SecureStore = SecureStore(),
    private val json: Json = AuthJson,
) : CookiesStorage {
    private val mutex = Mutex()

    override suspend fun addCookie(
        requestUrl: Url,
        cookie: Cookie,
    ) {
        if (cookie.name.isBlank()) {
            return
        }
        mutex.withLock {
            val cookieWithDefaults = cookie.fillDefaults(requestUrl)
            val now = getTimeMillis()
            val kept =
                read().filterNot { stored ->
                    stored.name == cookie.name && stored.toCookie().fillDefaults(requestUrl).matches(requestUrl)
                }
            write(
                kept + cookieWithDefaults.toStored(createdAt = now),
            )
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> =
        mutex.withLock {
            val now = getTimeMillis()
            val stored = read()
            val live = stored.filter { it.expiresAt()?.let { expires -> expires > now } ?: true }
            if (live.size != stored.size) {
                write(live)
            }
            live.map { it.toCookie() }
                .filter { it.fillDefaults(requestUrl).matches(requestUrl) }
        }

    /** Drops every stored cookie; called on sign-out. */
    suspend fun clear() {
        mutex.withLock { store.remove(COOKIES_KEY) }
    }

    override fun close() = Unit

    private fun write(cookies: List<StoredCookie>) {
        store.putString(COOKIES_KEY, json.encodeToString(cookies))
    }

    private fun read(): List<StoredCookie> =
        store.getString(COOKIES_KEY)
            ?.let { stored -> runCatching { json.decodeFromString<List<StoredCookie>>(stored) }.getOrNull() }
            .orEmpty()

    private fun Cookie.toStored(createdAt: Long): StoredCookie =
        StoredCookie(
            name = name,
            value = value,
            createdAt = createdAt,
            domain = domain,
            expiresInMillis = maxAge?.let { it * 1000L } ?: expires?.let { it.timestamp - createdAt },
            httpOnly = httpOnly,
            path = path,
            rawEncoding = encoding == CookieEncoding.RAW,
            secure = secure,
        )

    private companion object {
        const val COOKIES_KEY = "chronos.cookies"
    }
}
