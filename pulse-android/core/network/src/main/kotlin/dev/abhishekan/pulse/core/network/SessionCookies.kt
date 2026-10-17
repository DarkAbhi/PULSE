package dev.abhishekan.pulse.core.network

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.time.Clock

interface SessionMaterialStore {
    suspend fun read(): ByteArray?

    suspend fun write(material: ByteArray)

    suspend fun clear()
}

class SessionStorageException(
    cause: Throwable,
) : IOException("Secure session storage unavailable", cause)

@Serializable
private class StoredSession(
    val value: String,
    val origin: String,
    val path: String,
    val secure: Boolean,
    val expires: Long,
)

/** Stores only the backend's session cookie, restricted to the configured API origin. */
class PersistentSessionCookies(
    private val store: SessionMaterialStore,
    private val config: NetworkConfig,
    private val clock: Clock,
) : CookiesStorage {
    private val mutex = Mutex()
    private var loaded = false
    private var pendingClear = false
    private var session: StoredSession? = null
    private val origin = origin(Url(config.baseUrl))
    private val host = URI(config.baseUrl).host.lowercase()

    private suspend fun <T> storage(block: suspend () -> T): T =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw SessionStorageException(error)
        }

    private suspend fun load() {
        if (pendingClear) {
            storage { store.clear() }
            pendingClear = false
        }
        if (loaded) return
        session =
            storage {
                store.read()?.let { bytes ->
                    Json.decodeFromString<StoredSession>(bytes.decodeToString()).also {
                        require(it.path.startsWith("/") && validValue(it.value))
                    }
                }
            }
        loaded = true
    }

    override suspend fun get(requestUrl: Url): List<Cookie> =
        mutex.withLock {
            load()
            val item = session ?: return@withLock emptyList()
            if (item.origin != origin || origin(requestUrl) != origin) return@withLock emptyList()
            if (item.expires <= clock.millis()) {
                clearLocked()
                return@withLock emptyList()
            }
            val path = requestUrl.encodedPath
            val matchesPath =
                path == item.path ||
                    (
                        path.startsWith(item.path) &&
                            (item.path.endsWith("/") || path.getOrNull(item.path.length) == '/')
                    )
            if (!matchesPath ||
                (item.secure && requestUrl.protocol.name != "https")
            ) {
                return@withLock emptyList()
            }
            listOf(
                Cookie(
                    "life_session",
                    item.value,
                    encoding = CookieEncoding.RAW,
                    path = item.path,
                    secure = item.secure,
                    httpOnly = true,
                    expires = GMTDate(item.expires),
                ),
            )
        }

    override suspend fun addCookie(
        requestUrl: Url,
        cookie: Cookie,
    ) = mutex.withLock {
        if (cookie.name != "life_session" || origin(requestUrl) != origin) return@withLock
        if (cookie.domain != null &&
            cookie.domain!!.removePrefix(".").lowercase() != host
        ) {
            return@withLock
        }
        if (cookie.secure && requestUrl.protocol.name != "https") return@withLock
        load()
        val expires =
            cookie.maxAge?.let { if (it <= 0) 0L else clock.millis() + it.toLong() * 1_000 }
                ?: cookie.expires?.timestamp ?: return@withLock
        val defaultPath = requestUrl.encodedPath.substringBeforeLast('/', "").ifEmpty { "/" }
        val path = cookie.path?.takeIf { it.startsWith("/") } ?: defaultPath
        if (expires <= clock.millis()) {
            if (session?.path == path && session?.origin == origin) clearLocked()
        } else if (validValue(cookie.value)) {
            val item = StoredSession(cookie.value, origin, path, cookie.secure, expires)
            storage { store.write(Json.encodeToString(item).encodeToByteArray()) }
            session = item
        }
    }

    suspend fun expiresAt(): Long? {
        get(Url(config.baseUrl))
        return mutex.withLock {
            session
                ?.takeIf {
                    it.origin ==
                        origin
                }?.expires
        }
    }

    suspend fun clear() = mutex.withLock { clearLocked() }

    private suspend fun clearLocked() {
        session = null
        loaded = true
        pendingClear = true
        storage { store.clear() }
        pendingClear = false
    }

    override fun close() { }

    private fun origin(url: Url) = "${url.protocol.name}://${url.host.lowercase()}:${url.port}"

    private fun validValue(value: String) =
        value.isNotEmpty() &&
            value.none {
                it.code < 0x21 ||
                    it.code >= 0x7f ||
                    it == ';' ||
                    it == ',' ||
                    it == '"' ||
                    it == '\\'
            }
}
