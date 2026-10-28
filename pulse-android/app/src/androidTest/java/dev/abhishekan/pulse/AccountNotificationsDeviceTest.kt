package dev.abhishekan.pulse
import android.content.ClipDescription
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.view.WindowManager
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.data.EncryptedSessionStore
import dev.abhishekan.pulse.core.data.PulseAuthRepository
import dev.abhishekan.pulse.core.data.PulseDashboardRepository
import dev.abhishekan.pulse.core.data.PulseFitnessRepository
import dev.abhishekan.pulse.core.data.PulseGarageRepository
import dev.abhishekan.pulse.core.data.PulseHorizonRepository
import dev.abhishekan.pulse.core.data.PulseMealPlanRepository
import dev.abhishekan.pulse.core.data.PulseNotificationsRepository
import dev.abhishekan.pulse.core.data.PulseProfileRepository
import dev.abhishekan.pulse.core.data.di.dataModule
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
import dev.abhishekan.pulse.core.domain.repository.GarageRepository
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository
import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository
import dev.abhishekan.pulse.core.network.FitnessApi
import dev.abhishekan.pulse.core.network.GarageApi
import dev.abhishekan.pulse.core.network.HorizonApi
import dev.abhishekan.pulse.core.network.MealPlanApi
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.pulseHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** All account and notification fixtures are isolated from production traffic and credentials. */
class AccountNotificationsDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var overrides: Module
    private lateinit var store: EncryptedSessionStore
    private lateinit var client: HttpClient
    private lateinit var auth: PulseAuthRepository
    private var name: String? = "Asha"
    private var profileFailure = false
    private var currency = "GBP"
    private var currencyWrites = 0
    private var timezone = "UTC"
    private var timezoneWrites = 0
    private var wrongPassword = false
    private var loseKey = false
    private var loseDismiss = false
    private var passwordWrites = 0
    private var keyWrites = 0
    private var profileWrites = 0
    private var passwordBody = ""
    private val token = "lt_" + "b".repeat(64)
    private val keys = linkedMapOf<Long, JsonObject>()
    private val notifications = linkedMapOf<Long, JsonObject>()
    private val clock =
        object : Clock() {
            var offset = 0L

            override fun getZone() = ZoneOffset.UTC

            override fun withZone(zone: ZoneId) = this

            override fun instant() = Instant.now().plusSeconds(offset)
        }

    private fun key(
        id: Long,
        label: String,
    ) = buildJsonObject {
        put("id", id)
        put("name", label)
        put("token_prefix", "lt_bbbbbbbb")
        put("created_at", "2026-10-18T00:00:00Z")
    }

    private fun notification(
        id: Long,
        title: String,
        target: String?,
    ) = buildJsonObject {
        put("id", id)
        put("title", title)
        put("source", "Garage")
        put("body", "A little care for your vehicle.")
        put("priority", 1)
        put("created_at", "2026-10-18T00:00:00Z")
        put(
            "target_path",
            target?.let(::JsonPrimitive) ?: JsonNull,
        )
    }

    @Before fun setup() {
        store = GlobalContext.get().get()
        runBlocking { store.clear() }
        keys[1] = key(1, "Watch")
        notifications[1] = notification(1, "Vehicle care update", "/garage")
        notifications[2] =
            notification(2, "Information only", "https://outside.example.test")
        val config = NetworkConfig("https://phase7.fixture.test/api/", false)
        val cookies = PersistentSessionCookies(store, config, clock)
        client =
            pulseHttpClient(
                config,
                MockEngine { request ->
                    val path = request.url.encodedPath.removePrefix("/api/")
                    val verb = request.method.value
                    val json = (request.body as? TextContent)?.text?.let { Json.parseToJsonElement(it).jsonObject }
                    var status = HttpStatusCode.OK
                    var cookie: String? = null
                    val body =
                        when {
                            path == "auth/login" -> {
                                cookie = "life_session=phase7-fixture; Path=/; Max-Age=3600; Secure; HttpOnly"
                                """{"username":"fixture-user"}"""
                            }
                            path == "auth/session" -> """{"username":"fixture-user"}"""
                            path == "auth/logout" -> {
                                cookie = "life_session=; Path=/; Max-Age=0; Secure"
                                "{}"
                            }
                            path == "profile" && verb == "GET" ->
                                if (profileFailure) {
                                    status = HttpStatusCode.ServiceUnavailable
                                    "{}"
                                } else {
                                    buildJsonObject {
                                        put(
                                            "has_profile",
                                            name != null,
                                        )
                                        put("timezone", timezone)
                                        put("currency", currency)
                                        name?.let { put("name", it) }
                                    }.toString()
                                }
                            path == "profile" && verb == "PUT" -> {
                                profileWrites++
                                name = json!!.getValue("name").jsonPrimitive.content
                                buildJsonObject { put("name", name) }.toString()
                            }
                            path == "profile/currency" && verb == "PUT" -> {
                                currencyWrites++
                                currency = json!!.getValue("currency").jsonPrimitive.content
                                buildJsonObject { put("currency", currency) }.toString()
                            }
                            path == "profile/timezone" && verb == "PUT" -> {
                                timezoneWrites++
                                timezone = json!!.getValue("timezone").jsonPrimitive.content
                                buildJsonObject { put("timezone", timezone) }.toString()
                            }
                            path == "profile/password" -> {
                                passwordWrites++
                                passwordBody = (request.body as TextContent).text
                                if (wrongPassword) {
                                    status =
                                        HttpStatusCode.BadRequest
                                    """{"error":"current password is incorrect"}"""
                                } else {
                                    "{}"
                                }
                            }
                            path == "profile/api-keys" && verb == "GET" -> JsonArray(keys.values.toList()).toString()
                            path == "profile/api-keys" && verb == "POST" -> {
                                keyWrites++
                                val created = key(10 + keyWrites.toLong(), json!!.getValue("name").jsonPrimitive.content)
                                keys[
                                    10 +
                                        keyWrites.toLong(),
                                ] =
                                    created
                                if (loseKey)throw SocketTimeoutException("fixture lost response")
                                JsonObject(
                                    created + mapOf("token" to JsonPrimitive(token)),
                                ).toString()
                            }
                            path.startsWith("profile/api-keys/") && verb == "DELETE" -> {
                                keys.remove(path.substringAfterLast('/').toLong())
                                status =
                                    HttpStatusCode.NoContent
                                ""
                            }
                            path == "notifications" && verb == "GET" ->
                                JsonArray(
                                    notifications.values.take(request.url.parameters["limit"]?.toInt() ?: 100),
                                ).toString()
                            path == "notifications" && verb == "DELETE" -> {
                                notifications.clear()
                                status = HttpStatusCode.NoContent
                                ""
                            }
                            path.startsWith("notifications/") && verb == "DELETE" -> {
                                notifications.remove(path.substringAfterLast('/').toLong())
                                if (loseDismiss)throw SocketTimeoutException("fixture lost dismissal")
                                status =
                                    HttpStatusCode.NoContent
                                ""
                            }
                            path == "workout/today" -> """{"visited":false}"""
                            path == "horizon" -> """{"currency":"USD","base_amount":100,"remaining_amount":100,"total_deductions":0}"""
                            path == "vehicles" -> "[]"
                            path.startsWith("vehicles/") -> {
                                status = HttpStatusCode.NotFound
                                "{}"
                            }
                            else -> {
                                status = HttpStatusCode.NotFound
                                "{}"
                            }
                        }
                    respond(
                        body,
                        status,
                        Headers.build {
                            append(HttpHeaders.ContentType, "application/json")
                            cookie?.let { append(HttpHeaders.SetCookie, it) }
                        },
                    )
                },
                cookies,
            )
        val api = PulseApi(client)
        auth = PulseAuthRepository(api, cookies, clock)
        val dashboard = PulseDashboardRepository(api, auth)
        overrides =
            module {
                single<AuthRepository> { auth }
                single<DashboardRepository> { dashboard }
                single<ProfileRepository> { PulseProfileRepository(api, auth, dashboard) }
                single<NotificationsRepository> { PulseNotificationsRepository(api, auth, dashboard) }
                single<GarageRepository> { PulseGarageRepository(GarageApi(client), auth, GlobalContext.get().get()) }
                single<FitnessRepository> { PulseFitnessRepository(FitnessApi(client), auth) }
                single<HorizonRepository> { PulseHorizonRepository(HorizonApi(client), auth) }
                single<MealPlanRepository> { PulseMealPlanRepository(MealPlanApi(client), auth) }
            }
        loadKoinModules(overrides)
    }

    @After fun cleanup() {
        scenario?.close()
        unloadKoinModules(overrides)
        loadKoinModules(dataModule)
        client.close()
        runBlocking { store.clear() }
    }

    private fun awaitText(
        text: String,
        substring: Boolean = false,
    ) {
        try {
            compose.waitUntil(20_000) {
                runCatching {
                    compose
                        .onAllNodesWithText(
                            text,
                            substring = substring,
                        ).fetchSemanticsNodes()
                        .isNotEmpty()
                }.getOrDefault(false)
            }
        } catch (failure: Throwable) {
            println("Waiting for text: $text\n${runCatching { compose.onAllNodes(isRoot()).printToString(maxDepth = 100) }.getOrNull()}")
            throw failure
        }
    }

    private fun hideKeyboard() {
        scenario?.onActivity {
            it
                .getSystemService(
                    android.view.inputmethod.InputMethodManager::class.java,
                ).hideSoftInputFromWindow(it.window.decorView.windowToken, 0)
        }
    }

    private fun tap(text: String) {
        if (text == "You") {
            compose.openProfileFromNavigation()
            return
        }
        hideKeyboard()
        val button = hasText(text) and hasClickAction() and isEnabled()
        compose.waitUntil(20_000) { compose.onAllNodes(button).fetchSemanticsNodes().isNotEmpty() }
        compose
            .onAllNodes(button)[0]
            .apply {
                if (text !in listOf("Home", "Fitness", "Horizon", "Garage", "Meals") || usesNavigationRail()) {
                    performScrollTo()
                }
            }.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
    }

    private fun field(
        label: String,
        value: String,
    ) {
        compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)
    }

    private fun confirm(text: String) {
        val match =
            hasText(text) and hasAnyAncestor(isDialog())
        compose.waitUntil(20_000) { compose.onAllNodes(match).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(match).performClick()
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitText("Welcome back")
        field("Username", "fixture-user")
        field("Password", "fixture-password")
        tap("Continue")
        if (name ==
            null
        ) {
            awaitText("Make Pulse yours")
        } else {
            awaitText("Good to see you,")
        }
    }

    private fun openProfile() {
        tap("You")
        awaitText("Account information")
    }

    private fun openNotifications() {
        compose.onNodeWithContentDescription("Notifications").performClick()
        awaitText("Notifications")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        // Let the compositor present the settled Compose frame before capturing.
        android.os.SystemClock.sleep(300)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.externalCacheDir, "pulse-phase7-$name.png")
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        shell("cp ${file.absolutePath} /sdcard/Download/${file.name}")
    }

    private fun shell(cmd: String) =
        ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(cmd)).use {
            String(it.readBytes()).trim()
        }

    private fun assertSecure(expected: Boolean) {
        compose.waitForIdle()
        scenario!!.onActivity {
            assertEquals(
                expected,
                it.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0,
            )
        }
    }

    private fun createKey(label: String = "Reader") {
        tap("Create API key")
        field("Key name", label)
        tap("Save changes")
        awaitText("Your API key")
    }

    @Test fun profileCurrencyIsSearchableSavedAndRestored() {
        launch()
        openProfile()
        tap("Choose currency")
        compose.onNodeWithText("Search currencies").performTextReplacement("INR")
        hideKeyboard()
        compose.onNodeWithText("INR — Indian Rupee").performClick()
        screenshot("currency-picker")
        confirm("Save changes")
        awaitText("INR — Indian Rupee")
        assertEquals("INR", currency)
        assertEquals(1, currencyWrites)
        scenario!!.recreate()
        awaitText("INR — Indian Rupee")
        screenshot("currency-saved")
    }

    @Test fun profileTimeZoneIsSearchableSavedAndRestored() {
        launch()
        openProfile()
        tap("Change time zone")
        compose.onNodeWithText("Search time zones").performTextReplacement("New_York")
        hideKeyboard()
        compose.onNodeWithText("America/New York").performClick()
        screenshot("timezone-picker")
        confirm("Save changes")
        awaitText("America/New York")
        assertEquals("America/New_York", timezone)
        assertEquals(1, timezoneWrites)
        scenario!!.recreate()
        awaitText("America/New York")
        screenshot("timezone-saved")
    }

    @Test fun displayNameSaveUpdatesHomeAndUnsavedEditsSurviveRotation() {
        launch()
        openProfile()
        awaitText("Asha")
        screenshot("profile")
        tap("Edit display name")
        field("Display name", " 😀 Asha ")
        awaitText(" 😀 Asha ")
        scenario!!.recreate()
        awaitText(" 😀 Asha ")
        tap("Save changes")
        awaitText("😀 Asha")
        assertEquals(1, profileWrites)
        compose.onNodeWithText("Create API key").performScrollTo()
        screenshot("profile-integrations")
        tap("Home")
        awaitText("😀 Asha")
    }

    @Test fun confirmedMissingProfileOffersOptionalSetupOnlyOnceAndReusesEditor() {
        name = null
        launch()
        awaitText("Make Pulse yours")
        confirm("Not now")
        tap("You")
        tap("Home")
        compose.onNodeWithText("Make Pulse yours").assertDoesNotExist()
        openProfile()
        tap("Edit display name")
        field("Display name", "New rhythm")
        tap("Save changes")
        awaitText("New rhythm")
        tap("Home")
        awaitText("New rhythm")
    }

    @Test fun profileFetchFailureDoesNotTriggerOnboardingOrDisableAppearance() {
        profileFailure = true
        launch()
        compose.onNodeWithText("Make Pulse yours").assertDoesNotExist()
        openProfile()
        awaitText("Appearance")
        compose.onNodeWithText("Edit display name").assertIsEnabled()
        profileFailure =
            false
        compose.onNodeWithText("Refresh profile").assertDoesNotExist()
        compose.onNodeWithText("Refresh API keys").assertDoesNotExist()
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Appearance"))).performTouchInput { swipeDown() }
        awaitText("Asha")
    }

    @Test fun adaptiveNavigationOpensFeaturesAndGuardsUnsavedPassword() {
        launch()
        val rail = usesNavigationRail()
        val labels = listOf("Home", "Fitness", "Horizon", "Garage", "Meals") + if (rail) listOf("You") else emptyList()
        val items = labels.map { hasText(it) and isSelectable() }
        val bounds =
            items.map {
                compose
                    .onNode(it)
                    .apply { if (rail) performScrollTo() }
                    .assertIsDisplayed()
                    .getUnclippedBoundsInRoot()
            }
        bounds.zipWithNext().forEach { (first, next) ->
            if (rail) {
                assertEquals(first.left.value, next.left.value, 1f)
                assertTrue(first.bottom <= next.top)
            } else {
                assertEquals(first.top.value, next.top.value, 1f)
                assertTrue(first.right <= next.left)
            }
        }
        compose.onNodeWithText("Spaces").assertDoesNotExist()
        compose.onNode(items.first()).assertIsSelected()
        val actionLayouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText("Keep going", useUnmergedTree = true).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult,
        ) { it(actionLayouts) }
        assertEquals(1, actionLayouts.single().lineCount)
        if (!rail) {
            compose.onNode(hasText("You") and isSelectable()).assertDoesNotExist()
            val notifications = compose.onNodeWithContentDescription("Notifications").getUnclippedBoundsInRoot()
            val profile = compose.onNodeWithContentDescription("Profile").assertIsDisplayed().getUnclippedBoundsInRoot()
            assertEquals(notifications.top.value, profile.top.value, 1f)
            assertTrue(notifications.right <= profile.left)
        }
        screenshot("adaptive-navigation")
        items.drop(1).forEach { item ->
            compose.onNode(item).apply { if (rail) performScrollTo() }.performClick()
            compose.onNode(item).assertIsSelected()
            compose.onNodeWithText("Spaces").assertDoesNotExist()
            compose.onNodeWithContentDescription("Back to Spaces").assertDoesNotExist()
        }
        if (!rail) openProfile()
        awaitText("Account information")
        val viewport = compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Account information"))).getUnclippedBoundsInRoot()
        val window = compose.onRoot().getUnclippedBoundsInRoot()
        var safeTop = 0f
        var safeBottom = 0f
        scenario!!.onActivity { activity ->
            val safeInsets =
                checkNotNull(ViewCompat.getRootWindowInsets(activity.window.decorView))
                    .getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val density = activity.resources.displayMetrics.density
            safeTop = safeInsets.top / density
            safeBottom = safeInsets.bottom / density
        }
        assertEquals(window.top.value + safeTop, viewport.top.value, 1f)
        if (rail) {
            assertEquals(window.bottom.value - safeBottom, viewport.bottom.value, 1f)
        } else {
            assertTrue(viewport.bottom <= bounds.first().top)
        }
        screenshot("profile-safe-area")
        tap("Change password")
        field("Current password", "unsaved")
        hideKeyboard()
        compose.onNode(items[4]).apply { if (rail) performScrollTo() }.performClick()
        awaitText("Discard unsaved changes?")
        confirm("Cancel")
        compose.onNodeWithText("Current password").assertExists()
        compose.onNode(items[3]).apply { if (rail) performScrollTo() }.performClick()
        confirm("Discard changes")
        compose.onNode(items[3]).assertIsSelected()
        assertEquals(0, passwordWrites)
        scenario!!.recreate()
        compose.onNode(items[3]).assertIsSelected()
        compose.onNode(items[0]).apply { if (rail) performScrollTo() }.performClick()
        awaitText("Good to see you,")
        openNotifications()
        compose.onNode(items[0]).assertIsSelected()
        tap("Open space")
        awaitText("Your garage")
        compose.onNode(items[3]).assertIsSelected()
        compose.onNodeWithContentDescription("Back to notifications").performClick()
        awaitText("Notifications")
        compose.onNodeWithContentDescription("Back to Home").performClick()
        compose.onNode(items[0]).assertIsSelected()
        openNotifications()
        compose.onNode(items[0]).apply { if (rail) performScrollTo() }.performClick()
        awaitText("Good to see you,")
        compose.onNodeWithContentDescription("Back to Home").assertDoesNotExist()
    }

    @Test fun passwordFeedbackDuplicateProtectionClearingAndLogoutConfirmation() {
        launch()
        openProfile()
        tap("Change password")
        field("Current password", " current ")
        field("New password", " next password ")
        field("Confirm new password", "mismatch")
        scenario!!.recreate()
        tap("Save changes")
        assertEquals(0, passwordWrites)
        field("Confirm new password", " next password ")
        wrongPassword =
            true
        tap("Save changes")
        awaitText("The current password is incorrect. Please try again.")
        assertEquals(1, passwordWrites)
        wrongPassword =
            false
        tap("Save changes")
        awaitText("Password updated. Existing sessions remain signed in.")
        assertEquals(2, passwordWrites)
        val sent = Json.parseToJsonElement(passwordBody).jsonObject
        assertEquals(" current ", sent["current_password"]!!.jsonPrimitive.content)
        tap("Change password")
        awaitText("Current password")
        compose
            .onNodeWithText(
                "Current password",
            ).assert(
                SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.EditableText,
                    androidx.compose.ui.text
                        .AnnotatedString(""),
                ),
            )
        field("Current password", "unsaved")
        tap("Home")
        awaitText("Discard unsaved changes?")
        confirm("Cancel")
        assertSecure(true)
        tap("Log out")
        awaitText("Log out and discard edits?")
        confirm("Log out")
        awaitText("Welcome back")
        assertSecure(false)
    }

    @Test fun oneTimeTokenSurvivesRotationCopiesSensitivelyAndCanBeRevoked() {
        launch()
        openProfile()
        createKey()
        assertSecure(true)
        scenario!!.recreate()
        awaitText("Your API key")
        compose.onNodeWithText(token, substring = true).assertIsDisplayed()
        confirm("Copy token")
        awaitText("Token copied.", true)
        val clip =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getSystemService(ClipboardManager::class.java)
                .primaryClip
        assertEquals(token, clip?.getItemAt(0)?.text?.toString())
        assertTrue(clip!!.description.extras!!.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE))
        confirm("Close")
        assertSecure(false)
        awaitText("Reader")
        tap("Revoke Reader")
        awaitText("Revoke API key?")
        confirm("Revoke Reader")
        awaitText("API key revoked.")
        assertFalse(
            keys.values.any {
                it["name"]?.jsonPrimitive?.content ==
                    "Reader"
            },
        )
        assertEquals(1, keyWrites)
    }

    @Test fun lostKeyCreationRefreshesListAndBlocksBlindRetry() {
        launch()
        openProfile()
        loseKey = true
        createKeyLost()
        awaitText("I reviewed the server records")
        compose.onNodeWithText("Save changes").assertIsNotEnabled()
        assertEquals(1, keyWrites)
        compose.waitUntil(15_000) {
            runCatching { compose.onNodeWithText("I reviewed the server records").assertIsEnabled() }.isSuccess
        }
        tap("I reviewed the server records")
        tap("Cancel")
        confirm("Discard changes")
        awaitText("Lost reader")
    }

    private fun createKeyLost() {
        tap("Create API key")
        field("Key name", "Lost reader")
        tap("Save changes")
        awaitText("We couldn’t confirm the change.", true)
    }

    @Test fun dismissClearBeyondLimitAndReturnToHome() {
        launch()
        openNotifications()
        screenshot("notifications")
        compose.onNodeWithText("Vehicle care update").performTouchInput { swipeLeft() }
        awaitText("Notifications updated.")
        awaitText("Information only")
        compose.onNodeWithText("Vehicle care update").assertDoesNotExist()
        compose.onNodeWithContentDescription("Back to Home").performClick()
        compose.onNodeWithText("Vehicle care update").assertDoesNotExist()
        for (i in 3L..103L) {
            notifications[i] = notification(i, "Notice $i", null)
        }
        openNotifications()
        compose.onNodeWithContentDescription("Notification options").performClick()
        tap("Clear all")
        awaitText("This dismisses all notifications on the server, including any beyond the 100 displayed here. They cannot be restored.")
        confirm("Clear all")
        awaitText("All caught up")
        assertTrue(notifications.isEmpty())
        compose.onNodeWithContentDescription("Back to Home").performClick()
        awaitText("Good to see you,")
        compose.onNodeWithText("Recent notifications").assertDoesNotExist()
        compose.onNodeWithText("Your spaces").assertDoesNotExist()
    }

    @Test fun pullToRefreshLoadsNewNotificationsAndWorksWhenEmpty() {
        launch()
        openNotifications()
        awaitText("Vehicle care update")
        compose.onNodeWithText("Your latest updates").assertDoesNotExist()
        compose.onNodeWithText("Refresh notifications").assertDoesNotExist()
        compose.onNodeWithText("Clear all").assertDoesNotExist()
        notifications[3] = notification(3, "Fresh update", null)
        // Keep the refresh gesture away from the list's edges.
        compose.onNode(hasScrollAction()).performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.75f) }
        compose.waitUntil(20_000) {
            try {
                compose.onNode(hasScrollAction()).performScrollToNode(hasText("Fresh update"))
                compose.onAllNodesWithText("Fresh update").fetchSemanticsNodes().isNotEmpty()
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithText("Fresh update").assertIsDisplayed()

        compose.onNodeWithContentDescription("Notification options").performClick()
        tap("Clear all")
        confirm("Clear all")
        awaitText("All caught up")
        notifications[4] = notification(4, "After empty", null)
        compose.onNode(hasScrollAction()).performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.75f) }
        awaitText("After empty")
    }

    @Test fun safeRoutingSelectsGarageAndKeepsBackAndDoesNotDismiss() {
        launch()
        openNotifications()
        compose.onNodeWithText("Open space").assertDoesNotExist()
        compose.onNodeWithText("Information only").assert(hasClickAction().not())
        tap("Vehicle care update")
        awaitText("Your garage")
        compose
            .onNode(
                hasText("Garage") and isSelected(),
            ).assertExists()
        assertEquals(2, notifications.size)
        compose.onNodeWithContentDescription("Back to notifications").performClick()
        awaitText("Notifications")
        compose.onNodeWithText("Vehicle care update").assert(hasClickAction())
        compose.onNodeWithText("Open space").assertDoesNotExist()
    }

    @Test fun lostDismissalReconcilesWithoutAutomaticMutationRetry() {
        launch()
        openNotifications()
        loseDismiss = true
        compose.onNodeWithText("Vehicle care update").performTouchInput { swipeRight() }
        awaitText("I reviewed the server notifications")
        assertEquals(1, notifications.size)
        compose.onNodeWithText("Vehicle care update").assertDoesNotExist()
        tap("I reviewed the server notifications")
        screenshot("notification-recovery")
    }

    @Test fun sessionExpiryClosesTokenAndSensitiveAccountContent() {
        launch()
        openProfile()
        createKey()
        clock.offset = 7200
        scenario!!.moveToState(Lifecycle.State.CREATED)
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        awaitText("Welcome back")
        compose.onNodeWithText("Your API key").assertDoesNotExist()
        compose.onNodeWithText(token, substring = true).assertDoesNotExist()
        assertSecure(false)
    }

    @Test fun resizingAndLargeTextKeepAccountFormsAndNotificationsUsable() {
        val originalSize = shell("wm size").lineSequence().last().substringAfter(": ")
        val originalDensity = shell("wm density").lineSequence().last().substringAfter(": ")
        try {
            launch()
            openProfile()
            tap("Edit display name")
            field("Display name", "Resized name")
            awaitText("Resized name")
            shell("wm size 1440x900")
            shell("wm density 320")
            awaitText("Resized name")
            screenshot("landscape-editor")
            compose.onNodeWithText("Display name").performClick()
            compose.waitUntil(15_000) {
                var visible = false
                scenario!!.onActivity {
                    visible = ViewCompat.getRootWindowInsets(it.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
                }
                visible
            }
            compose.waitUntil(15_000) {
                val save = compose.onNodeWithText("Save changes").performScrollTo()
                val visible = Rect()
                scenario!!.onActivity { it.window.decorView.getWindowVisibleDisplayFrame(visible) }
                val bounds = save.fetchSemanticsNode().boundsInWindow
                bounds.height > 0 && bounds.top >= visible.top && bounds.bottom <= visible.bottom
            }
            screenshot("landscape-editor-keyboard")
            compose.onNodeWithText("Save changes").performTouchInput { click() }
            awaitText("Your account is up to date.")
            tap("Edit display name")
            shell("settings put system font_scale 2.0")
            shell("wm size 720x1600")
            awaitText("Resized name")
            screenshot("large-text-editor")
            tap("Save changes")
            awaitText("Resized name")
            tap("Home")
            openNotifications()
            screenshot("large-text-notifications")
            compose.onNode(hasScrollAction()).performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.75f) }
            awaitText("Vehicle care update")
        } catch (failure: Throwable) {
            screenshot("resize-failure")
            throw failure
        } finally {
            shell("settings put system font_scale 1.0")
            shell("wm size $originalSize")
            shell("wm density $originalDensity")
        }
    }

    @Test fun onboardingSetupOpensTheSharedEditor() {
        name = null
        launch()
        confirm("Set display name")
        awaitText("Display name")
        field("Display name", "Morning rhythm")
        tap("Save changes")
        awaitText("Morning rhythm")
        tap("Home")
        awaitText("Morning rhythm")
        assertEquals(1, profileWrites)
    }

    @Test fun deletedVehicleTargetReturnsToNativeCollectionWithUsefulFeedback() {
        notifications[1] = notification(1, "Deleted vehicle", "/garage/999")
        launch()
        openNotifications()
        tap("Deleted vehicle")
        awaitText("This record is no longer available. Refresh to see the latest updates.")
        awaitText("Your garage")
        scenario!!.recreate()
        awaitText("Your garage")
        compose.onNodeWithText("This record is no longer available. Refresh to see the latest updates.").assertDoesNotExist()
        assertEquals(2, notifications.size)
    }
}
