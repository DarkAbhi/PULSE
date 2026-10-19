package dev.abhishekan.pulse
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.data.AndroidReceiptFiles
import dev.abhishekan.pulse.core.data.EncryptedSessionStore
import dev.abhishekan.pulse.core.data.PulseAuthRepository
import dev.abhishekan.pulse.core.data.PulseDashboardRepository
import dev.abhishekan.pulse.core.data.PulseGarageRepository
import dev.abhishekan.pulse.core.data.di.dataModule
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.domain.repository.GarageRepository
import dev.abhishekan.pulse.core.network.GarageApi
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.pulseHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.io.IOException
import java.time.Clock

/** Test-only HTTP fixtures use production session, repositories, file I/O and navigation. */
class GarageDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var overrides: Module
    private lateinit var client: HttpClient
    private lateinit var store: EncryptedSessionStore
    private val vehicles = linkedMapOf(1L to "Comet")
    private val fuel = linkedMapOf<Long, JsonObject>()
    private val care = linkedMapOf<Long, JsonObject>()
    private val air = linkedMapOf<Long, JsonObject>()
    private val receipts = linkedMapOf<Long, JsonObject>()
    private var pressure: JsonObject =
        buildJsonObject {
            put("front_tire_pressure_solo", 28)
            put("rear_tire_pressure_solo", 32)
        }
    private var next = 100L

    @Volatile private var offline = false

    @Volatile private var loseSave = false

    @Volatile private var storageError = false
    private var mutations = 0
    private var storageCookie: String? = null
    private val monitors = mutableListOf<Instrumentation.ActivityMonitor>()

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    private fun history() =
        buildJsonObject {
            put("vehicle_name", vehicles.values.firstOrNull() ?: "Comet")
            pressure.forEach { (k, v) -> put(k, v) }
            put("fuel_fillups", JsonArray(fuel.values.toList()))
            put("air_fills", JsonArray(air.values.toList()))
            put(
                "maintenance_records",
                JsonArray(
                    care.values.map {
                        JsonObject(
                            it + ("attachments" to JsonArray(receipts.values.toList())),
                        )
                    },
                ),
            )
            put(
                "fuel_efficiency",
                obj(
                    """{"petrol":{"total_cost":250.25,"total_volume":2.5,"average_km_per_litre":null}}""",
                ),
            )
        }.toString()

    @Before fun setup() {
        store = GlobalContext.get().get()
        runBlocking { store.clear() }
        care[10] =
            obj(
                (
                    """{"id":10,""" +
""""category":"service",""" +
""""title":"Annual service",""" +
""""amount":900.50,""" +
""""occurred_at":"2026-10-17T10:00:00Z",""" +
""""odometer_km":1234,""" +
""""provider_name":"Care centre"}"""
                ),
            )
        val config = NetworkConfig("https://garage-fixture.test/api/", false)
        val clock = Clock.systemUTC()
        val cookies = PersistentSessionCookies(store, config, clock)
        client =
            pulseHttpClient(
                config,
                MockEngine { request ->
                    val path = request.url.encodedPath.removePrefix("/api/")
                    if (request.url.host == "receipt-storage.test") {
                        storageCookie = request.headers[HttpHeaders.Cookie]
                        respond(
                            "%PDF-1.4 fixture",
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/pdf"),
                        )
                    } else if (path ==
                        "auth/login"
                    ) {
                        respond(
                            """{"username":"garage-fixture"}""",
                            HttpStatusCode.OK,
                            headersOf(
                                HttpHeaders.ContentType to listOf("application/json"),
                                HttpHeaders.SetCookie to
                                    listOf(
                                        "life_session=garage-test-only; Path=/; Max-Age=3600; Secure; HttpOnly",
                                    ),
                            ),
                        )
                    } else if (path.endsWith("/attachments/3") && request.method == HttpMethod.Get) {
                        respond(
                            "",
                            HttpStatusCode.Found,
                            headersOf(
                                HttpHeaders.Location,
                                "https://receipt-storage.test/signed-receipt",
                            ),
                        )
                    } else {
                        if (offline) throw IOException("Offline fixture")
                        var status = HttpStatusCode.OK
                        val raw = (request.body as? TextContent)?.text ?: "{}"
                        val body =
                            if (request.method == HttpMethod.Get) {
                                when (path) {
                                    "auth/session" -> """{"username":"garage-fixture"}"""
                                    "profile" -> """{"has_profile":true,"name":"Asha"}"""
                                    "workout/today" -> """{"visited":false}"""
                                    "notifications" -> "[]"
                                    "horizon" ->
                                        """{"currency":"₹","remaining_amount":100,"base_amount":200,"total_deductions":100,"total_transactions":0}"""
                                    "vehicles" ->
                                        JsonArray(
                                            vehicles.map { (id, name) ->
                                                buildJsonObject {
                                                    put("id", id)
                                                    put("name", name)
                                                }
                                            },
                                        ).toString()
                                    else ->
                                        if (path.endsWith(
                                                "/history",
                                            )
                                        ) {
                                            history()
                                        } else {
                                            error("Unexpected read $path")
                                        }
                                }
                            } else {
                                mutations++
                                val parts = path.split('/')
                                if (parts.last() == "attachments") {
                                    val wire =
                                        request.body.toByteArray().decodeToString().substringBefore(
                                            "\r\n\r\n",
                                        )
                                    val disposition = wire.lineSequence().first { it.startsWith("Content-Disposition:", true) }
                                    check(disposition.contains("form-data", true))
                                    check(disposition.contains("name=file") || disposition.contains("name=\"file\""))
                                    check(disposition.contains("filename=receipt.pdf") || disposition.contains("filename=\"receipt.pdf\""))
                                    check(!disposition.contains("; file;"))
                                }
                                if (parts.last() == "attachments" &&
                                    storageError
                                ) {
                                    status = HttpStatusCode.InternalServerError
                                } else if (path ==
                                    "vehicles"
                                ) {
                                    vehicles[next++] =
                                        obj(raw).getValue("name").jsonPrimitive.content
                                } else if (parts.size == 2 &&
                                    request.method == HttpMethod.Delete
                                ) {
                                    vehicles.remove(parts[1].toLong())
                                    care.clear()
                                    fuel.clear()
                                    air.clear()
                                    receipts.clear()
                                } else {
                                    when (parts[2]) {
                                        "tire-pressure" -> pressure = obj(raw)
                                        "air-fills" ->
                                            if (request.method ==
                                                HttpMethod.Delete
                                            ) {
                                                air.remove(parts.last().toLong())
                                            } else {
                                                val id = next++
                                                air[id] =
                                                    obj("""{"id":$id,"filled_at":"2026-10-17T10:00:00Z"}""")
                                            }
                                        "fuel-fillups", "maintenance-records" -> {
                                            val target = if (parts[2] == "fuel-fillups") fuel else care
                                            if (parts.contains("attachments")) {
                                                if (request.method ==
                                                    HttpMethod.Delete
                                                ) {
                                                    receipts.remove(parts.last().toLong())
                                                } else {
                                                    receipts[3] =
                                                        obj(
                                                            (
                                                                """{"id":3,""" +
""""file_name":"receipt.pdf",""" +
""""content_type":"application/pdf",""" +
""""size_bytes":16,""" +
""""created_at":"2026-10-17T10:00:00Z"}"""
                                                            ),
                                                        )
                                                }
                                            } else if (request.method ==
                                                HttpMethod.Delete
                                            ) {
                                                target.remove(parts.last().toLong())
                                            } else {
                                                val id = parts.last().toLongOrNull() ?: next++
                                                val input = obj(raw)
                                                val value =
                                                    buildJsonObject {
                                                        input.forEach { (k, v) -> put(k, v) }
                                                        put("id", id)
                                                        if (parts[2] ==
                                                            "fuel-fillups"
                                                        ) {
                                                            put(
                                                                "items",
                                                                JsonArray(
                                                                    input
                                                                        .getValue(
                                                                            "items",
                                                                        ).jsonArray
                                                                        .map { tank ->
                                                                            JsonObject(
                                                                                tank.jsonObject +
                                                                                    (
                                                                                        "total_cost" to
                                                                                            JsonPrimitive(250.25)
                                                                                    ),
                                                                            )
                                                                        },
                                                                ),
                                                            )
                                                        }
                                                    }
                                                target[id] = value
                                            }
                                        }
                                    }
                                }
                                if (loseSave) {
                                    loseSave = false
                                    offline = true
                                    throw IOException("Lost after commit")
                                }
                                "{}"
                            }
                        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                },
                cookies,
            )
        val api = PulseApi(client)
        val auth = PulseAuthRepository(api, cookies, clock)
        val repo =
            PulseGarageRepository(
                GarageApi(client),
                auth,
                AndroidReceiptFiles(InstrumentationRegistry.getInstrumentation().targetContext),
            )
        overrides =
            module {
                single<AuthRepository> { auth }
                single<DashboardRepository> { PulseDashboardRepository(api, auth) }
                single<dev.abhishekan.pulse.core.domain.repository.ProfileRepository> {
                    dev.abhishekan.pulse.core.data
                        .PulseProfileRepository(api, auth, PulseDashboardRepository(api, auth))
                }
                single<dev.abhishekan.pulse.core.domain.repository.NotificationsRepository> {
                    dev.abhishekan.pulse.core.data
                        .PulseNotificationsRepository(api, auth, PulseDashboardRepository(api, auth))
                }
                single<GarageRepository> { repo }
            }
        loadKoinModules(overrides)
    }

    @After fun cleanup() {
        monitors.forEach { InstrumentationRegistry.getInstrumentation().removeMonitor(it) }
        scenario?.close()
        unloadKoinModules(overrides)
        loadKoinModules(dataModule)
        client.close()
        runBlocking { store.clear() }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(text: String) {
        if (text == "You") {
            compose.openProfileFromNavigation()
            return
        }
        if (text in listOf("Home", "Fitness", "Horizon", "Garage", "Meals", "You")) {
            scenario?.onActivity { activity ->
                activity
                    .getSystemService(
                        android.view.inputmethod.InputMethodManager::class.java,
                    ).hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
            }
        }
        awaitText(text)
        compose
            .onNodeWithText(text)
            .apply {
                if (!isDisplayed() && (text !in listOf("Home", "Fitness", "Horizon", "Garage", "Meals") || usesNavigationRail())) {
                    performScrollTo()
                }
            }.assertIsDisplayed()
            .performClick()
    }

    private fun pick(
        label: String,
        current: String,
        option: String,
    ) {
        tap("$label: $current")
        awaitText(option)
        compose.onNodeWithText(option).performScrollTo().performClick()
    }

    private fun field(
        label: String,
        value: String,
    ) {
        compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)
    }

    private fun confirm(text: String) {
        val matcher = hasText(text) and hasAnyAncestor(isDialog())
        try {
            compose.waitUntil(15_000) {
                compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (
            error: Exception,
        ) {
            screenshot("confirmation-failure")
            throw error
        }
        compose.onNode(matcher).performClick()
    }

    private fun launchGarage() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitText("Welcome back")
        field("Username", "garage-fixture")
        field("Password", "test-only")
        tap("Continue")
        awaitText("Asha")
        tap("Garage")
        awaitText("Your garage")
    }

    private fun select() {
        tap("View Comet")
        awaitText("Care, at a glance")
    }

    private fun action(
        tag: String,
        action: String,
    ) {
        compose
            .onNode(
                hasText(action) and hasAnyAncestor(hasTestTag(tag)),
            ).performScrollTo()
            .performClick()
    }

    private fun shell(command: String) =
        ParcelFileDescriptor
            .AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command),
            ).use {
                String(it.readBytes()).trim()
            }

    private fun screenshot(
        name: String,
        dialog: Boolean = false,
    ) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.externalCacheDir, "pulse-phase5-$name.png")
        val bitmap =
            if (dialog) {
                compose
                    .onNode(
                        isDialog(),
                    ).captureToImage()
                    .asAndroidBitmap()
            } else {
                checkNotNull(
                    InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot(),
                )
            }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        shell("cp ${file.absolutePath} /sdcard/Download/${file.name}")
    }

    private fun resultMonitor(
        action: String,
        result: Intent? = null,
        success: Boolean = true,
    ): Instrumentation.ActivityMonitor =
        Instrumentation
            .ActivityMonitor(
                IntentFilter(action).apply {
                    if (action in
                        listOf(Intent.ACTION_OPEN_DOCUMENT, Intent.ACTION_CREATE_DOCUMENT)
                    ) {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        addDataType("*/*")
                    }
                },
                Instrumentation.ActivityResult(
                    if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                    result,
                ),
                true,
            ).also {
                monitors += it
                InstrumentationRegistry.getInstrumentation().addMonitor(it)
            }

    @Test fun collectionArtworkAndLongNamesKeepVehicleActionsAccessible() {
        vehicles[2] = "Weekend adventure motorcycle with a long name"
        launchGarage()
        compose.onNodeWithText("Add vehicle").assertIsDisplayed()
        compose.onNodeWithText("View Comet").performScrollTo().assertIsDisplayed()
        screenshot("redesigned-collection")
        compose
            .onNodeWithText("View Weekend adventure motorcycle with a long name")
            .performScrollTo()
            .assertIsDisplayed()
        select()
        compose.onNodeWithText("Care, at a glance").performScrollTo().assertIsDisplayed()
    }

    @Test fun vehicleDetailsUseSeparatePageWithTopBackAndSurviveRecreation() {
        launchGarage()
        select()
        compose.onNodeWithText("Your garage").assertDoesNotExist()
        compose.onNodeWithText("Add vehicle").assertDoesNotExist()
        compose.onNodeWithContentDescription("Back to Garage").assertIsDisplayed()
        scenario!!.recreate()
        awaitText("Care, at a glance")
        compose.onNodeWithContentDescription("Back to Garage").assertIsDisplayed().performClick()
        awaitText("Your garage")
        compose.onNodeWithContentDescription("Back to Garage").assertDoesNotExist()
        select()
        compose.onNodeWithText("Care, at a glance").performScrollTo()
        compose.onNodeWithContentDescription("Back to Garage").assertIsDisplayed()
        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Your garage")
    }

    @Test fun pullToRefreshLoadsUpdatedVehicleHistory() {
        launchGarage()
        select()
        compose.onNodeWithText("Refresh vehicle history").assertDoesNotExist()
        compose.runOnIdle { vehicles[1] = "Updated Comet" }
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Care, at a glance"))).performTouchInput { swipeDown() }
        awaitText("Updated Comet")
        tap("Fuel")
        compose.runOnIdle { vehicles[1] = "Refreshed Comet" }
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Updated Comet"))).performTouchInput { swipeDown() }
        awaitText("Refreshed Comet")
    }

    @Test fun fuelCardsKeepInputsManualAndOptionalDetailsAcrossRecreation() {
        launchGarage()
        select()
        tap("Fuel")
        tap("Add fuel fill-up")
        screenshot("fuel-cards-trip")
        compose.onNodeWithText("Trip details").assertIsDisplayed()
        compose.onAllNodesWithText("Remove second tank").assertCountEquals(0)
        field("Odometer (km)", "1500")
        field("Quantity (L) · tank 1", "2.5")
        field("Price per litre (₹) · tank 1", "100")
        val total = compose.onNodeWithText("Total cost (₹) · tank 1")
        assertEquals("", total.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
        compose.onNodeWithText("Price per litre (₹) · tank 1").performScrollTo().performClick()
        compose.onNodeWithText("Save fuel fill-up").assertIsDisplayed()
        screenshot("fuel-cards-keyboard")
        scenario?.onActivity { activity ->
            activity
                .getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        tap("Partial")
        tap("How does this affect efficiency?")
        compose.onNodeWithText("A partial fill counts toward the next full-to-full efficiency interval.").assertIsDisplayed()
        tap("Missed fill-up")
        compose
            .onNodeWithText(
                "A missed fill resets the efficiency baseline. The next full tank starts a new interval.",
            ).performScrollTo()
            .assertIsDisplayed()
        tap("Full")
        tap("Add second fuel tank")
        field("Quantity (L) · tank 2", "1")
        tap("Remove second tank")
        tap("Add second fuel tank")
        assertEquals(
            "1",
            compose
                .onNodeWithText(
                    "Quantity (L) · tank 2",
                ).fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.EditableText]
                .text,
        )
        tap("Remove second tank")
        tap("Notes (optional)")
        field("Notes (optional)", "Evening pit stop")
        tap("Hide notes")
        screenshot("fuel-cards-before-recreation")
        scenario?.recreate()
        screenshot("fuel-cards-after-recreation")
        awaitText("Save fuel fill-up")
        tap("Notes (optional)")
        compose.onNodeWithText("Evening pit stop").performScrollTo().assertIsDisplayed()
        screenshot("fuel-cards-notes")
        tap("Save fuel fill-up")
        awaitText("1500 km")
        assertEquals(
            "Evening pit stop",
            fuel.values
                .single()["notes"]
                ?.jsonPrimitive
                ?.content,
        )
    }

    @Test fun vehicleFuelPressureAirAndMaintenanceCrud() {
        launchGarage()
        screenshot("collection")
        tap("Add vehicle")
        field("Vehicle name", "Tourer")
        tap("Save vehicle")
        awaitText("Tourer")
        select()
        screenshot("overview")
        tap("Set tire pressure")
        field("Rear · solo (PSI)", "34.5")
        tap("Save tire pressure")
        awaitText("Care, at a glance")
        assertEquals("34.5", pressure["rear_tire_pressure_solo"]?.jsonPrimitive?.content)
        tap("Record air fill now")
        awaitText("Saved. Your garage is up to date.")
        tap("Tires & air")
        tap("Delete air fill 101")
        confirm("Delete")
        compose.waitForIdle()
        assertTrue(air.isEmpty())
        tap("Fuel")
        tap("Add fuel fill-up")
        field("Odometer (km)", "1234.5")
        field("Quantity (L) · tank 1", "2.5")
        field("Price per litre (₹) · tank 1", "100.1")
        pick("Fuel type · tank 1", "Petrol", "Diesel")
        screenshot("fuel-editor")
        tap("Save fuel fill-up")
        awaitText("1234.5 km")
        assertEquals(
            "diesel",
            fuel.values
                .single()
                .getValue(
                    "items",
                ).jsonArray
                .first()
                .jsonObject
                .getValue("fuel_type")
                .jsonPrimitive.content,
        )
        screenshot("fuel-history")
        action("garage-fuel-102", "Edit")
        field("Station / vendor (optional)", "Roadside")
        tap("Save fuel fill-up")
        awaitText("Roadside")
        action("garage-fuel-102", "Delete")
        confirm("Delete")
        awaitText("No fuel fill-ups")
        tap("Maintenance")
        tap("Add maintenance")
        field("Title", "Tyre care")
        field("Amount (₹)", "499.95")
        tap("Save maintenance")
        awaitText("Tyre care")
        action("garage-care-103", "Edit")
        field("Title", "New tyre")
        tap("Save maintenance")
        awaitText("New tyre")
        screenshot("maintenance")
        action("garage-care-103", "Delete")
        screenshot("delete-confirmation", true)
        confirm("Delete")
        awaitText("Annual service")
        tap("Overview")
        tap("Delete Comet")
        confirm("Delete")
        awaitText("Your garage")
        assertFalse(vehicles.containsKey(1))
        assertTrue(care.isEmpty())
    }

    @Test fun draftRotationTabsLargeTextKeyboardAndDiscard() {
        launchGarage()
        select()
        tap("Add maintenance")
        field("Title", "Unsaved care")
        field("Amount (₹)", "-2")
        tap("Save maintenance")
        assertEquals(0, mutations)
        scenario!!.recreate()
        awaitText("Unsaved care")
        tap("Home")
        tap("Garage")
        awaitText("Unsaved care")
        tap("You")
        tap("Log out")
        awaitText("Log out and discard edits?")
        confirm("Cancel")
        tap("Garage")
        awaitText("Unsaved care")
        compose.onNodeWithText("Title").performScrollTo().performImeAction()
        compose.onNodeWithText("Amount (₹)").assertIsFocused()
        val target =
            48 *
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext.resources.displayMetrics.density
        compose.waitUntil(15_000) {
            compose
                .onNodeWithText("Amount (₹)")
                .fetchSemanticsNode()
                .boundsInRoot.height >=
                target
        }
        screenshot("draft")
        compose.onNodeWithContentDescription("Back to Garage").performClick()
        confirm("Cancel")
        compose.onNodeWithText("Unsaved care").assertExists()
        compose.onNodeWithContentDescription("Back to Garage").performClick()
        confirm("Discard edits")
        awaitText("Care, at a glance")
    }

    @Test fun lostSaveRequiresServerReviewAndDoesNotDuplicate() {
        launchGarage()
        select()
        tap("Add maintenance")
        field("Title", "Lost receipt record")
        field("Amount (₹)", "123.45")
        loseSave =
            true
        tap("Save maintenance")
        awaitText("Review before another change")
        compose.onNodeWithText("Save maintenance").assertIsNotEnabled()
        assertEquals(1, mutations)
        compose.onNodeWithText("I reviewed the server records").assertIsNotEnabled()
        offline =
            false
        tap("Refresh server records")
        tap("View server records")
        tap("Maintenance")
        awaitText("Lost receipt record")
        screenshot("recovery")
        tap("I reviewed the server records")
        tap("Resume unsaved edits")
        tap("Cancel editing")
        confirm("Discard edits")
        assertEquals(1, mutations)
    }

    @Test fun receiptPickerUploadDownloadRemovalAndFailureRecovery() {
        launchGarage()
        select()
        tap("Maintenance")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = File(context.cacheDir, "receipts/test").apply { mkdirs() }
        val file = File(directory, "receipt.pdf").apply { writeText("%PDF-1.4 fixture") }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.receipts", file)
        val cancelled = resultMonitor(Intent.ACTION_OPEN_DOCUMENT, success = false)
        tap("Attach receipt to Annual service")
        compose.waitForIdle()
        assertEquals(0, mutations)
        assertEquals(1, cancelled.hits)
        instrumentation.removeMonitor(cancelled)
        val upload = resultMonitor(Intent.ACTION_OPEN_DOCUMENT, Intent().setData(uri))
        tap("Attach receipt to Annual service")
        awaitText("receipt.pdf · 16 bytes")
        assertEquals(1, upload.hits)
        instrumentation.removeMonitor(upload)
        screenshot("receipts")
        val output = File(directory, "download.pdf").apply { writeText("") }
        val outputUri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.receipts",
                output,
            )
        val save = resultMonitor(Intent.ACTION_CREATE_DOCUMENT, Intent().setData(outputUri))
        tap("Download receipt.pdf")
        awaitText("Receipt downloaded to your selected document.")
        instrumentation.removeMonitor(save)
        assertEquals("%PDF-1.4 fixture", output.readText())
        assertNull(storageCookie)
        val view = resultMonitor(Intent.ACTION_CHOOSER, success = false)
        tap("Open receipt.pdf")
        compose.waitUntil(10_000) { view.hits > 0 }
        instrumentation.removeMonitor(view)
        assertNull(storageCookie)
        tap("Remove receipt.pdf")
        confirm("Delete")
        awaitText("No receipts attached.")
        assertTrue(receipts.isEmpty())
        storageError = true
        val failed = resultMonitor(Intent.ACTION_OPEN_DOCUMENT, Intent().setData(uri))
        tap("Attach receipt to Annual service")
        awaitText("Review before another change")
        instrumentation.removeMonitor(failed)
        assertTrue(care.containsKey(10))
        tap("I reviewed the server records")
        storageError = false
        val retry = resultMonitor(Intent.ACTION_OPEN_DOCUMENT, Intent().setData(uri))
        tap("Attach receipt to Annual service")
        awaitText("receipt.pdf · 16 bytes")
        instrumentation.removeMonitor(retry)
    }

    @Test fun attachmentLimitsUnreadableFilesAndPickerCancellationKeepRecord() {
        launchGarage()
        select()
        tap("Maintenance")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file =
            File(context.cacheDir, "receipts/oversize.pdf").apply {
                parentFile!!.mkdirs()
                outputStream().use {
                    it.write(
                        ByteArray(
                            10 * 1024 * 1024 + 1,
                        ),
                    )
                }
            }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.receipts", file)
        val large = resultMonitor(Intent.ACTION_OPEN_DOCUMENT, Intent().setData(uri))
        tap("Attach receipt to Annual service")
        awaitText("Review before another change")
        instrumentation.removeMonitor(large)
        assertEquals(0, mutations)
        assertTrue(care.containsKey(10))
        tap("I reviewed the server records")
        file.delete()
        val unreadable = resultMonitor(Intent.ACTION_OPEN_DOCUMENT, Intent().setData(uri))
        tap("Attach receipt to Annual service")
        awaitText("Review before another change")
        instrumentation.removeMonitor(unreadable)
        assertEquals(0, mutations)
        assertTrue(care.containsKey(10))
    }

    @Test fun nativeDateTimePickersAndResizingPreserveDraft() {
        val size =
            shell("wm size")
                .lineSequence()
                .firstOrNull {
                    it.startsWith("Override size:")
                }?.substringAfter(": ")
        val density =
            shell("wm density")
                .lineSequence()
                .firstOrNull {
                    it.startsWith("Override density:")
                }?.substringAfter(": ")
        try {
            launchGarage()
            select()
            tap("Add maintenance")
            field("Title", "Resized care")
            field("Amount (₹)", "500.25")
            compose.onNodeWithText("Date:", substring = true).performScrollTo().performClick()
            screenshot("date-picker", true)
            confirm("Choose date")
            compose.onNodeWithText("Time:", substring = true).performScrollTo().performClick()
            screenshot("time-picker", true)
            confirm("Choose time")
            shell("wm size 2048x1800")
            shell("wm density 320")
            awaitText("Resized care")
            compose.onNodeWithText("500.25").performScrollTo().assertIsDisplayed()
            screenshot("resized-editor")
            tap("Save maintenance")
            awaitText("Care, at a glance")
            assertEquals(1, mutations)
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
            compose.waitForIdle()
        }
    }
}
