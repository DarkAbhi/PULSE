package dev.abhishekan.pulse.core.network

import io.ktor.client.call.body
import io.ktor.serialization.JsonConvertException
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NetworkTest {
    @Serializable private data class Probe(val label: String, val detail: String? = null)

    @Test fun jsonAcceptsOptionalAndUnknownFieldsButRejectsMissingRequiredData() = runTest {
        var response = """{"label":"sample","extra":true}"""
        val engine = MockEngine { respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json")) }
        pulseHttpClient(NetworkConfig("https://example.com/api/", false), engine).use { client ->
            assertEquals(Probe("sample"), client.get("probe").body<Probe>())
            response = "{}"
            try { client.get("probe").body<Probe>(); fail("Expected malformed response failure") }
            catch (_: JsonConvertException) { }
        }
    }

    @Test fun releaseRejectsInsecureAndMalformedAddresses() {
        listOf("http://example.com/api/", "http://10.0.2.2/api/", "https://user:secret@example.com/api/",
            "https://example.com/", "https://example.com/api/?key=secret").forEach {
            assertThrows(IllegalArgumentException::class.java) { NetworkConfig(it, false) }
        }
        assertThrows(IllegalArgumentException::class.java) { NetworkConfig("http://localhost/api/", true) }
        NetworkConfig("https://example.com/api/", false)
        NetworkConfig("http://10.0.2.2:18081/api/", true)
    }

    @Test fun relativePathsUseApiBaseAndRequestsCarryNoCredentials() = runTest {
        val engine = MockEngine { request ->
            assertEquals("https://example.com/api/profile", request.url.toString())
            assertNull(request.headers["Authorization"])
            assertNull(request.headers["Cookie"])
            respond("{}")
        }
        pulseHttpClient(NetworkConfig("https://example.com/api/", false), engine).use { it.get("profile") }
    }

    @Test fun serverFailureIsNotRetried() = runTest {
        var attempts = 0
        val engine = MockEngine { attempts++; respond("{}", HttpStatusCode.InternalServerError) }
        pulseHttpClient(NetworkConfig("https://example.com/api/", false), engine).use { client ->
            try { client.get("profile"); fail("Expected server failure") } catch (_: ServerResponseException) { }
        }
        assertEquals(1, attempts)
    }

    @Test fun cancellationPropagates() = runTest {
        val engine = MockEngine { throw CancellationException("Cancelled") }
        pulseHttpClient(NetworkConfig("https://example.com/api/", false), engine).use { client ->
            try { client.get("profile"); fail("Expected cancellation") } catch (_: CancellationException) { }
        }
    }
}
