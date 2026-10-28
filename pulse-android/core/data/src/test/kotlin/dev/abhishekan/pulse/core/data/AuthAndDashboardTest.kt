package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.model.WorkoutToday
import dev.abhishekan.pulse.core.domain.usecase.Login
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.SessionMaterialStore
import dev.abhishekan.pulse.core.network.pulseHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.Closeable
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class AuthAndDashboardTest {
    private class Store : SessionMaterialStore {
        var bytes: ByteArray? = null
        var beforeClear: (suspend () -> Unit)? = null

        override suspend fun read() = bytes

        override suspend fun write(material: ByteArray) {
            bytes = material.copyOf()
        }

        override suspend fun clear() {
            beforeClear?.invoke()
            bytes = null
        }
    }

    private class TestClock : Clock() {
        var now = Instant.parse("2026-10-16T12:00:00Z")

        override fun instant() = now

        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this
    }

    private class Harness(
        val store: Store = Store(),
    ) : Closeable {
        val clock = TestClock()
        val config = NetworkConfig("https://api.example.test/api/", false)
        val cookies = PersistentSessionCookies(store, config, clock)
        var sessionError: Exception? = null
        var rejectLogin = false
        var omitCookie = false
        var failLogout = false
        var rejectProfile = false
        var timezone = "UTC"
        var badHorizon = false
        var requestBody = ""
        val requests = mutableListOf<String>()
        val engine =
            MockEngine { request ->
                val path = request.url.encodedPath
                requests += path
                var status = HttpStatusCode.OK
                var text = "{}"
                var setCookie: String? = null
                when (path) {
                    "/api/auth/login" -> {
                        requestBody = (request.body as TextContent).text
                        if (rejectLogin) {
                            status = HttpStatusCode.Unauthorized
                        } else {
                            text = """{"username":"alice"}"""
                            if (!omitCookie) {
                                setCookie =
                                    "life_session=fixture-secret; Path=/; Max-Age=60; HttpOnly; Secure"
                            }
                        }
                    }
                    "/api/auth/session" -> {
                        sessionError?.let { throw it }
                        assertTrue(
                            request.headers[HttpHeaders.Cookie].orEmpty().contains(
                                "life_session=fixture-secret",
                            ),
                        )
                        text = """{"username":"alice"}"""
                    }
                    "/api/auth/logout" -> {
                        if (failLogout) throw IOException("fixture connection failure")
                        setCookie = "life_session=; Path=/; Max-Age=0; HttpOnly; Secure"
                    }
                    "/api/profile" -> {
                        if (rejectProfile) status = HttpStatusCode.Unauthorized
                        text = """{"has_profile":true,"name":null,"timezone":"$timezone"}"""
                    }
                    "/api/workout/today" -> text = """{"visited":false}"""
                    "/api/horizon" ->
                        text =
                            if (badHorizon) {
                                """{"currency":"₹","base_amount":null}"""
                            } else {
                                """{"currency":"₹","base_amount":12345678901234567890.123456789,"remaining_amount":0.000000000000123456789,"total_deductions":12.50}"""
                            }
                    "/api/notifications" -> {
                        assertEquals("5", request.url.parameters["limit"])
                        text =
                            "[]"
                    }
                }
                val headers =
                    Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                        setCookie?.let { append(HttpHeaders.SetCookie, it) }
                    }
                respond(text, status, headers)
            }
        val client = pulseHttpClient(config, engine, cookies)
        val api = PulseApi(client)
        val auth = PulseAuthRepository(api, cookies, clock)
        val dashboard = PulseDashboardRepository(api, auth)

        suspend fun login() {
            auth.restore()
            assertEquals(Outcome.Success(Unit), Login(auth)("  alice  ", " exact password "))
        }

        override fun close() {
            client.close()
        }
    }

    @Test fun loginTrimsOnlyUsernameAndRestoresFromPersistedCookie() =
        runTest {
            val store = Store()
            Harness(store).use { h ->
                h.login()
                assertTrue(h.requestBody.contains("\"username\":\"alice\""))
                assertTrue(h.requestBody.contains("\"password\":\" exact password \""))
                assertTrue(h.auth.session.value is SessionState.Authenticated)
            }
            Harness(store).use { h ->
                h.auth.restore()
                assertEquals("alice", (h.auth.session.value as SessionState.Authenticated).username)
                assertEquals(listOf("/api/auth/session", "/api/profile"), h.requests)
            }
        }

    @Test fun loginLoadsPreferenceBeforeScreensAndLogoutResetsIt() =
        runTest {
            Harness().use { h ->
                h.timezone = "Asia/Tokyo"
                h.login()
                assertEquals("Asia/Tokyo", dev.abhishekan.pulse.core.domain.model.UserTimeZone.zone.id)
                h.auth.logout()
                assertEquals("UTC", dev.abhishekan.pulse.core.domain.model.UserTimeZone.zone.id)
            }
        }

    @Test fun networkAndTimeoutDuringRestorationKeepStoredSession() =
        runTest {
            val store = Store()
            Harness(store).use { it.login() }
            val original = store.bytes!!.copyOf()
            Harness(store).use { h ->
                h.sessionError = IOException("private stack information")
                h.auth.restore()
                assertEquals(SessionState.Unavailable(Failure.Connection), h.auth.session.value)
                assertArrayEquals(original, store.bytes)
                h.sessionError = SocketTimeoutException()
                h.auth.restore()
                assertEquals(SessionState.Unavailable(Failure.Timeout), h.auth.session.value)
                assertArrayEquals(original, store.bytes)
            }
        }

    @Test fun invalidCredentialsAndMissingCookieNeverGrantAccess() =
        runTest {
            Harness().use { h ->
                h.auth.restore()
                h.rejectLogin = true
                assertEquals(Outcome.Error(Failure.InvalidCredentials), Login(h.auth)("alice", "wrong"))
                assertTrue(h.auth.session.value is SessionState.SignedOut)
                h.rejectLogin = false
                h.omitCookie = true
                assertEquals(Outcome.Error(Failure.Malformed), Login(h.auth)("alice", "password"))
                assertTrue(h.auth.session.value is SessionState.SignedOut)
                assertNull(h.store.bytes)
            }
        }

    @Test fun emptyInputNeverReachesServer() =
        runTest {
            Harness().use { h ->
                assertEquals(Outcome.Error(Failure.InvalidInput), Login(h.auth)("   ", "password"))
                assertEquals(Outcome.Error(Failure.InvalidInput), Login(h.auth)("alice", ""))
                assertTrue(h.requests.isEmpty())
            }
        }

    @Test fun failedLogoutKeepsSessionAndSuccessfulLogoutRemovesIt() =
        runTest {
            Harness().use { h ->
                h.login()
                h.failLogout = true
                assertEquals(Outcome.Error(Failure.Connection), h.auth.logout())
                assertTrue(h.auth.session.value is SessionState.Authenticated)
                assertNotNull(h.store.bytes)
                h.failLogout = false
                assertEquals(Outcome.Success(Unit), h.auth.logout())
                assertEquals(SessionState.SignedOut(), h.auth.session.value)
                assertNull(h.store.bytes)
            }
        }

    @Test fun expiryAndUnauthorizedDashboardResponsesRemoveAccess() =
        runTest {
            Harness().use { h ->
                h.login()
                h.clock.now = h.clock.now.plusSeconds(61)
                h.auth.checkExpiry()
                assertEquals(SessionState.SignedOut(expired = true), h.auth.session.value)
                assertNull(h.store.bytes)
                h.login()
                h.rejectProfile = true
                assertEquals(Outcome.Error(Failure.Unauthorized), h.dashboard.profile())
                assertEquals(SessionState.SignedOut(expired = true), h.auth.session.value)
                assertNull(h.store.bytes)
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun localRevocationFinishesWhenTheSessionObserverCancelsItsCaller() =
        runTest {
            Harness().use { h ->
                h.login()
                h.clock.now = h.clock.now.plusSeconds(61)
                val clearing = CompletableDeferred<Unit>()
                h.store.beforeClear = {
                    clearing.complete(Unit)
                    delay(100)
                }
                backgroundScope.launch {
                    h.auth.session.collectLatest {
                        if (it is SessionState.Authenticated) h.auth.checkExpiry()
                    }
                }
                runCurrent()
                assertTrue(clearing.isCompleted)
                assertEquals(SessionState.SignedOut(expired = true), h.auth.session.value)
                advanceTimeBy(101)
                runCurrent()
                assertNull(h.store.bytes)
            }
        }

    @Test fun oldRequestCannotExpireANewerLogin() =
        runTest {
            Harness().use { h ->
                h.login()
                val old = h.auth.version
                h.auth.logout()
                h.login()
                h.auth.expire(old)
                assertTrue(h.auth.session.value is SessionState.Authenticated)
            }
        }

    @Test fun cancellationPropagatesWithoutErasingSession() =
        runTest {
            val store = Store()
            Harness(store).use { it.login() }
            val original = store.bytes!!.copyOf()
            Harness(store).use { h ->
                h.sessionError = CancellationException("fixture cancellation")
                try {
                    h.auth.restore()
                    fail("Expected cancellation")
                } catch (
                    _: CancellationException,
                ) {
                }
                assertArrayEquals(original, store.bytes)
            }
        }

    @Test fun dashboardPreservesPrecisionAndHandlesOptionalAndMalformedValues() =
        runTest {
            Harness().use { h ->
                val horizon = (h.dashboard.horizon() as Outcome.Success).value
                assertEquals("12345678901234567890.123456789", horizon.baseAmount.toPlainString())
                assertEquals("0.000000000000123456789", horizon.remainingAmount.toPlainString())
                assertEquals(Outcome.Success(Profile(null)), h.dashboard.profile())
                assertEquals(Outcome.Success(emptyList<Notification>()), h.dashboard.notifications())
                assertEquals(Outcome.Success(WorkoutToday(false)), h.dashboard.workout())
                h.badHorizon = true
                assertEquals(Outcome.Error(Failure.Malformed), h.dashboard.horizon())
            }
        }
}
