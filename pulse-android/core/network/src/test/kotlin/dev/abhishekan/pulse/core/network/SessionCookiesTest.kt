package dev.abhishekan.pulse.core.network
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SessionCookiesTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC)
    private val config = NetworkConfig("https://api.example.test/api/", false)
    private val url = Url(config.baseUrl + "auth/login")

    private class Store : SessionMaterialStore {
        var bytes: ByteArray? = null
        var failClear = false

        override suspend fun read() = bytes

        override suspend fun write(material: ByteArray) {
            bytes = material
        }

        override suspend fun clear() {
            if (failClear) throw IOException()
            bytes = null
        }
    }

    private fun cookie(path: String = "/") =
        Cookie(
            "life_session",
            "test-secret",
            path = path,
            secure = true,
            httpOnly = true,
            expires = GMTDate(clock.millis() + 60_000),
        )

    @Test fun tailnetHttpSessionPersistsOnlyForItsOwnOrigin() =
        runTest {
            val tailnet = NetworkConfig("http://100.115.134.90:3100/api/", false)
            val loginUrl = Url(tailnet.baseUrl + "auth/login")
            val store = Store()
            val cookies = PersistentSessionCookies(store, tailnet, clock)
            cookies.addCookie(loginUrl, cookie().copy(secure = false))
            val restored = PersistentSessionCookies(store, tailnet, clock)
            assertEquals("test-secret", restored.get(Url(tailnet.baseUrl + "profile")).single().value)
            assertTrue(restored.get(Url("http://100.115.134.90:18080/api/profile")).isEmpty())
            assertTrue(restored.get(Url("http://100.115.134.91:3100/api/profile")).isEmpty())
            val secureOnly = PersistentSessionCookies(Store(), tailnet, clock)
            secureOnly.addCookie(loginUrl, cookie())
            assertTrue(secureOnly.get(loginUrl).isEmpty())
        }

    @Test fun onlySessionCookieSurvivesReopeningWithAllScopeRules() =
        runTest {
            val store = Store()
            val cookies = PersistentSessionCookies(store, config, clock)
            cookies.addCookie(url, cookie("/api/auth"))
            cookies.addCookie(url, cookie().copy(name = "unrelated", value = "ignore-me"))
            val restored = PersistentSessionCookies(store, config, clock)
            assertEquals(
                "test-secret",
                restored.get(Url("https://api.example.test/api/auth/session")).single().value,
            )
            listOf(
                "https://other.example.test/api/auth/session",
                "https://sub.api.example.test/api/auth/session",
                "http://api.example.test/api/auth/session",
                "https://api.example.test:444/api/auth/session",
                "https://api.example.test/api/authentication",
                "https://api.example.test/api/profile",
            ).forEach {
                assertTrue(restored.get(Url(it)).isEmpty())
            }
            assertFalse(store.bytes!!.decodeToString().contains("ignore-me"))
        }

    @Test fun foreignDomainsAndSessionOnlyCookiesAreNotPersisted() =
        runTest {
            val store = Store()
            val cookies = PersistentSessionCookies(store, config, clock)
            cookies.addCookie(url, cookie().copy(domain = "example.test"))
            cookies.addCookie(url, Cookie("life_session", "no-expiry", path = "/"))
            cookies.addCookie(Url("https://other.test/api/"), cookie())
            assertNull(store.bytes)
        }

    @Test fun maxAgeTakesPrecedenceAndExpiryRemovesTheSecret() =
        runTest {
            val store = Store()
            val cookies = PersistentSessionCookies(store, config, clock)
            cookies.addCookie(url, cookie().copy(maxAge = 30, expires = GMTDate(0)))
            assertEquals(clock.millis() + 30_000, cookies.expiresAt())
            val later = Clock.fixed(clock.instant().plusSeconds(31), ZoneOffset.UTC)
            assertTrue(PersistentSessionCookies(store, config, later).get(url).isEmpty())
            assertNull(store.bytes)
        }

    @Test fun deletionOnlyRemovesTheMatchingCookiePath() =
        runTest {
            val store = Store()
            val cookies = PersistentSessionCookies(store, config, clock)
            cookies.addCookie(url, cookie())
            cookies.addCookie(url, cookie("/other").copy(value = "", maxAge = 0))
            assertEquals("test-secret", cookies.get(url).single().value)
            cookies.addCookie(url, cookie().copy(value = "", maxAge = 0))
            assertTrue(cookies.get(url).isEmpty())
            assertNull(store.bytes)
        }

    @Test fun malformedStoredMaterialIsNotSilentlyErased() =
        runTest {
            val store = Store().apply { bytes = "corrupt".encodeToByteArray() }
            try {
                PersistentSessionCookies(store, config, clock).get(url)
                fail("Expected storage failure")
            } catch (
                _: SessionStorageException,
            ) {
            }
            assertEquals("corrupt", store.bytes!!.decodeToString())
        }

    @Test fun failedDeletionIsRetriedBeforeARequestCanProceed() =
        runTest {
            val store = Store()
            val cookies = PersistentSessionCookies(store, config, clock)
            cookies.addCookie(url, cookie())
            store.failClear = true
            try {
                cookies.clear()
                fail("Expected storage failure")
            } catch (
                _: SessionStorageException,
            ) {
            }
            try {
                cookies.get(url)
                fail("Expected storage failure")
            } catch (
                _: SessionStorageException,
            ) {
            }
            assertNotNull(store.bytes)
            store.failClear = false
            assertTrue(cookies.get(url).isEmpty())
            assertNull(store.bytes)
        }
}
