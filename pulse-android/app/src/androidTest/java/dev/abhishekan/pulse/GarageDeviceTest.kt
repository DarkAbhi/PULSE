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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipeDown
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private val vehicleDetails = mutableMapOf<Long, JsonObject>()
    private val fuel = linkedMapOf<Long, JsonObject>()
    private val care = linkedMapOf<Long, JsonObject>()
    private val air = linkedMapOf<Long, JsonObject>()
    private val receipts = linkedMapOf<Long, JsonObject>()
    private var pressure: JsonObject =
        buildJsonObject {
            put("front_tire_pressure_solo", 28)
            put("rear_tire_pressure_solo", 32)
        }
    private var efficiencyFixture =
        """{"petrol":{"total_cost":250.25,"total_volume":2.5,"average_km_per_litre":null}}"""
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
            put("vehicle_type", "two_wheeler")
            put("fuel_types", JsonArray(listOf(JsonPrimitive("petrol"))))
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
                    efficiencyFixture,
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
                                    "profile" -> """{"has_profile":true,"name":"Asha","currency":"INR"}"""
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
                                                    put(
                                                        "vehicle_type",
                                                        vehicleDetails[id]?.get("vehicle_type") ?: JsonPrimitive("two_wheeler"),
                                                    )
                                                    put(
                                                        "fuel_types",
                                                        vehicleDetails[id]?.get("fuel_types") ?: JsonArray(listOf(JsonPrimitive("petrol"))),
                                                    )
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
                                    val details = obj(raw)
                                    assertTrue(
                                        details.getValue("vehicle_type").jsonPrimitive.content in listOf("two_wheeler", "four_wheeler"),
                                    )
                                    val fuels = details.getValue("fuel_types").jsonArray.map { it.jsonPrimitive.content }
                                    assertTrue(fuels.size in 1..2 && fuels.distinct().size == fuels.size)
                                    val id = next++
                                    vehicles[id] = details.getValue("name").jsonPrimitive.content
                                    vehicleDetails[id] = details
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
        try {
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            println("Waiting for text: $text")
            println(runCatching { compose.onRoot().printToString(maxDepth = 100) }.getOrElse { "No UI root: ${it.message}" })
            throw failure
        }
    }

    private fun tap(text: String) {
        if (text == "You") {
            compose.openProfileFromNavigation()
            return
        }
        scenario?.onActivity { activity ->
            if (text.startsWith("Save ")) activity.window.decorView.clearFocus()
            activity
                .getSystemService(
                    android.view.inputmethod.InputMethodManager::class.java,
                ).hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        // Keyboard dismissal changes the scroll viewport asynchronously.
        compose.waitUntil(15_000) {
            var keyboardHidden = false
            scenario?.onActivity { activity ->
                val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
                keyboardHidden = insets != null &&
                    !insets.isVisible(WindowInsetsCompat.Type.ime()) &&
                    insets.getInsets(WindowInsetsCompat.Type.ime()).bottom == 0
            }
            keyboardHidden
        }
        val button = (hasText(text) or hasContentDescription(text)) and hasClickAction() and isEnabled()
        compose.waitUntil(15_000) { compose.onAllNodes(button).fetchSemanticsNodes().isNotEmpty() }
        val node = compose.onAllNodes(button)[0]
        if (hasAnyAncestor(hasScrollAction()).matches(node.fetchSemanticsNode())) {
            node.performScrollTo()
        }
        compose.waitUntil(15_000) { node.isDisplayed() }
        node
            .assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
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
        val field = hasText(label) and hasSetTextAction()
        compose.waitUntil(15_000) { compose.onAllNodes(field).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(field).performScrollTo().performTextReplacement(value)
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
            .assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
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
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Fuel history"))).performTouchInput { swipeDown() }
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
        awaitText("1500")
        assertEquals(
            "Evening pit stop",
            fuel.values
                .single()["notes"]
                ?.jsonPrimitive
                ?.content,
        )
    }

    @Test fun tabletVehicleArtworkCyclesUntilTypeSelectedWithoutChangingDraft() {
        val originalSize =
            shell("wm size")
                .lineSequence()
                .firstOrNull { it.startsWith("Override size:") }
                ?.substringAfter(": ")
        val originalDensity =
            shell("wm density")
                .lineSequence()
                .firstOrNull { it.startsWith("Override density:") }
                ?.substringAfter(": ")
        val originalAnimationScale = shell("settings get global animator_duration_scale").toFloatOrNull() ?: 1f
        val originalFontScale = shell("settings get system font_scale").toFloatOrNull() ?: 1f
        try {
            launchGarage()
            // CI starts a phone with animations disabled; this case exercises the tablet animation.
            shell("wm size 2560x1800")
            shell("wm density 320")
            shell("settings put system font_scale 1.0")
            shell("settings put global animator_duration_scale 1")
            compose.waitUntil(15_000) {
                val configuration =
                    InstrumentationRegistry
                        .getInstrumentation()
                        .targetContext.resources.configuration
                configuration.densityDpi == 320 &&
                    configuration.fontScale == 1f &&
                    usesNavigationRail() &&
                    android.animation.ValueAnimator.areAnimatorsEnabled()
            }
            awaitText("Your garage")
            tap("Add vehicle")
            field("Vehicle name", "Unsaved scooter")
            compose.onNodeWithText("Add a new vehicle").performScrollTo()
            val artwork = hasTestTag("vehicle-editor-artwork")
            compose.onNode(artwork).assertExists()
            val first = compose.onNode(artwork).captureToImage().asAndroidBitmap()
            try {
                compose.mainClock.advanceTimeBy(10_500)
                compose.waitUntil(12_000) {
                    val next = compose.onNode(artwork).captureToImage().asAndroidBitmap()
                    val changed = !first.sameAs(next)
                    next.recycle()
                    changed
                }
            } finally {
                first.recycle()
            }
            tap("Two-wheeler")
            compose.onNodeWithText("Add a new vehicle").performScrollTo()
            compose.mainClock.advanceTimeBy(2_500)
            compose.waitForIdle()
            val motorcycle = compose.onNode(artwork).captureToImage().asAndroidBitmap()
            try {
                compose.mainClock.advanceTimeBy(10_500)
                val selected = compose.onNode(artwork).captureToImage().asAndroidBitmap()
                assertTrue("Selected vehicle artwork should stay fixed", motorcycle.sameAs(selected))
                selected.recycle()
                tap("Four-wheeler")
                compose.onNodeWithText("Add a new vehicle").performScrollTo()
                compose.mainClock.advanceTimeBy(2_500)
                val car = compose.onNode(artwork).captureToImage().asAndroidBitmap()
                assertTrue("Selecting a different vehicle type should change its artwork", !motorcycle.sameAs(car))
                car.recycle()
            } finally {
                motorcycle.recycle()
            }
            compose.onNodeWithText("Unsaved scooter").assertIsDisplayed()
            assertEquals(0, mutations)
            screenshot("vehicle-artwork-cycle")
        } finally {
            scenario?.close()
            scenario = null
            shell("settings put global animator_duration_scale $originalAnimationScale")
            shell("settings put system font_scale $originalFontScale")
            shell("wm size ${originalSize ?: "reset"}")
            shell("wm density ${originalDensity ?: "reset"}")
        }
    }

    @Test fun addVehicleCancelIsCenteredAndKeepsDiscardProtection() {
        launchGarage()
        tap("Add vehicle")
        field("Vehicle name", "Unsaved scooter")
        compose.onNodeWithText("Cancel").performScrollTo()
        val cancelBounds = compose.onNodeWithText("Cancel").fetchSemanticsNode().boundsInRoot
        val saveBounds = compose.onNodeWithText("Save vehicle").fetchSemanticsNode().boundsInRoot
        assertEquals(saveBounds.center.x, cancelBounds.center.x, 1f)
        tap("Cancel")
        confirm("Discard edits")
        awaitText("Add vehicle")
        tap("Add vehicle")
        field("Vehicle name", "Back button draft")
        compose.onNodeWithText("Cancel").performScrollTo()
        compose.onNode(hasContentDescription("Back to garage")).assertIsDisplayed().performClick()
        confirm("Discard edits")
        awaitText("Add vehicle")
        tap("Add vehicle")
        compose.onNode(hasContentDescription("Back to garage")).assertIsDisplayed().performClick()
        awaitText("Add vehicle")
        assertEquals(0, mutations)
    }

    @Test fun addVehiclePreservesOptionalPressures() {
        launchGarage()
        tap("Add vehicle")
        field("Vehicle name", "City scooter")
        tap("Two-wheeler")
        tap("Electric")
        compose.onNodeWithText("Add a new vehicle").performScrollTo()
        screenshot("add-vehicle-top")
        field("Front · solo (PSI)", "28.5")
        field("Rear · solo (PSI)", "32")
        field("Front · pillion (PSI)", "30")
        field("Rear · pillion (PSI)", "36")
        compose.onNodeWithText("Active vehicle").assertDoesNotExist()
        compose.onNodeWithText("Cancel editing").assertDoesNotExist()
        scenario!!.recreate()
        awaitText("Add a new vehicle")
        compose.onNodeWithText("Save vehicle").performScrollTo()
        screenshot("add-vehicle-pressures")
        tap("Save vehicle")
        compose.waitUntil(15_000) { vehicleDetails.values.any { it["name"]?.jsonPrimitive?.content == "City scooter" } }
        awaitText("City scooter")
        val saved = vehicleDetails.values.single()
        assertEquals("true", saved.getValue("is_active").jsonPrimitive.content)
        assertEquals("28.5", saved.getValue("front_tire_pressure_solo").jsonPrimitive.content)
        assertEquals("32", saved.getValue("rear_tire_pressure_solo").jsonPrimitive.content)
        assertEquals("30", saved.getValue("front_tire_pressure_pillion").jsonPrimitive.content)
        assertEquals("36", saved.getValue("rear_tire_pressure_pillion").jsonPrimitive.content)
        assertEquals(listOf("electric"), saved.getValue("fuel_types").jsonArray.map { it.jsonPrimitive.content })
    }

    @Test fun vehicleClassificationRequiresSelectionsAndSurvivesRecreation() {
        launchGarage()
        tap("Add vehicle")
        field("Vehicle name", "Dual fuel car")
        tap("Save vehicle")
        compose.waitForIdle()
        assertEquals(0, mutations)
        tap("Four-wheeler")
        tap("Petrol")
        tap("Dual tank (two fuel types)")
        tap("Second fuel type (required): Select an option")
        compose.onNode(hasText("Petrol") and hasAnyAncestor(isPopup())).assertDoesNotExist()
        compose.onNode(hasText("CNG") and hasAnyAncestor(isPopup())).performClick()
        tap("CNG")
        awaitText("Second fuel type (required): Select an option")
        tap("Save vehicle")
        compose.waitForIdle()
        assertEquals(0, mutations)
        tap("Second fuel type (required): Select an option")
        compose.onNode(hasText("Petrol") and hasAnyAncestor(isPopup())).performClick()
        scenario!!.recreate()
        awaitText("Second fuel type (required): Petrol")
        screenshot("vehicle-classification")
        tap("Save vehicle")
        awaitText("Dual fuel car")
        awaitText("Four-wheeler · CNG + Petrol")
        assertEquals(1, mutations)
    }

    @Test fun redesignedOverviewKeepsTitleActionsAndPressureAccessible() {
        efficiencyFixture =
            """{"petrol":{"total_cost":2000,"total_volume":20,"average_km_per_litre":22.1,"last_km_per_litre":22.1,"min_km_per_litre":22.1,"max_km_per_litre":22.1}}"""
        pressure =
            obj(
                """{"front_tire_pressure_solo":27,"rear_tire_pressure_solo":34,"front_tire_pressure_pillion":29,"rear_tire_pressure_pillion":36}""",
            )
        launchGarage()
        select()
        compose.onNodeWithText("Latest care").assertDoesNotExist()
        compose.onNodeWithText("Air fills are recorded at the server’s current time.").assertDoesNotExist()
        compose.onAllNodesWithText("Comet").assertCountEquals(1)
        compose.onNodeWithText("Comet").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to Garage").assertIsDisplayed()
        val titleBounds = compose.onNodeWithText("Comet").getUnclippedBoundsInRoot()
        val backBounds = compose.onNodeWithContentDescription("Back to Garage").getUnclippedBoundsInRoot()
        assertTrue(titleBounds.left >= backBounds.right)
        assertTrue(titleBounds.top < backBounds.bottom)
        compose.onAllNodesWithText("22.1").assertCountEquals(4)
        compose.onNodeWithText("km/L").assertExists()
        screenshot("detail-redesign-top")
        compose.onNodeWithText("Record air fill now").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Solo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pillion").performScrollTo().assertIsDisplayed()
        screenshot("detail-redesign-pressure")
        tap("Set tire pressure")
        awaitText("Save tire pressure")
    }

    @Test fun redesignedTiresAirKeepsPressureHistoryAndActionsAccessible() {
        pressure =
            obj(
                """{"front_tire_pressure_solo":27,"rear_tire_pressure_solo":34,"front_tire_pressure_pillion":29,"rear_tire_pressure_pillion":36}""",
            )
        air[41] = obj("""{"id":41,"filled_at":"2026-09-27T17:47:00Z"}""")
        launchGarage()
        select()
        tap("Tires & air")
        compose.onNodeWithText("Solo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("27").assertExists()
        compose.onNodeWithText("34").assertExists()
        compose.onNodeWithText("Pillion").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("29").assertExists()
        compose.onNodeWithText("36").assertExists()
        compose.onNodeWithText("Air fill 41").performScrollTo().assertIsDisplayed()
        screenshot("tires-air-history")
        tap("Delete air fill 41")
        confirm("Delete")
        compose.waitUntil(10_000) { air.isEmpty() }
        compose.onNodeWithText("No air fills recorded").performScrollTo().assertIsDisplayed()
        tap("Record air fill now")
        compose.waitUntil(10_000) { air.size == 1 }
        tap("Set tire pressure")
        awaitText("Save tire pressure")
    }

    @Test fun redesignedFuelHistoryKeepsBadgesAndActionsAccessible() {
        fuel[41] =
            obj(
                """{"id":41,"odometer_km":34820,"filled_at":"2026-10-04T01:53:00Z","items":[{"fuel_type":"petrol","fill_type":"full","quantity":10,"unit_price":100,"total_cost":1000}]}""",
            )
        launchGarage()
        select()
        tap("Fuel")
        awaitText("34820")
        compose.onNodeWithText("Petrol").assertExists()
        compose.onNodeWithText("Full").assertExists()
        compose.onNodeWithText("10 L").assertExists()
        screenshot("fuel-redesign")
        action("garage-fuel-41", "Edit")
        awaitText("Save fuel fill-up")
        tap("Cancel editing")
        action("garage-fuel-41", "Delete")
        confirm("Delete")
        awaitText("No fuel fill-ups")
        assertTrue(fuel.isEmpty())
    }

    @Test fun vehicleFuelPressureAirAndMaintenanceCrud() {
        launchGarage()
        screenshot("collection")
        tap("Add vehicle")
        field("Vehicle name", "Tourer")
        tap("Four-wheeler")
        tap("Petrol")
        tap("Dual tank (two fuel types)")
        tap("Second fuel type (required): Select an option")
        compose.onNode(hasText("CNG") and hasAnyAncestor(isPopup())).performClick()
        tap("Save vehicle")
        awaitText("Tourer")
        assertEquals(
            "four_wheeler",
            vehicleDetails.values
                .single()
                .getValue("vehicle_type")
                .jsonPrimitive.content,
        )
        assertEquals(
            listOf("petrol", "cng"),
            vehicleDetails.values
                .single()
                .getValue("fuel_types")
                .jsonArray
                .map { it.jsonPrimitive.content },
        )
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
        awaitText("1234.5")
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

    @Test fun receiptListExpandsAndCollapses() {
        (1L..5L).forEach { id ->
            receipts[id] =
                buildJsonObject {
                    put("id", id)
                    put("file_name", "service-$id.pdf")
                    put("content_type", "application/pdf")
                    put("size_bytes", 130257)
                    put("created_at", "2026-10-17T10:00:00Z")
                }
        }
        launchGarage()
        select()
        tap("Maintenance")
        awaitText("Receipts (5)")
        compose.onNode(hasTestTag("garage-receipt-3")).assertExists()
        compose.onNode(hasTestTag("garage-receipt-4")).assertDoesNotExist()
        screenshot("compact-receipts")
        tap("Show all 5 documents")
        compose.onNode(hasTestTag("garage-receipt-5")).assertExists()
        tap("Show fewer documents")
        compose.onNode(hasTestTag("garage-receipt-4")).assertDoesNotExist()
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
        awaitText("receipt.pdf")
        awaitText(
            android.text.format.Formatter
                .formatShortFileSize(context, 16),
        )
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
        tap("Options for receipt.pdf")
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
        awaitText("receipt.pdf")
        awaitText(
            android.text.format.Formatter
                .formatShortFileSize(context, 16),
        )
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
