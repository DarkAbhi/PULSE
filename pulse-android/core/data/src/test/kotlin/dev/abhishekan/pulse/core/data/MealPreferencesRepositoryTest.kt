package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealOnboardingStatus
import dev.abhishekan.pulse.core.domain.model.MealPreferences
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.network.MealPlanApi
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.SessionMaterialStore
import dev.abhishekan.pulse.core.network.pulseHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Closeable
import java.time.Clock

class MealPreferencesRepositoryTest {
    private val complete =
        """
        {"status":"completed","questionnaire_version":1,"decided_at":"2026-11-06T00:00:00Z",
        "preferences":{"goal":"feel_satisfied","barriers":[],"dietary_pattern":"vegetarian",
        "food_avoidances":["peanuts"],"meals":[{"kind":"lunch","usual_time":"12:30"}]}}
        """.trimIndent()

    private class Harness : Closeable {
        var response = """{"status":"pending","questionnaire_version":1,"preferences":null}"""
        var status = HttpStatusCode.OK
        var thrown: Exception? = null
        var hold: CompletableDeferred<Unit>? = null
        var requested: CompletableDeferred<Unit>? = null
        val bodies = mutableListOf<String>()
        val paths = mutableListOf<String>()
        var authorization: String? = null
        var cache: String? = null
        private val store =
            object : SessionMaterialStore {
                private var value: ByteArray? = null

                override suspend fun read() = value

                override suspend fun write(material: ByteArray) {
                    value = material
                }

                override suspend fun clear() {
                    value = null
                }
            }
        private val clock = Clock.systemUTC()
        private val config = NetworkConfig("https://preferences.test/api/", false)
        private val cookies = PersistentSessionCookies(store, config, clock)
        private val client =
            pulseHttpClient(
                config,
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/api/auth/login" ->
                            respond(
                                """{"username":"fixture"}""",
                                HttpStatusCode.OK,
                                headersOf(
                                    HttpHeaders.ContentType to listOf("application/json"),
                                    HttpHeaders.SetCookie to listOf("life_session=fixture; Path=/; Max-Age=3600; Secure"),
                                ),
                            )
                        "/api/profile" ->
                            respond(
                                """{"has_profile":false,"timezone":"UTC","currency":"INR"}""",
                                HttpStatusCode.OK,
                                headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        "/api/auth/logout" -> respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                        else -> {
                            paths += "${request.method.value} ${request.url.encodedPath}"
                            bodies += ((request.body as? TextContent)?.text ?: "")
                            authorization = request.headers[HttpHeaders.Authorization]
                            cache = request.headers[HttpHeaders.CacheControl]
                            requested?.complete(Unit)
                            hold?.await()
                            thrown?.let { throw it }
                            respond(response, status, headersOf(HttpHeaders.ContentType, "application/json"))
                        }
                    }
                },
                cookies,
            )
        val auth = PulseAuthRepository(PulseApi(client), cookies, clock)
        val repo = PulseMealPreferencesRepository(MealPlanApi(client), auth)

        override fun close() {
            client.close()
        }
    }

    @Test fun webCompletedSkippedAndPendingStatesUseSharedAuthenticatedEndpoint() =
        runTest {
            Harness().use { h ->
                h.auth.restore()
                h.auth.login("fixture", "test-only")
                h.response = complete
                val state = (h.repo.onboarding() as Outcome.Success).value
                assertEquals(MealOnboardingStatus.Completed, state.status)
                assertEquals(listOf("peanuts"), state.preferences?.foodAvoidances)
                assertEquals(
                    "12:30",
                    state.preferences
                        ?.meals
                        ?.single()
                        ?.time
                        .toString(),
                )
                assertEquals("GET /api/meal-plan-onboarding", h.paths.single())
                assertEquals("Bearer fixture", h.authorization)
                assertEquals("no-store", h.cache)
                h.response = """{"status":"skipped","questionnaire_version":1,"preferences":null}"""
                assertEquals(MealOnboardingStatus.Skipped, (h.repo.onboarding() as Outcome.Success).value.status)
                h.response = """{"status":"pending","questionnaire_version":1,"preferences":null}"""
                assertEquals(MealOnboardingStatus.Pending, (h.repo.onboarding() as Outcome.Success).value.status)
            }
        }

    @Test fun androidWritesUseExactlyTheWebContractAndNeverSendOwnerOrPlatformFlags() =
        runTest {
            Harness().use { h ->
                h.response = complete
                val preferences = MealPreferences(goal = "feel_satisfied", dietaryPattern = "vegetarian")
                h.repo.resolve(preferences)
                val body = Json.parseToJsonElement(h.bodies.last()).jsonObject
                assertEquals("PUT /api/meal-plan-onboarding", h.paths.last())
                assertEquals(1, body.getValue("questionnaire_version").jsonPrimitive.int)
                assertEquals("completed", body.getValue("status").jsonPrimitive.content)
                assertFalse("user_id" in body)
                assertFalse("platform" in body)
                val data = body.getValue("preferences").jsonObject
                assertEquals("vegetarian", data.getValue("dietary_pattern").jsonPrimitive.content)
                assertEquals("[]", data.getValue("barriers").toString())
                h.response = """{"status":"skipped","questionnaire_version":1,"preferences":null}"""
                h.repo.resolve(null)
                val skip = Json.parseToJsonElement(h.bodies.last()).jsonObject
                assertEquals("skipped", skip.getValue("status").jsonPrimitive.content)
                assertTrue(skip["preferences"] == null || skip["preferences"] == JsonNull)
                h.response = complete
                h.repo.save(preferences)
                assertEquals("PUT /api/meal-preferences", h.paths.last())
                assertNull(Json.parseToJsonElement(h.bodies.last()).jsonObject["status"])
            }
        }

    @Test fun conflictsReturnTheAuthoritativeOtherDeviceDecision() =
        runTest {
            Harness().use { h ->
                h.status = HttpStatusCode.Conflict
                h.response = """{"error":"already finished","state":$complete}"""
                assertEquals(MealOnboardingStatus.Completed, (h.repo.resolve(null) as Outcome.Success).value.status)
            }
        }

    @Test fun malformedResponsesAndInvalidLocalAnswersNeverSuppressSetup() =
        runTest {
            Harness().use { h ->
                for (raw in listOf(
                    "{}",
                    complete.replace("completed", "unknown"),
                    complete.replace("12:30", "24:00"),
                    complete.replace("vegetarian", "unknown"),
                )) {
                    h.response = raw
                    assertTrue(h.repo.onboarding() is Outcome.Error)
                }
                val reads = h.paths.size
                assertEquals(Outcome.Error(Failure.InvalidInput), h.repo.resolve(MealPreferences()))
                assertEquals(reads, h.paths.size)
            }
        }

    @Test fun cancellationIsPropagatedAndStaleAccountResponseIsRejected() =
        runTest {
            Harness().use { h ->
                h.thrown = CancellationException("cancel")
                try {
                    h.repo.onboarding()
                    throw AssertionError("cancellation swallowed")
                } catch (_: CancellationException) {
                }
                h.thrown = null
                h.response = complete
                h.auth.restore()
                h.auth.login("fixture", "test-only")
                h.hold = CompletableDeferred()
                h.requested = CompletableDeferred()
                val late = async { h.repo.onboarding() }
                h.requested!!.await()
                h.auth.logout()
                h.auth.login("other", "test-only")
                h.hold!!.complete(Unit)
                assertEquals(Outcome.Error(Failure.Unauthorized), late.await())
            }
        }
}
