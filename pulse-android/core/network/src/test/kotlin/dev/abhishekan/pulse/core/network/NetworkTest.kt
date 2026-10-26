package dev.abhishekan.pulse.core.network
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class NetworkTest {
    @Test fun debugLogsHeadersAndBodiesButRedactsCredentialAndSignedLocationHeaders() =
        runTest {
            val messages = CopyOnWriteArrayList<String>()
            val logger: (String) -> Unit = { messages += it }
            val engine =
                MockEngine {
                    respond(
                        "response-secret",
                        headers =
                            headersOf(
                                HttpHeaders.SetCookie to listOf("session=cookie-secret"),
                                HttpHeaders.ContentType to listOf("text/plain"),
                                "X-Response-Probe" to listOf("response-header-value"),
                                HttpHeaders.Location to listOf("https://storage.test/file?signature=secret"),
                            ),
                    )
                }
            pulseHttpClient(NetworkConfig("https://example.com/api/", true), engine, logger = logger).use { client ->
                client
                    .post("auth/login") {
                        header("X-Request-Probe", "request-header-value")
                        header(HttpHeaders.Authorization, "Bearer auth-secret")
                        header(HttpHeaders.Cookie, "session=request-cookie-secret")
                        setBody("password-secret")
                    }.body<String>()
            }
            val logs = messages.joinToString("\n")
            assertTrue(logs, logs.contains("POST"))
            assertTrue(logs, logs.contains("https://example.com/api/auth/login"))
            assertTrue(logs, logs.contains("200"))
            assertTrue(logs, logs.contains("request-header-value"))
            assertTrue(logs, logs.contains("response-header-value"))
            assertTrue(logs, logs.contains("password-secret"))
            assertTrue(logs, logs.contains("response-secret"))
            listOf("auth-secret", "cookie-secret", "signature=secret").forEach {
                assertTrue(logs, !logs.contains(it))
            }
        }

    @Test fun releaseAndExternalOriginsProduceNoLogs() =
        runTest {
            val messages = CopyOnWriteArrayList<String>()
            val logger: (String) -> Unit = { messages += it }
            listOf(false, true).forEach { debug ->
                val engine = MockEngine { respond("{}") }
                pulseHttpClient(NetworkConfig("https://example.com/api/", debug), engine, logger = logger).use { client ->
                    if (!debug) client.get("profile")
                    client.get("https://storage.test/file?signature=secret")
                    client.get("https://example.com:8443/file?signature=secret")
                    client.get("http://example.com/file?signature=secret")
                }
            }
            assertTrue(messages.toString(), messages.isEmpty())
        }

    @Serializable private data class Probe(
        val label: String,
        val detail: String? = null,
    )

    @Test fun jsonAcceptsOptionalAndUnknownFieldsButRejectsMissingRequiredData() =
        runTest {
            var response = """{"label":"sample","extra":true}"""
            val engine =
                MockEngine {
                    respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
            pulseHttpClient(NetworkConfig("https://example.com/api/", false), engine).use { client ->
                assertEquals(Probe("sample"), client.get("probe").body<Probe>())
                response = "{}"
                try {
                    client.get("probe").body<Probe>()
                    fail("Expected malformed response failure")
                } catch (
                    _: JsonConvertException,
                ) {
                }
            }
        }

    @Test fun releaseRejectsInsecureAndMalformedAddresses() {
        listOf(
            "http://example.com/api/",
            "http://10.0.2.2/api/",
            "http://100.115.134.90/api/",
            "http://100.115.134.90:18080/api/",
            "http://100.115.134.91:3100/api/",
            "https://user:secret@example.com/api/",
            "https://example.com/",
            "https://example.com/api/?key=secret",
        ).forEach {
            assertThrows(IllegalArgumentException::class.java) { NetworkConfig(it, false) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            NetworkConfig("http://localhost/api/", true)
        }
        NetworkConfig("https://example.com/api/", false)
        NetworkConfig("http://10.0.2.2:18081/api/", true)
    }

    @Test fun releaseAllowsOnlyTheConfiguredTailnetHttpEndpoint() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals("http://100.115.134.90:3100/api/profile", request.url.toString())
                    respond("{}")
                }
            pulseHttpClient(NetworkConfig("http://100.115.134.90:3100/api/", false), engine).use {
                it.get("profile")
            }
        }

    @Test fun relativePathsUseApiBaseAndRequestsCarryNoCredentials() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals("https://example.com/api/profile", request.url.toString())
                    assertNull(request.headers["Authorization"])
                    assertNull(request.headers["Cookie"])
                    respond("{}")
                }
            pulseHttpClient(NetworkConfig("https://example.com/api/", false), engine).use {
                it.get("profile")
            }
        }

    @Test fun serverFailureIsNotRetried() =
        runTest {
            var attempts = 0
            val engine =
                MockEngine {
                    attempts++
                    respond("{}", HttpStatusCode.InternalServerError)
                }
            pulseHttpClient(NetworkConfig("https://example.com/api/", false), engine).use { client ->
                try {
                    client.get("profile")
                    fail("Expected server failure")
                } catch (
                    _: ServerResponseException,
                ) {
                }
            }
            assertEquals(1, attempts)
        }

    @Test fun cancellationPropagates() =
        runTest {
            val engine = MockEngine { throw CancellationException("Cancelled") }
            pulseHttpClient(NetworkConfig("https://example.com/api/", false), engine).use { client ->
                try {
                    client.get("profile")
                    fail("Expected cancellation")
                } catch (
                    _: CancellationException,
                ) {
                }
            }
        }
}
