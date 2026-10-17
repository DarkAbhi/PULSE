package dev.abhishekan.pulse

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.data.*
import dev.abhishekan.pulse.core.data.di.dataModule
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.SessionState
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.network.*
import dev.abhishekan.pulse.core.ui.message
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import java.io.File
import java.time.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module

/** Test-only backend responses pass through the production client, cookie store and repositories. */
class AuthNavigationDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var overrides: Module
    private lateinit var store: EncryptedSessionStore
    private lateinit var auth: PulseAuthRepository
    private val clients = mutableListOf<HttpClient>()
    private var failHorizon = false
    private var offline = false
    private var rejectLogin = false
    private val clock = object : Clock() {
        var offset = 0L
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant() = Instant.now().plusSeconds(offset)
    }

    @Before fun setup() {
        store = GlobalContext.get().get()
        runBlocking { store.clear() }
        configureRepositories()
    }
    @After fun cleanup() {
        scenario?.close()
        unloadKoinModules(overrides)
        loadKoinModules(dataModule)
        clients.forEach { it.close() }
        runBlocking { store.clear() }
    }

    private fun configureRepositories() {
        val config = NetworkConfig("https://fixture.example.test/api/", false)
        val cookies = PersistentSessionCookies(store, config, clock)
        val engine = MockEngine { request ->
            var status = HttpStatusCode.OK
            var cookie: String? = null
            val text = when (request.url.encodedPath) {
                "/api/auth/login" -> {
                    if (rejectLogin) status = HttpStatusCode.Unauthorized
                    else cookie = "life_session=android-test-fixture; Path=/; Max-Age=3600; Secure; HttpOnly"
                    """{"username":"fixture-user"}"""
                }
                "/api/auth/session" -> {
                    if (offline) throw java.io.IOException("Fixture offline")
                    """{"username":"fixture-user"}"""
                }
                "/api/auth/logout" -> {
                    cookie = "life_session=; Path=/; Max-Age=0; Secure; HttpOnly"; "{}"
                }
                "/api/profile" -> """{"has_profile":true,"name":"Asha"}"""
                "/api/workout/today" -> """{"visited":true}"""
                "/api/horizon" -> {
                    if (failHorizon) status = HttpStatusCode.ServiceUnavailable
                    """{"currency":"₹","base_amount":50000.00,"remaining_amount":32000.125,"total_deductions":18000.00}"""
                }
                "/api/notifications" -> """[{"id":1,"title":"Vehicle care update","body":null,"created_at":"2026-10-16T12:00:00Z"}]"""
                else -> error("Unexpected test request")
            }
            respond(text, status, Headers.build {
                append(HttpHeaders.ContentType, "application/json")
                cookie?.let { append(HttpHeaders.SetCookie, it) }
            })
        }
        val client = pulseHttpClient(config, engine, cookies).also { clients += it }
        val api = PulseApi(client)
        auth = PulseAuthRepository(api, cookies, clock)
        val dashboard = PulseDashboardRepository(api, auth)
        overrides = module {
            single<AuthRepository> { auth }
            single<DashboardRepository> { dashboard }
        }
        loadKoinModules(overrides)
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = File(instrumentation.targetContext.externalCacheDir, "pulse-phase2-$name.png")
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        image.outputStream().use { Assert.assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "cp ${image.absolutePath} /sdcard/Download/${image.name}")).use { it.readBytes() }
    }

    private fun launch() { scenario = ActivityScenario.launch(MainActivity::class.java) }
    private fun awaitText(text: String) {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun login() {
        awaitText("Welcome back")
        compose.onNodeWithText("Username").performScrollTo().performTextInput("fixture-user")
        compose.onNodeWithText("Password").performScrollTo().performTextInput("test-only-password")
        compose.onNodeWithText("Continue").performScrollTo().performClick()
        awaitText("Asha")
    }

    @Test fun loginReachesRealDashboardAndLogoutRemovesAuthenticatedNavigation() {
        launch(); login()
        compose.onNodeWithText("₹ 32000.125").performScrollTo().assertIsDisplayed()
        screenshot("home")
        val notifications = compose.onNodeWithContentDescription("Notifications")
        val target = notifications.getUnclippedBoundsInRoot()
        Assert.assertTrue((target.right - target.left).value >= 48f && (target.bottom - target.top).value >= 48f)
        notifications.assertHasClickAction().performClick()
        compose.onNodeWithText("Vehicle care update").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to Home").performClick()
        compose.onNodeWithText("You", substring = false).performClick()
        compose.onNodeWithText("Appearance").assertIsDisplayed()
        compose.onNodeWithText("Log out").performScrollTo().performClick()
        awaitText("Welcome back")
        compose.onNodeWithText("Asha").assertDoesNotExist()
        Assert.assertTrue(auth.session.value is SessionState.SignedOut)
        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(10_000) { scenario!!.state == Lifecycle.State.DESTROYED }
    }

    @Test fun recreationKeepsTransientLoginStateAndSessionRestoresFromDisk() {
        launch(); awaitText("Welcome back")
        compose.onNodeWithText("Username").performScrollTo().performTextInput("fixture-user")
        compose.onNodeWithText("Password").performScrollTo().performTextInput("test-only-password")
        scenario!!.recreate()
        compose.onNodeWithText("fixture-user").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Password").performScrollTo().performImeAction()
        awaitText("Asha")
        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(10_000) { scenario!!.state == Lifecycle.State.DESTROYED }
        scenario = null
        unloadKoinModules(overrides)
        configureRepositories()
        launch(); awaitText("Asha")
        compose.onNodeWithText("Welcome back").assertDoesNotExist()
        runBlocking { clock.offset = 3601; auth.checkExpiry() }
        awaitText("Welcome back")
        compose.onNodeWithText("Asha").assertDoesNotExist()
    }

    @Test fun partialFailureLeavesOtherSectionsAndRetryRecoversOnlyHorizon() {
        failHorizon = true
        launch(); login()
        compose.onNodeWithText("Couldn’t load this section").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Workout recorded").performScrollTo().assertIsDisplayed()
        failHorizon = false
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        awaitText("₹ 32000.125")
        compose.onNodeWithText("₹ 32000.125").performScrollTo().assertIsDisplayed()
    }

    @Test fun offlineRestorationKeepsTheSessionAndOffersRetry() {
        launch(); login()
        scenario!!.close(); scenario = null
        unloadKoinModules(overrides)
        offline = true
        configureRepositories()
        launch(); awaitText("We couldn’t open your session")
        Assert.assertNotNull(runBlocking { store.read() })
        compose.onNodeWithText("Welcome back").assertDoesNotExist()
        offline = false
        compose.onNodeWithText("Try again").performClick()
        awaitText("Asha")
    }

    @Test fun invalidCredentialsDoNotRevealAuthenticatedTabs() {
        rejectLogin = true
        launch(); awaitText("Welcome back")
        compose.onNodeWithText("Username").performScrollTo().performTextInput("fixture-user")
        compose.onNodeWithText("Username").performImeAction()
        compose.onNodeWithText("Password").assertIsFocused().performTextInput("wrong-password")
        compose.onNodeWithText("Password").performImeAction()
        awaitText(Failure.InvalidCredentials.message)
        screenshot("login-error")
        compose.onNodeWithText("Home", substring = false).assertDoesNotExist()
    }
}
