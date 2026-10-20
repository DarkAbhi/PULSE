package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.APIKey
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.SessionMaterialStore
import dev.abhishekan.pulse.core.network.pulseHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.Closeable
import java.net.SocketTimeoutException
import java.time.Clock

class AccountRepositoriesTest {
    private class Harness : Closeable {
        var body = "{}"
        var status = HttpStatusCode.OK
        var error: Exception? = null
        var sent = ""
        var path = ""
        var query = ""
        var method = ""
        val store =
            object : SessionMaterialStore {
                var value: ByteArray? = null

                override suspend fun read() = value

                override suspend fun write(material: ByteArray) {
                    value =
                        material
                }

                override suspend fun clear() {
                    value = null
                }
            }
        val clock = Clock.systemUTC()
        val config = NetworkConfig("https://fixture.example.test/api/", false)
        val cookies = PersistentSessionCookies(store, config, clock)
        val client =
            pulseHttpClient(
                config,
                MockEngine { request ->
                    path = request.url.encodedPath
                    query = request.url.encodedQuery
                    method = request.method.value
                    sent =
                        (request.body as? TextContent)?.text.orEmpty()
                    if (path ==
                        "/api/auth/login"
                    ) {
                        respond(
                            """{"username":"fixture"}""",
                            HttpStatusCode.OK,
                            headersOf(
                                HttpHeaders.ContentType to listOf("application/json"),
                                HttpHeaders.SetCookie to listOf("life_session=fixture; Path=/; Max-Age=3600; Secure"),
                            ),
                        )
                    } else if (path ==
                        "/api/auth/session"
                    ) {
                        respond("""{"username":"fixture"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                    } else {
                        error?.let { throw it }
                        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                },
                cookies,
            )
        val api = PulseApi(client)
        val auth = PulseAuthRepository(api, cookies, clock)
        val dashboard = PulseDashboardRepository(api, auth)
        val profile = PulseProfileRepository(api, auth, dashboard)
        val notifications = PulseNotificationsRepository(api, auth, dashboard)

        override fun close() {
            client.close()
        }
    }

    @Test fun successfulExplicitRefreshesInvalidateHomeButReadFailuresDoNot() =
        runTest {
            Harness().use { h ->
                assertEquals(0L, h.dashboard.changes.first())
                h.body = """{"has_profile":true,"name":"Updated elsewhere"}"""
                assertTrue(h.profile.profile() is Outcome.Success)
                assertEquals(1L, h.dashboard.changes.first())
                h.body = "[]"
                assertTrue(h.notifications.list() is Outcome.Success)
                assertEquals(2L, h.dashboard.changes.first())
                h.status = HttpStatusCode.ServiceUnavailable
                h.notifications.list()
                h.profile.profile()
                assertEquals(2L, h.dashboard.changes.first())
            }
        }

    @Test fun profileAbsenceIsExplicitAndNameSaveNotifiesHome() =
        runTest {
            Harness().use { h ->
                h.body = """{"has_profile":false}"""
                assertEquals(Profile(null, false), (h.profile.profile() as Outcome.Success).value)
                h.body = """{"name":"😀 Asha"}"""
                val result = h.profile.saveName("😀 Asha")
                assertTrue(result is Outcome.Success)
                assertEquals("PUT", h.method)
                assertEquals(
                    "😀 Asha",
                    Json
                        .parseToJsonElement(h.sent)
                        .jsonObject["name"]!!
                        .jsonPrimitive.content,
                )
                assertEquals(Profile("😀 Asha"), (result as Outcome.Success).value)
            }
        }

    @Test fun passwordBytesAndWhitespaceArePreservedAndWrongPasswordIsNotExpiry() =
        runTest {
            Harness().use { h ->
                h.auth.restore()
                assertEquals(Outcome.Success(Unit), h.auth.login("fixture", "unused"))
                h.status = HttpStatusCode.BadRequest
                h.body =
                    """{"error":"current password is incorrect"}"""
                assertEquals(Outcome.Error(Failure.InvalidCredentials), h.profile.changePassword(" old ", "😀é "))
                val sent = Json.parseToJsonElement(h.sent).jsonObject
                assertEquals(" old ", sent["current_password"]!!.jsonPrimitive.content)
                assertEquals("😀é ", sent["new_password"]!!.jsonPrimitive.content)
                assertTrue(h.auth.session.value is SessionState.Authenticated)
                h.status = HttpStatusCode.Unauthorized
                assertEquals(Outcome.Error(Failure.Unauthorized), h.profile.profile())
                assertTrue(h.auth.session.value is SessionState.SignedOut)
            }
        }

    @Test fun keysAreOneTimeAndMalformedCreationIsUncertainInsteadOfFabricatedToken() =
        runTest {
            Harness().use { h ->
                h.body = "null"
                assertEquals(emptyList<APIKey>(), (h.profile.keys() as Outcome.Success).value)
                h.body =
                    """{"id":1,"name":"Fixture","token_prefix":"lt_12345678","created_at":"2026-10-18T00:00:00Z","token":"lt_${"a".repeat(
                        64,
                    )}"}"""
                val result = h.profile.createKey("Fixture") as Outcome.Success
                assertEquals("lt_${"a".repeat(64)}", result.value.token)
                assertFalse(result.value.toString().contains(result.value.token))
                assertEquals("POST", h.method)
                h.body = """{"id":1,"name":"Fixture","token_prefix":"lt_12345678","created_at":"2026-10-18T00:00:00Z"}"""
                assertEquals(Outcome.Error(Failure.Malformed), h.profile.createKey("Fixture"))
            }
        }

    @Test fun notificationCenterUsesFullLimitContractAndRetainsRoutingMetadata() =
        runTest {
            Harness().use { h ->
                h.body =
                    """[{"id":1,"source":"Garage","title":"Care","body":null,"priority":1,"target_path":"/garage/42","created_at":"2026-10-18T00:00:00Z"}]"""
                val items = (h.notifications.list() as Outcome.Success).value
                assertEquals("", h.query)
                assertEquals("/api/notifications", h.path)
                assertEquals("Garage", items.single().source)
                assertEquals(NotificationTarget.Vehicle(42), NotificationTarget.parse(items.single().targetPath))
                h.status = HttpStatusCode.NotFound
                assertEquals(Outcome.Success(Unit), h.notifications.dismiss(1))
                assertEquals("DELETE", h.method)
                assertEquals("/api/notifications/1", h.path)
                h.status = HttpStatusCode.NoContent
                h.body = ""
                assertEquals(Outcome.Success(Unit), h.notifications.clear())
                assertEquals("/api/notifications", h.path)
            }
        }

    @Test fun absentRevokedKeysAreSuccessAndNetworkOutcomesStayDomainLevel() =
        runTest {
            Harness().use { h ->
                h.status = HttpStatusCode.NotFound
                assertEquals(Outcome.Success(Unit), h.profile.revokeKey(4))
                h.error = SocketTimeoutException()
                assertEquals(Outcome.Error(Failure.Timeout), h.profile.createKey("Fixture"))
            }
        }

    @Test fun cancellationIsPropagatedForAccountAndNotificationRequests() =
        runTest {
            Harness().use { h ->
                h.error = CancellationException("fixture")
                try {
                    h.profile.keys()
                    fail("Cancellation swallowed")
                } catch (_: CancellationException) {
                }
                try {
                    h.notifications.list()
                    fail("Cancellation swallowed")
                } catch (_: CancellationException) {
                }
            }
        }
}
