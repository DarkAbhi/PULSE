package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealChange
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.network.MealPlanApi
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
import io.ktor.http.cookies
import io.ktor.http.date
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.Closeable
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class MealPlanRepositoryTest {
    private class Harness : Closeable {
        var response = "[]"
        var status = HttpStatusCode.OK
        var thrown: Exception? = null
        val requests = mutableListOf<Pair<String, String>>()
        val store =
            object : SessionMaterialStore {
                var value: ByteArray? = null

                override suspend fun read() = value

                override suspend fun write(material: ByteArray) {
                    value = material
                }

                override suspend fun clear() {
                    value = null
                }
            }
        val clock = Clock.fixed(Instant.parse("2026-12-31T20:00:00Z"), ZoneOffset.UTC)
        val config = NetworkConfig("https://meals.test/api/", false)
        val cookies = PersistentSessionCookies(store, config, clock)
        val client =
            pulseHttpClient(
                config,
                MockEngine { request ->
                    if (request.url.encodedPath.endsWith("auth/login")) {
                        respond(
                            """{"username":"fixture"}""",
                            HttpStatusCode.OK,
                            headersOf(
                                HttpHeaders.ContentType to listOf("application/json"),
                                HttpHeaders.SetCookie to
                                    listOf("life_session=fixture; Path=/; Max-Age=3600; Secure"),
                            ),
                        )
                    } else {
                        thrown?.let { throw it }
                        requests +=
                            "${request.method.value} ${request.url}" to
                            ((request.body as? TextContent)?.text ?: "")
                        respond(
                            response,
                            status,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                },
                cookies,
            )
        val auth = PulseAuthRepository(PulseApi(client), cookies, clock)
        val repo = PulseMealPlanRepository(MealPlanApi(client), auth)

        suspend fun login() {
            auth.restore()
            auth.login("fixture", "fixture-only")
        }

        override fun close() {
            client.close()
        }
    }

    private val start = LocalDate.parse("2026-12-28")
    private val json = """{"id":1,"date":"2027-01-01","name":"Rice","is_consumed":false}"""

    @Test fun rangeQueryAndOptionalTimesPreserveCalendarDate() =
        runTest {
            Harness().use { h ->
                h.response = "[$json]"
                val meal = (h.repo.meals(start, start.plusDays(6)) as Outcome.Success).value.single()
                assertEquals(LocalDate.parse("2027-01-01"), meal.date)
                assertNull(meal.timeId)
                assertNull(meal.start)
                assertTrue(
                    h.requests
                        .single()
                        .first
                        .contains("start_date=2026-12-28&end_date=2027-01-03"),
                )
            }
        }

    @Test fun mutationsUseSupportedPathsAndOmitUnclearableTimeRelationship() =
        runTest {
            Harness().use { h ->
                listOf(
                    MealChange.Save(null, start, " Rice ", null),
                    MealChange.Save(1, start, "Soup", 2),
                    MealChange.Delete(1),
                    MealChange.CreateTime(" Late snack ", LocalTime.of(23, 0), LocalTime.of(1, 0)),
                    MealChange.DeleteTime(MealTime(9, "Snack", LocalTime.NOON, LocalTime.NOON, false)),
                ).forEach {
                    assertEquals(Outcome.Success(Unit), h.repo.apply(it))
                }
                assertEquals(
                    listOf("POST", "PATCH", "DELETE", "POST", "DELETE"),
                    h.requests.map {
                        it.first.substringBefore(' ')
                    },
                )
                val first = Json.parseToJsonElement(h.requests[0].second).jsonObject
                assertFalse(first.containsKey("meal_time_id"))
                assertFalse(first.containsKey("is_consumed"))
                assertEquals("Rice", first.getValue("name").jsonPrimitive.content)
                assertEquals(
                    2,
                    Json
                        .parseToJsonElement(
                            h.requests[1].second,
                        ).jsonObject
                        .getValue("meal_time_id")
                        .jsonPrimitive.int,
                )
                assertTrue(
                    h.requests
                        .last()
                        .first
                        .endsWith("/meal-times/9"),
                )
                assertEquals(5L, h.repo.changes.value)
            }
        }

    @Test fun malformedMissingFieldsOutOfRangeDatesAndBadTimesFailSafely() =
        runTest {
            Harness().use { h ->
                listOf(
                    """[{"id":1,"date":"2027-01-01","name":"Rice"}]""",
                    """[{"id":1,"date":"2027-02-30","name":"Rice","is_consumed":false}]""",
                    """[{"id":1,"date":"2028-01-01","name":"Rice","is_consumed":false}]""",
                ).forEach { body ->
                    h.response =
                        body
                    assertEquals(
                        Outcome.Error(Failure.Malformed),
                        h.repo.meals(start, start.plusDays(6)),
                    )
                }
                h.response =
                    """[{"id":2,"name":"Lunch","start_time":"25:00","end_time":"14:00","is_default":true}]"""
                assertEquals(Outcome.Error(Failure.Malformed), h.repo.times())
            }
        }

    @Test fun consumedUsesAuthoritativeResponseAndRejectsMismatchedRecord() =
        runTest {
            Harness().use { h ->
                h.response = json.replace("false", "true")
                assertTrue((h.repo.consumed(1, true) as Outcome.Success).value.consumed)
                assertTrue(
                    h.requests.single().first.contains(
                        "PATCH https://meals.test/api/meal-plans/1/consumed",
                    ),
                )
                assertTrue(
                    h.requests
                        .single()
                        .second
                        .contains("true"),
                )
                assertEquals(Outcome.Error(Failure.Malformed), h.repo.consumed(2, true))
            }
        }

    @Test fun failuresExpireOnlyConfirmedSessionAndPreserveCancellation() =
        runTest {
            Harness().use { h ->
                h.login()
                h.thrown = IOException()
                assertEquals(Outcome.Error(Failure.Connection), h.repo.times())
                assertTrue(h.auth.session.value is SessionState.Authenticated)
                h.thrown = null
                h.status = HttpStatusCode.Unauthorized
                assertEquals(Outcome.Error(Failure.Unauthorized), h.repo.times())
                assertTrue(h.auth.session.value is SessionState.SignedOut)
                h.thrown = CancellationException("cancel")
                try {
                    h.repo.apply(MealChange.Delete(1))
                    fail("Cancellation swallowed")
                } catch (
                    _: CancellationException,
                ) {
                }
                assertEquals(0L, h.repo.changes.value)
            }
        }

    @Test fun deletedTimesKeepMealsAndSnapshotTimesWithNoAssociation() =
        runTest {
            Harness().use { h ->
                h.response =
                    """[{"id":1,"date":"2027-01-01","name":"Rice","start_time":"23:00","end_time":"01:00","is_consumed":false}]"""
                val meal = (h.repo.meals(start, start.plusDays(6)) as Outcome.Success).value.single()
                assertNull(meal.timeName)
                assertNull(meal.timeId)
                assertEquals(LocalTime.of(23, 0), meal.start)
                h.status = HttpStatusCode.InternalServerError
                assertEquals(Outcome.Error(Failure.Server), h.repo.apply(MealChange.Delete(1)))
                assertEquals(1L, h.repo.changes.value)
            }
        }
}
