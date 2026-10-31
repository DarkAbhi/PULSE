package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FuelItem
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.RECEIPT_LIMIT
import dev.abhishekan.pulse.core.domain.model.TirePressure
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.network.GarageApi
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.SessionMaterialStore
import dev.abhishekan.pulse.core.network.pulseHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.Closeable
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class GarageRepositoryTest {
    private class Harness : Closeable {
        val config = NetworkConfig("https://garage.test/api/", false)
        val clock = Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC)
        val store =
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
        val cookies = PersistentSessionCookies(store, config, clock)
        var status = HttpStatusCode.OK
        var redirect = "https://storage.test/receipt?signature=test"
        var storageStatus = HttpStatusCode.OK
        var storageCookie: String? = null
        var downloadBytes = byteArrayOf(1, 2, 3)
        var declaredLength: String? = null
        var multipartType: String? = null
        var multipartHeader = ""
        var body = (
            """{"vehicle_name":"Ride","kind":"motorcycle","fuel_types":["petrol"],""" +
""""air_fills":null,""" +
""""fuel_fillups":null,""" +
""""maintenance_records":null,""" +
""""fuel_efficiency":{"petrol":{"total_cost":123.1234567890123456789,""" +
""""total_volume":2.5,""" +
""""average_km_per_litre":null}}}"""
        )
        var file = ReceiptBytes("receipt.pdf", "application/pdf", byteArrayOf(1, 2, 3))
        var fileFailure = false
        var catalogParameters: Map<String, String> = emptyMap()
        var writeFailure: Exception? = null
        val requests = mutableListOf<Pair<String, String>>()
        val engine =
            MockEngine { request ->
                if (request.url.encodedPath.startsWith("/api/vehicle-catalog/")) {
                    catalogParameters =
                        listOf("kind", "make_slug", "q", "make_name", "model_name")
                            .mapNotNull { key ->
                                request.url.parameters[key]?.let {
                                    key to
                                        it
                                }
                            }.toMap()
                    return@MockEngine respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                }
                if (request.url.encodedPath == "/api/profile") {
                    return@MockEngine respond(
                        """{"has_profile":false,"timezone":"UTC"}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                if (request.url.host == "storage.test") {
                    storageCookie = request.headers[HttpHeaders.Cookie]
                    respond(
                        downloadBytes,
                        storageStatus,
                        Headers.build {
                            append(HttpHeaders.ContentType, "application/pdf")
                            declaredLength?.let { append(HttpHeaders.ContentLength, it) }
                        },
                    )
                } else if (request.url.encodedPath.endsWith("/auth/login")) {
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
                    if (request.method != HttpMethod.Get) writeFailure?.let { throw it }
                    if (request.url.encodedPath.endsWith("/attachments")) {
                        multipartType =
                            request.headers[HttpHeaders.ContentType]
                                ?: request.body.contentType?.toString()
                        val bytes = request.body.toByteArray()
                        multipartHeader = bytes.copyOf(minOf(1024, bytes.size)).decodeToString()
                    }
                    requests +=
                        "${request.method.value} ${request.url.encodedPath}" to
                        ((request.body as? TextContent)?.text ?: "multipart")
                    if (request.method == HttpMethod.Get &&
                        request.url.encodedPath.endsWith("/attachments/3") &&
                        status == HttpStatusCode.OK
                    ) {
                        respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, redirect))
                    } else {
                        respond(
                            if (request.url.encodedPath ==
                                "/api/vehicles"
                            ) {
                                """[{"id":1,"name":"Ride","kind":"motorcycle","fuel_types":["petrol"]}]"""
                            } else {
                                body
                            },
                            status,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
            }
        val client = pulseHttpClient(config, engine, cookies)
        val auth = PulseAuthRepository(PulseApi(client), cookies, clock)
        val files =
            object : ReceiptFiles {
                override suspend fun read(source: String): ReceiptBytes {
                    if (fileFailure) throw ReceiptFileException()
                    return file
                }

                override suspend fun write(
                    bytes: ByteArray,
                    name: String,
                    destination: String?,
                ) = destination ?: "content://preview/receipt"
            }
        val repo = PulseGarageRepository(GarageApi(client), auth, files)

        suspend fun login() {
            auth.restore()
            auth.login("fixture", "fixture-only")
        }

        override fun close() {
            client.close()
        }
    }

    private val receipt = GarageReceipt(3, "receipt.pdf", "application/pdf", 3)

    @Test fun exactBackendSummaryAndMissingHistoryStayHonest() =
        runTest {
            Harness().use { h ->
                val result = (h.repo.history(1) as Outcome.Success).value
                assertEquals("motorcycle", result.vehicleKind)
                assertEquals(listOf("petrol"), result.fuelTypes)
                assertTrue(result.air.isEmpty())
                assertTrue(result.fuel.isEmpty())
                assertNull(result.efficiency.getValue("petrol").average)
                assertEquals(
                    "123.1234567890123456789",
                    result.efficiency
                        .getValue("petrol")
                        .totalCost
                        .toPlainString(),
                )
                assertEquals(listOf(Vehicle(1, "Ride", "motorcycle", listOf("petrol"))), (h.repo.vehicles() as Outcome.Success).value)
            }
        }

    @Test fun manualMatchDecodesCanonicalIdentityAndEncodesNames() =
        runTest {
            Harness().use { h ->
                h.body = """{"match":{"id":"car/honda/civic","make_slug":"honda","make_name":"Honda","model_name":"Civic"}}"""
                val match = (h.repo.match("car", "Honda & Co", "Civic + Type R") as Outcome.Success).value!!
                assertEquals("car/honda/civic", match.id)
                assertEquals(mapOf("kind" to "car", "make_name" to "Honda & Co", "model_name" to "Civic + Type R"), h.catalogParameters)
                h.body = """{"match":null}"""
                assertNull((h.repo.match("car", "Unknown", "Unknown") as Outcome.Success).value)
                h.body = """{"match":{"id":"bus/honda/civic","make_slug":"honda","make_name":"Honda","model_name":"Civic"}}"""
                assertTrue(h.repo.match("car", "Honda", "Civic") is Outcome.Error)
            }
        }

    @Test fun catalogQueriesEncodeFiltersAndUseTheSharedSessionClient() =
        runTest {
            Harness().use { h ->
                h.body = """[{"id":"car/honda/civic","name":"Civic"}]"""
                val options = (h.repo.search("car", "honda", "Civic & Type R") as Outcome.Success).value
                assertEquals("car/honda/civic", options.single().id)
                assertEquals(mapOf("kind" to "car", "make_slug" to "honda", "q" to "Civic & Type R"), h.catalogParameters)
                h.repo.search("bus", null, "")
                assertFalse(h.catalogParameters.containsKey("make_slug"))
            }
        }

    @Test fun unknownLegacyKindAndCatalogIdentityArePreservedInHistory() =
        runTest {
            Harness().use { h ->
                h.body =
                    h.body.replace(
                        "\"kind\":\"motorcycle\"",
                        "\"kind\":\"unknown\",\"catalog_id\":null,\"make_name\":\"Custom\",\"model_name\":\"Old model\"",
                    )
                val history = (h.repo.history(1) as Outcome.Success).value
                assertEquals("unknown", history.vehicleKind)
                assertEquals("Custom", history.makeName)
                assertEquals("Old model", history.modelName)
                assertNull(history.catalogId)
            }
        }

    @Test fun vehicleEditExplicitlyClearsCatalogAndDoesNotReactivateVehicle() =
        runTest {
            Harness().use { h ->
                h.repo.apply(
                    GarageChange.SaveVehicle(
                        "My van",
                        true,
                        TirePressure(),
                        "van",
                        listOf("diesel"),
                        makeName = "Custom",
                        modelName = "Van",
                        vehicle = 42,
                    ),
                )
                val (path, body) = h.requests.last()
                assertEquals("PUT /api/vehicles/42", path)
                val json = Json.parseToJsonElement(body).jsonObject
                assertEquals("", json.getValue("catalog_id").jsonPrimitive.content)
                assertEquals("van", json.getValue("kind").jsonPrimitive.content)
                assertFalse(json.containsKey("is_active"))
                assertFalse(json.containsKey("vehicle_type"))
            }
        }

    @Test fun missingHistoryCollectionsDoNotInventAnEmptyGarage() =
        runTest {
            Harness().use { h ->
                h.body = """{"vehicle_name":"Ride","kind":"motorcycle","fuel_types":["petrol"]}"""
                assertEquals(Outcome.Error(Failure.Malformed), h.repo.history(1))
            }
        }

    @Test fun invalidVehicleDetailsAreNotReplacedWithGuesses() =
        runTest {
            Harness().use { h ->
                val valid = h.body
                for (invalid in listOf(
                    valid.replace("motorcycle", "spaceship"),
                    valid.replace("[\"petrol\"]", "[]"),
                    valid.replace("[\"petrol\"]", "[\"petrol\",\"petrol\"]"),
                    valid.replace("\"kind\":\"motorcycle\",", ""),
                )) {
                    h.body = invalid
                    assertEquals(Outcome.Error(Failure.Malformed), h.repo.history(1))
                }
            }
        }

    @Test fun mutationPathsDecimalPrecisionAndExplicitPressureClears() =
        runTest {
            Harness().use { h ->
                val n = "123.1234567890123456789".toBigDecimal()
                val date = Instant.parse("2026-12-31T23:59:59Z")
                listOf(
                    GarageChange.SaveVehicle(
                        "Ride",
                        true,
                        TirePressure(),
                        "car",
                        listOf("petrol", "cng"),
                    ),
                    GarageChange.Pressure(1, TirePressure(frontSolo = n)),
                    GarageChange.Air(1),
                    GarageChange.Fuel(
                        1,
                        null,
                        n,
                        date,
                        null,
                        null,
                        listOf(FuelItem("petrol", "partial", n, null, n)),
                    ),
                    GarageChange.Fuel(
                        1,
                        2,
                        n,
                        date,
                        null,
                        null,
                        listOf(FuelItem("petrol", "full", n, n, null)),
                    ),
                    GarageChange.Care(1, null, "service", "Care", n, date, null, null, null),
                    GarageChange.Care(1, 2, "repair", "Care", n, date, n, "P", null),
                    GarageChange.Delete(
                        GarageRecord.Receipt,
                        1,
                        3,
                        2,
                    ),
                    GarageChange.Delete(
                        GarageRecord.Maintenance,
                        1,
                        2,
                    ),
                    GarageChange.Delete(GarageRecord.Fuel, 1, 2),
                    GarageChange.Delete(
                        GarageRecord.Air,
                        1,
                        2,
                    ),
                    GarageChange.Delete(GarageRecord.Vehicle, 1),
                ).forEach {
                    assertEquals(Outcome.Success(Unit), h.repo.apply(it))
                }
                val vehicleBody = Json.parseToJsonElement(h.requests.first().second).jsonObject
                assertEquals("car", vehicleBody.getValue("kind").jsonPrimitive.content)
                assertEquals(listOf("petrol", "cng"), vehicleBody.getValue("fuel_types").jsonArray.map { it.jsonPrimitive.content })
                assertEquals(
                    listOf(
                        "POST /api/vehicles",
                        "PUT /api/vehicles/1/tire-pressure",
                        "POST /api/vehicles/1/air-fills",
                        "POST /api/vehicles/1/fuel-fillups",
                        "PUT /api/vehicles/1/fuel-fillups/2",
                        "POST /api/vehicles/1/maintenance-records",
                        "PUT /api/vehicles/1/maintenance-records/2",
                        "DELETE /api/vehicles/1/maintenance-records/2/attachments/3",
                        "DELETE /api/vehicles/1/maintenance-records/2",
                        "DELETE /api/vehicles/1/fuel-fillups/2",
                        "DELETE /api/vehicles/1/air-fills/2",
                        "DELETE /api/vehicles/1",
                    ),
                    h.requests.map {
                        it.first
                    },
                )
                val pressure = Json.parseToJsonElement(h.requests[1].second).jsonObject
                assertEquals(JsonNull, pressure["rear_tire_pressure_solo"])
                assertEquals(
                    n.toPlainString(),
                    pressure["front_tire_pressure_solo"]!!.jsonPrimitive.content,
                )
                val item =
                    Json
                        .parseToJsonElement(
                            h.requests[3].second,
                        ).jsonObject["items"]!!
                        .jsonArray
                        .single()
                        .jsonObject
                assertFalse(item.containsKey("unit_price"))
                assertEquals(n.toPlainString(), item["quantity"]!!.jsonPrimitive.content)
                assertEquals(12L, h.repo.changes.value)
            }
        }

    @Test fun signedDownloadNeverForwardsSessionAcrossOriginsAndStorage401KeepsSession() =
        runTest {
            Harness().use { h ->
                h.login()
                assertTrue(h.cookies.get(Url(h.config.baseUrl)).isNotEmpty())
                assertTrue(h.repo.download(1, 2, receipt, null) is Outcome.Success)
                assertNull(h.storageCookie)
                h.storageStatus = HttpStatusCode.Unauthorized
                assertEquals(Outcome.Error(Failure.Server), h.repo.download(1, 2, receipt, null))
                assertTrue(h.cookies.get(Url(h.config.baseUrl)).isNotEmpty())
            }
        }

    @Test fun insecureMalformedAndChainedStorageRedirectsAreRejected() =
        runTest {
            Harness().use { h ->
                for (url in listOf(
                    "http://storage.test/receipt",
                    "file:///etc/passwd",
                    "https://user:password@storage.test/file",
                    "/relative",
                )) {
                    h.redirect = url
                    assertTrue(h.repo.download(1, 2, receipt, null) is Outcome.Error)
                }
                h.redirect = "https://storage.test/file"
                h.storageStatus = HttpStatusCode.Found
                assertEquals(Outcome.Error(Failure.Server), h.repo.download(1, 2, receipt, null))
            }
        }

    @Test fun missingOrOversizedUploadsAndUnconfiguredStorageHaveSafeFailures() =
        runTest {
            Harness().use { h ->
                h.fileFailure = true
                assertEquals(Outcome.Error(Failure.InvalidInput), h.repo.upload(1, 2, "content://x") {})
                h.fileFailure = false
                h.file = h.file.copy(bytes = ByteArray(RECEIPT_LIMIT + 1))
                assertTrue(h.repo.upload(1, 2, "content://x") {} is Outcome.Error)
                assertTrue(h.requests.isEmpty())
                h.file = h.file.copy(bytes = byteArrayOf(1))
                h.status =
                    HttpStatusCode.InternalServerError
                assertEquals(Outcome.Error(Failure.Server), h.repo.upload(1, 2, "content://x") {})
                assertTrue(
                    h.requests
                        .single()
                        .first
                        .endsWith("/maintenance-records/2/attachments"),
                )
            }
        }

    @Test fun cancellationPropagatesAndFailedWritesInvalidateObservers() =
        runTest {
            Harness().use { h ->
                h.writeFailure = SocketTimeoutException()
                assertEquals(Outcome.Error(Failure.Timeout), h.repo.apply(GarageChange.Air(1)))
                assertEquals(1L, h.repo.changes.value)
                h.writeFailure = CancellationException()
                try {
                    h.repo.apply(GarageChange.Air(1))
                    fail("Cancellation swallowed")
                } catch (
                    _: CancellationException,
                ) {
                }
                assertEquals(1L, h.repo.changes.value)
            }
        }

    @Test fun oversizedDeclaredAndStreamedDownloadsAreRejected() =
        runTest {
            Harness().use { h ->
                h.declaredLength = (RECEIPT_LIMIT + 1).toString()
                assertEquals(Outcome.Error(Failure.Server), h.repo.download(1, 2, receipt, null))
                h.declaredLength = null
                h.downloadBytes = ByteArray(RECEIPT_LIMIT + 1)
                assertEquals(Outcome.Error(Failure.Server), h.repo.download(1, 2, receipt, null))
            }
        }

    @Test fun successfulUploadUsesMultipartAndExactLimitIsAccepted() =
        runTest {
            Harness().use { h ->
                h.file = h.file.copy(bytes = ByteArray(RECEIPT_LIMIT))
                assertEquals(Outcome.Success(Unit), h.repo.upload(1, 2, "content://fixture") {})
                assertTrue(h.multipartType!!.startsWith("multipart/form-data"))
                val disposition =
                    h.multipartHeader.lineSequence().first {
                        it.startsWith("Content-Disposition:", true)
                    }
                assertTrue(
                    disposition,
                    disposition.contains("name=file") || disposition.contains("name=\"file\""),
                )
                assertTrue(
                    disposition,
                    disposition.contains("filename=receipt.pdf") ||
                        disposition.contains("filename=\"receipt.pdf\""),
                )
                assertFalse(disposition, disposition.contains("; file;"))
            }
        }

    @Test fun pulseApi401ExpiresSessionWhileMalformedHistoryIsSafe() =
        runTest {
            Harness().use { h ->
                h.login()
                h.body =
                    """{"vehicle_name":"Ride","kind":"motorcycle","fuel_types":["petrol"],"fuel_fillups":[{"id":2,"odometer_km":-1,"filled_at":"bad-date","items":[]}]}"""
                assertEquals(Outcome.Error(Failure.Malformed), h.repo.history(1))
                h.status = HttpStatusCode.Unauthorized
                assertEquals(Outcome.Error(Failure.Unauthorized), h.repo.download(1, 2, receipt, null))
                assertTrue(h.cookies.get(Url(h.config.baseUrl)).isEmpty())
            }
        }

    @Test fun filenamesCannotEscapePrivateCacheOrInjectMultipartHeaders() {
        assertEquals(240, safeReceiptName("🙂".repeat(80)).toByteArray(Charsets.UTF_8).size)
        assertEquals("bill.pdf", safeReceiptName("../../bill.pdf"))
        assertEquals("receipt", safeReceiptName(".."))
        assertEquals("badname.pdf", safeReceiptName("bad\r\n\";name.pdf"))
    }
}
