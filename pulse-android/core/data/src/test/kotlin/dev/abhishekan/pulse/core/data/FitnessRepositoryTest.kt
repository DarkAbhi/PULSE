package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.domain.model.ExerciseInput
import dev.abhishekan.pulse.core.domain.model.ExerciseSetInput
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.network.FitnessApi
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.SessionMaterialStore
import dev.abhishekan.pulse.core.network.pulseHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.Closeable
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class FitnessRepositoryTest {
    private class Harness : Closeable {
        private val store =
            object : SessionMaterialStore {
                var bytes: ByteArray? = null

                override suspend fun read() = bytes

                override suspend fun write(material: ByteArray) {
                    bytes = material
                }

                override suspend fun clear() {
                    bytes = null
                }
            }
        private val config = NetworkConfig("https://fitness.example.test/api/", false)
        private val clock = Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC)
        private val cookies = PersistentSessionCookies(store, config, clock)
        var status = HttpStatusCode.OK
        var failure: Exception? = null
        var badDate = false
        var query = ""
        var history: String? = null
        val writes = mutableListOf<Pair<String, String>>()
        val engine =
            MockEngine { request ->
                if (request.url.encodedPath == "/api/profile") {
                    return@MockEngine respond(
                        """{"has_profile":false,"timezone":"UTC"}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                val path = request.url.encodedPath
                if (path != "/api/auth/login") failure?.let { throw it }
                val mutation = request.method != HttpMethod.Get && path != "/api/auth/login"
                if (mutation) {
                    writes +=
                        "${request.method.value} $path" to ((request.body as? TextContent)?.text ?: "")
                }
                val body =
                    when {
                        path == "/api/auth/login" -> """{"username":"fixture-user"}"""
                        path == "/api/fitness/overview" ->
                            if (badDate) {
                                """{"today":"not-a-date"}"""
                            } else {
                                (
                                    """{"today":"2026-10-17",""" +
""""cards":[{"id":"sets",""" +
""""label":"Sets",""" +
""""value":"74",""" +
""""subtitle":"from server"}],""" +
""""calendar_workouts":{"2026-10-17":{"date":"2026-10-17",""" +
""""workouts":[{"id":1,""" +
""""time_label":"12:00 AM"},""" +
"""{"id":99,""" +
""""time_label":"stale deleted record"}]}}}"""
                                )
                            }
                        path == "/api/gym-visits" && !mutation ->
                            history
                                ?: """[{"id":2,"created_at":"2026-10-18T18:30:00Z"},{"id":1,"created_at":"2026-10-16T18:30:00Z"}]"""
                        path == "/api/fitness-workouts/1" -> (
                            """{"id":1,""" +
""""created_at":"2026-10-16T18:30:00Z",""" +
""""start_time":"2026-10-17T01:00:00Z",""" +
""""end_time":null,""" +
""""duration_seconds":1800.125,""" +
""""calories_burned":null,""" +
""""synced_from_apple_watch":true}"""
                        )
                        path == "/api/gym-visits/1/exercises" && !mutation -> (
                            """[{"id":5,""" +
""""name":"Squat",""" +
""""exercise_catalog_id":"catalog-1",""" +
""""catalog_exercise":{"id":"catalog-1",""" +
""""name":"Squat",""" +
""""equipment":null,""" +
""""data":{"primaryMuscles":["quadriceps"],""" +
""""instructions":["Stand tall."]}},""" +
""""sets":[{"set_number":1,""" +
""""reps":12,""" +
""""weight":12.1234567890123456789},""" +
"""{"set_number":2,""" +
""""reps":8,""" +
""""weight":null}]}]"""
                        )
                        path == "/api/exercise-catalog" -> {
                            query = request.url.parameters["q"].orEmpty()
                            """{"candidates":null}"""
                        }
                        mutation -> """{"id":3}"""
                        else -> error("Unexpected fixture path $path")
                    }
                respond(
                    body,
                    if (path ==
                        "/api/auth/login"
                    ) {
                        HttpStatusCode.OK
                    } else {
                        status
                    },
                    Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                        if (path ==
                            "/api/auth/login"
                        ) {
                            append(
                                HttpHeaders.SetCookie,
                                "life_session=test-fixture; Path=/; Max-Age=3600; Secure; HttpOnly",
                            )
                        }
                    },
                )
            }
        val client = pulseHttpClient(config, engine, cookies)
        val auth = PulseAuthRepository(PulseApi(client), cookies, clock)
        val repo = PulseFitnessRepository(FitnessApi(client), auth)

        suspend fun login() {
            auth.restore()
            assertEquals(Outcome.Success(Unit), auth.login("fixture-user", "test-only"))
        }

        override fun close() {
            client.close()
        }
    }

    @Test fun calendarUsesUtcByDefaultKeepsFutureVisitsAndDropsDeletedOverviewRows() =
        runTest {
            Harness().use { h ->
                val data = (h.repo.overview() as Outcome.Success).value
                assertEquals(LocalDate.of(2026, 10, 17), data.today)
                assertEquals("74", data.metrics.single().value)
                assertEquals(setOf(1L, 2L), data.workouts.map { it.id }.toSet())
                assertEquals(LocalDate.of(2026, 10, 18), data.workouts.first().date)
                assertEquals(LocalDate.of(2026, 10, 16), data.workouts.last().date)
            }
        }

    @Test fun importedDetailsAndOptionalFieldsPreserveExactNumericValues() =
        runTest {
            Harness().use { h ->
                val detail = (h.repo.workout(1) as Outcome.Success).value
                assertTrue(detail.info.imported)
                assertEquals("1800.125", detail.info.durationSeconds?.toPlainString())
                assertNull(detail.info.calories)
                assertEquals(
                    "12.1234567890123456789",
                    detail.exercises
                        .single()
                        .sets
                        .first()
                        .weight
                        ?.toPlainString(),
                )
                assertNull(
                    detail.exercises
                        .single()
                        .sets
                        .last()
                        .weight,
                )
                assertEquals(
                    listOf("Stand tall."),
                    detail.exercises
                        .single()
                        .catalog
                        ?.instructions,
                )
            }
        }

    @Test fun exerciseWritesUseSupportedFieldsExplicitNullAndExactDecimals() =
        runTest {
            Harness().use { h ->
                val input =
                    ExerciseInput(
                        "Custom squat",
                        listOf(ExerciseSetInput(8, "100.1234567890123456789".toBigDecimal())),
                        null,
                        false,
                    )
                h.repo.saveExercise(1, null, input)
                h.repo.saveExercise(1, 5, input)
                h.repo.deleteExercise(1, 5)
                h.repo.deleteWorkout(1)
                assertEquals(
                    listOf(
                        "POST /api/gym-visits/1/exercises",
                        "PUT /api/gym-visits/1/exercises/5",
                        "DELETE /api/gym-visits/1/exercises/5",
                        "DELETE /api/gym-visits/1",
                    ),
                    h.writes.map {
                        it.first
                    },
                )
                val json = Json.parseToJsonElement(h.writes.first().second).jsonObject
                assertEquals(JsonNull, json["exercise_catalog_id"])
                assertEquals(JsonPrimitive(false), json["remember_alias"])
                assertEquals(
                    "100.1234567890123456789",
                    json["sets"]!!
                        .jsonArray
                        .single()
                        .jsonObject["weight"]!!
                        .jsonPrimitive.content,
                )
                assertEquals(4L, h.repo.changes.value)
            }
        }

    @Test fun todayAndChosenDateUseDifferentEndpointsAndSearchIsEncoded() =
        runTest {
            Harness().use { h ->
                h.repo.record(null)
                h.repo.record(LocalDate.of(2024, 2, 29))
                assertEquals("POST /api/workout/today", h.writes[0].first)
                assertEquals("POST /api/gym-visits", h.writes[1].first)
                assertEquals(
                    "2024-02-29",
                    Json
                        .parseToJsonElement(
                            h.writes[1].second,
                        ).jsonObject["date"]!!
                        .jsonPrimitive.content,
                )
                assertEquals(
                    Outcome.Success(emptyList<CatalogExercise>()),
                    h.repo.search("press & pull+"),
                )
                assertEquals("press & pull+", h.query)
            }
        }

    @Test fun failedMutationInvalidatesObserversButIsNeverAutomaticallyRetried() =
        runTest {
            Harness().use { h ->
                h.status = HttpStatusCode.ServiceUnavailable
                assertEquals(Outcome.Error(Failure.Server), h.repo.record(null))
                assertEquals(1, h.writes.size)
                assertEquals(1L, h.repo.changes.value)
                h.failure = SocketTimeoutException("private details")
                assertEquals(Outcome.Error(Failure.Timeout), h.repo.deleteWorkout(1))
                h.failure = IOException("private details")
                assertEquals(Outcome.Error(Failure.Connection), h.repo.deleteExercise(1, 2))
            }
        }

    @Test fun malformedNotFoundAndValidationResponsesStayDomainFailures() =
        runTest {
            Harness().use { h ->
                h.badDate = true
                assertEquals(Outcome.Error(Failure.Malformed), h.repo.overview())
                h.status = HttpStatusCode.NotFound
                assertEquals(Outcome.Error(Failure.NotFound), h.repo.workout(1))
                h.status = HttpStatusCode.BadRequest
                assertEquals(Outcome.Error(Failure.InvalidInput), h.repo.record(null))
            }
        }

    @Test fun protectedRequestExpiryClearsAuthAndCancellationPropagates() =
        runTest {
            Harness().use { h ->
                h.login()
                h.status = HttpStatusCode.Unauthorized
                assertEquals(Outcome.Error(Failure.Unauthorized), h.repo.workout(1))
                assertTrue(h.auth.session.value is SessionState.SignedOut)
                h.failure = CancellationException("Cancelled")
                try {
                    h.repo.record(null)
                    fail("Cancellation must propagate")
                } catch (
                    _: CancellationException,
                ) {
                }
                assertEquals(0L, h.repo.changes.value)
            }
        }

    @Test fun historyOrderComesFromBackendRatherThanRecordIdentifiers() =
        runTest {
            Harness().use { h ->
                h.history =
                    """[{"id":10,"created_at":"2026-10-17T08:00:00Z"},{"id":20,"created_at":"2026-10-17T02:00:00Z"}]"""
                val overview = (h.repo.overview() as Outcome.Success).value
                assertEquals(listOf(10L, 20L), overview.workouts.map { it.id })
            }
        }
}
