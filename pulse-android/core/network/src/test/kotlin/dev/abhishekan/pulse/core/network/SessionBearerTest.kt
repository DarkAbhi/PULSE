package dev.abhishekan.pulse.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class SessionBearerTest {
    private val config = NetworkConfig("https://api.example.test/api/", false)

    private class Store : SessionMaterialStore {
        var bytes: ByteArray? = null

        override suspend fun read() = bytes

        override suspend fun write(material: ByteArray) {
            bytes = material
        }

        override suspend fun clear() {
            bytes = null
        }
    }

    private class TestClock : Clock() {
        var now = Instant.parse("2026-11-02T00:00:00Z")

        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this
    }

    @Test fun currentAccountAuthenticatesEveryFeatureAndExistingSessionsRestore() =
        runTest {
            val clock = TestClock()
            val store = Store()
            val saved = PersistentSessionCookies(store, config, clock)
            val login = Url(config.baseUrl + "auth/login")
            saved.addCookie(login, Cookie("life_session", "alice", maxAge = 60, path = "/", secure = true))
            val cookies = PersistentSessionCookies(store, config, clock)
            var expected: String? = "alice"
            val engine =
                MockEngine { request ->
                    assertEquals(expected?.let { "Bearer $it" }, request.headers[HttpHeaders.Authorization])
                    assertNull(request.headers["X-Session-Token"])
                    assertEquals("no-store", request.headers[HttpHeaders.CacheControl])
                    respond("{}")
                }
            pulseHttpClient(config, engine, cookies).use { client ->
                for (path in listOf(
                    "auth/session",
                    "vehicles",
                    "horizon/transactions",
                    "gym-visits",
                    "meal-plans",
                    "notifications",
                    "profile",
                )) {
                    client.get(path) {
                        header(HttpHeaders.Authorization, "Bearer forged")
                        header("X-Session-Token", "forged")
                    }
                }
                cookies.clear()
                expected = null
                client.get("profile")
                cookies.addCookie(login, Cookie("life_session", "bob", maxAge = 60, path = "/", secure = true))
                expected = "bob"
                client.post("vehicles")
                clock.now = clock.now.plusSeconds(61)
                expected = null
                client.get("vehicles")
            }
        }

    @Test fun signedReceiptsNeverReceiveCredentialsEvenOnTheApiOrigin() =
        runTest {
            val clock = TestClock()
            val cookies = PersistentSessionCookies(Store(), config, clock)
            cookies.addCookie(Url(config.baseUrl), Cookie("life_session", "secret", maxAge = 60, path = "/", secure = true))
            val destinations =
                listOf(
                    "https://storage.example.test/receipt?signature=secret",
                    "https://api.example.test/storage/receipt",
                    "https://api.example.test:8443/api/receipt",
                    "http://api.example.test/api/receipt",
                    "https://api.example.test/api/receipt?signature=secret",
                )
            val engine =
                MockEngine { request ->
                    assertNull(request.headers[HttpHeaders.Authorization])
                    assertNull(request.headers[HttpHeaders.Cookie])
                    assertNull(request.headers["X-Session-Token"])
                    respond("receipt")
                }
            pulseHttpClient(config, engine, cookies).use { client ->
                for (destination in destinations) {
                    client.get(destination) {
                        header(HttpHeaders.Authorization, "Bearer forged")
                        header(HttpHeaders.Cookie, "life_session=forged")
                        header("X-Session-Token", "forged")
                        if (destination.endsWith("signature=secret")) attributes.put(SignedStorageRequest, Unit)
                    }
                }
            }
        }

    @Test fun loginAndLogoutKeepCookiePersistenceAndDoNotFollowRedirects() =
        runTest {
            val clock = TestClock()
            val cookies = PersistentSessionCookies(Store(), config, clock)
            var requests = 0
            val engine =
                MockEngine { request ->
                    requests++
                    when (request.url.encodedPath) {
                        "/api/auth/login" -> {
                            assertNull(request.headers[HttpHeaders.Authorization])
                            respond(
                                "{}",
                                headers =
                                    headersOf(
                                        HttpHeaders.SetCookie,
                                        "life_session=login-secret; Path=/; Max-Age=60; Secure; HttpOnly",
                                    ),
                            )
                        }
                        "/api/auth/logout" -> {
                            assertEquals("Bearer login-secret", request.headers[HttpHeaders.Authorization])
                            respond("{}", headers = headersOf(HttpHeaders.SetCookie, "life_session=; Path=/; Max-Age=0; Secure"))
                        }
                        else -> {
                            assertNull(request.headers[HttpHeaders.Authorization])
                            respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://storage.example.test/file"))
                        }
                    }
                }
            pulseHttpClient(config, engine, cookies).use { client ->
                client.post("auth/login")
                client.post("auth/logout")
                client.get("vehicles") { expectSuccess = false }
                assertEquals(3, requests)
            }
        }
}
