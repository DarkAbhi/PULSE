package dev.abhishekan.pulse
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.data.EncryptedSessionStore
import dev.abhishekan.pulse.core.data.PulseAuthRepository
import dev.abhishekan.pulse.core.data.PulseDashboardRepository
import dev.abhishekan.pulse.core.data.PulseMealPlanRepository
import dev.abhishekan.pulse.core.data.di.dataModule
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository
import dev.abhishekan.pulse.core.network.MealPlanApi
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.pulseHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.cookies
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.http.parameters
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
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
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Isolated HTTP fixtures exercise production network, persistence, repositories and navigation. */
class MealPlanDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var overrides: Module
    private lateinit var client: HttpClient
    private lateinit var store: EncryptedSessionStore
    private val times = linkedMapOf<Long, JsonObject>()
    private val meals = linkedMapOf<Long, JsonObject>()
    private val day = LocalDate.now(ZoneId.of("Asia/Kolkata"))
    private var next = 100L

    @Volatile private var offline = false

    @Volatile private var loseSave = false

    @Volatile private var failToggle = false
    private var mutations = 0

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    private fun joined(value: JsonObject): JsonObject {
        val time = times[value["meal_time_id"]?.jsonPrimitive?.longOrNull]
        return JsonObject(
            value +
                if (time ==
                    null
                ) {
                    emptyMap()
                } else {
                    mapOf(
                        "meal_time_name" to time.getValue("name"),
                        "start_time" to time.getValue("start_time"),
                        "end_time" to time.getValue("end_time"),
                    )
                },
        )
    }

    @Before fun setup() {
        store = GlobalContext.get().get()
        runBlocking { store.clear() }
        times[1] =
            obj(
                """{"id":1,"name":"Lunch","start_time":"12:00","end_time":"14:30","is_default":true}""",
            )
        times[2] =
            obj(
                """{"id":2,"name":"Evening bowl","start_time":"18:00","end_time":"19:00","is_default":false}""",
            )
        meals[10] =
            obj("""{"id":10,"date":"$day","name":"Soup","meal_time_id":1,"is_consumed":false}""")
        val config = NetworkConfig("https://meal-fixture.test/api/", false)
        val clock = Clock.systemUTC()
        val cookies = PersistentSessionCookies(store, config, clock)
        client =
            pulseHttpClient(
                config,
                MockEngine { request ->
                    val path = request.url.encodedPath.removePrefix("/api/")
                    if (path ==
                        "auth/login"
                    ) {
                        respond(
                            """{"username":"meal-fixture"}""",
                            HttpStatusCode.OK,
                            headersOf(
                                HttpHeaders.ContentType to listOf("application/json"),
                                HttpHeaders.SetCookie to
                                    listOf(
                                        "life_session=meal-test-only; Path=/; Max-Age=3600; Secure; HttpOnly",
                                    ),
                            ),
                        )
                    } else {
                        if (offline) throw IOException("Offline fixture")
                        var status = HttpStatusCode.OK
                        val body =
                            if (request.method == HttpMethod.Get) {
                                when (path) {
                                    "auth/session" -> """{"username":"meal-fixture"}"""
                                    "profile" -> """{"has_profile":true,"name":"Asha"}"""
                                    "workout/today" -> """{"visited":false}"""
                                    "notifications" -> "[]"
                                    "horizon" ->
                                        """{"currency":"₹","remaining_amount":100,"base_amount":200,"total_deductions":100,"total_transactions":0}"""
                                    "meal-times" -> JsonArray(times.values.toList()).toString()
                                    "meal-plans" -> {
                                        val start = LocalDate.parse(request.url.parameters["start_date"]!!)
                                        val end = LocalDate.parse(request.url.parameters["end_date"]!!)
                                        check(end == start.plusDays(6))
                                        JsonArray(
                                            meals.values
                                                .filter {
                                                    LocalDate.parse(
                                                        it.getValue("date").jsonPrimitive.content,
                                                    ) in
                                                        start..end
                                                }.map(::joined),
                                        ).toString()
                                    }
                                    else -> error("Unexpected read $path")
                                }
                            } else {
                                mutations++
                                val raw = (request.body as? TextContent)?.text ?: "{}"
                                val input = obj(raw)
                                val parts = path.split('/')
                                val id = parts.getOrNull(1)?.toLongOrNull()
                                var result = "{}"
                                when {
                                    path == "meal-times" -> {
                                        val key = next++
                                        times[key] =
                                            JsonObject(
                                                input +
                                                    mapOf(
                                                        "id" to JsonPrimitive(key),
                                                        "is_default" to JsonPrimitive(false),
                                                    ),
                                            )
                                    }
                                    parts[0] == "meal-times" -> {
                                        check(
                                            times[id]?.getValue("is_default")?.jsonPrimitive?.boolean ==
                                                false,
                                        )
                                        times.remove(id)
                                        meals.replaceAll { _, v ->
                                            if (v["meal_time_id"]?.jsonPrimitive?.longOrNull ==
                                                id
                                            ) {
                                                JsonObject(
                                                    joined(v).filterKeys {
                                                        it !in
                                                            setOf("meal_time_id", "meal_time_name")
                                                    },
                                                )
                                            } else {
                                                v
                                            }
                                        }
                                    }
                                    path == "meal-plans" -> {
                                        val key = next++
                                        meals[key] =
                                            JsonObject(
                                                input +
                                                    mapOf(
                                                        "id" to JsonPrimitive(key),
                                                        "is_consumed" to JsonPrimitive(false),
                                                    ),
                                            )
                                    }
                                    parts.last() == "consumed" ->
                                        if (failToggle) {
                                            status =
                                                HttpStatusCode.InternalServerError
                                        } else {
                                            meals[id!!] =
                                                JsonObject(meals.getValue(id) + input)
                                            result =
                                                joined(meals.getValue(id)).toString()
                                        }
                                    request.method == HttpMethod.Delete -> meals.remove(id)
                                    else -> {
                                        check(request.method == HttpMethod.Patch)
                                        meals[id!!] =
                                            JsonObject(meals.getValue(id) + input)
                                    }
                                }
                                if (loseSave) {
                                    loseSave = false
                                    offline = true
                                    throw IOException("Lost after commit")
                                }
                                result
                            }
                        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                },
                cookies,
            )
        val api = PulseApi(client)
        val auth = PulseAuthRepository(api, cookies, clock)
        val repo = PulseMealPlanRepository(MealPlanApi(client), auth)
        overrides =
            module {
                single<AuthRepository> { auth }
                single<DashboardRepository> { PulseDashboardRepository(api, auth) }
                single<MealPlanRepository> { repo }
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

    private fun awaitText(text: String) {
        try {
            compose.waitUntil(20_000) {
                runCatching {
                    compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
                }.getOrDefault(false)
            }
        } catch (
            error: AssertionError,
        ) {
            throw AssertionError("Missing text: $text", error)
        }
    }

    private fun tap(text: String) {
        if (text in
            listOf("Home", "Spaces", "You")
        ) {
            scenario?.onActivity {
                it
                    .getSystemService(
                        android.view.inputmethod.InputMethodManager::class.java,
                    ).hideSoftInputFromWindow(it.window.decorView.windowToken, 0)
            }
        }
        awaitText(text)
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText(text) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose
            .onAllNodesWithText(text)[0]
            .apply {
                if (text !in
                    listOf("Home", "Spaces", "You")
                ) {
                    performScrollTo()
                }
            }.assertIsDisplayed()
            .performClick()
    }

    private fun field(
        label: String,
        value: String,
    ) {
        compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)
    }

    private fun confirm(text: String) {
        val m =
            hasText(text) and hasAnyAncestor(isDialog())
        compose.waitUntil(20_000) { compose.onAllNodes(m).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(m).performClick()
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
        val file = File(context.externalCacheDir, "pulse-phase6-$name.png")
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

    private fun launchPlanner() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitText("Welcome back")
        field("Username", "meal-fixture")
        field("Password", "test-only")
        tap("Continue")
        awaitText("Asha")
        tap("Explore Meal Plan")
        awaitText("Soup")
    }

    private fun pickTime(
        current: String,
        option: String,
    ) {
        tap("Meal time: $current")
        tap(option)
    }

    @Test fun weeklyPlannerMealCrudConsumptionAndEmptyWeeks() {
        launchPlanner()
        screenshot("planner")
        tap("Next week")
        awaitText("A little room to plan")
        tap("Previous week")
        awaitText("Soup")
        tap("Today")
        tap("Add meal")
        field("Meal name", "Rice bowl")
        pickTime("No meal time", "Lunch · 12:00–14:30")
        tap("Meal date: $day")
        screenshot("date-picker", true)
        confirm("Choose date")
        tap("Save meal")
        awaitText("Rice bowl")
        assertEquals(1, mutations)
        tap("Planned · Rice bowl")
        awaitText("✓ Consumed · Rice bowl")
        assertTrue(
            meals.values
                .single {
                    it.getValue("name").jsonPrimitive.content ==
                        "Rice bowl"
                }.getValue("is_consumed")
                .jsonPrimitive.boolean,
        )
        tap("✓ Consumed · Rice bowl")
        awaitText("Planned · Rice bowl")
        tap("Edit Rice bowl")
        field("Meal name", "Rice and beans")
        pickTime("Lunch · 12:00–14:30", "Evening bowl · 18:00–19:00")
        tap("Save meal")
        awaitText("Rice and beans")
        screenshot("meals")
        tap("Delete Rice and beans")
        screenshot("deletion", true)
        confirm("Delete")
        compose.waitUntil(20_000) {
            meals.size ==
                1
        }
        awaitText("Soup")
        assertEquals(1, meals.size)
    }

    @Test fun customMealTimesAndDeletionKeepAssociatedMeals() {
        launchPlanner()
        tap("Meal times")
        awaitText("Default · protected")
        assertTrue(compose.onAllNodesWithText("Delete time Lunch").fetchSemanticsNodes().isEmpty())
        tap("Add custom meal time")
        field("Meal time name", "Night snack")
        tap("Start time: 16:00")
        screenshot("time-picker", true)
        confirm("Choose time")
        tap("End time: 17:00")
        confirm("Choose time")
        tap("Save meal time")
        awaitText("Night snack")
        screenshot("times")
        tap("Back to planner")
        tap("Add meal")
        field("Meal name", "Oats")
        pickTime("No meal time", "Evening bowl · 18:00–19:00")
        tap("Save meal")
        awaitText("Oats")
        tap("Meal times")
        tap("Delete time Evening bowl")
        confirm("Delete")
        tap("Back to planner")
        awaitText("Other meals")
        awaitText("Oats")
        assertEquals(2, meals.size)
        assertFalse(times.containsKey(2))
        tap("Meal times")
        tap("Delete time Night snack")
        confirm("Delete")
        compose.waitUntil(20_000) {
            times.size ==
                1
        }
        assertEquals(1, times.size)
    }

    @Test fun failedToggleKeepsConfirmedValueAndRequiresReview() {
        launchPlanner()
        failToggle = true
        tap("Planned · Soup")
        awaitText("Review before another change")
        awaitText("Planned · Soup")
        assertFalse(
            meals
                .getValue(10)
                .getValue("is_consumed")
                .jsonPrimitive.boolean,
        )
        screenshot("failed-toggle")
        tap("I reviewed the server records")
        failToggle = false
        tap("Planned · Soup")
        awaitText("✓ Consumed · Soup")
        assertEquals(2, mutations)
    }

    @Test fun lostSaveRefreshesServerRecordsBeforeAnotherAttempt() {
        launchPlanner()
        tap("Add meal")
        field("Meal name", "Lost bowl")
        loseSave = true
        tap("Save meal")
        awaitText("Review before another change")
        compose.onNodeWithText("Save meal").assertIsNotEnabled()
        assertEquals(1, mutations)
        compose.onNodeWithText("I reviewed the server records").assertIsNotEnabled()
        offline = false
        tap("Refresh server records")
        tap("View server records")
        awaitText("Lost bowl")
        screenshot("recovery")
        tap("I reviewed the server records")
        tap("Resume unsaved edits")
        tap("Cancel")
        confirm("Discard")
        assertEquals(1, mutations)
    }

    @Test fun draftRecreationTabsLogoutKeyboardAndDiscard() {
        launchPlanner()
        tap("Add meal")
        field("Meal name", "Unsaved bowl")
        scenario!!.recreate()
        awaitText("Unsaved bowl")
        tap("Home")
        tap("Spaces")
        awaitText("Unsaved bowl")
        tap("You")
        tap("Log out")
        awaitText("Log out and discard edits?")
        confirm("Cancel")
        tap("Spaces")
        awaitText("Unsaved bowl")
        field("Meal name", "")
        compose.onNodeWithText("Meal name").performImeAction()
        compose.waitForIdle()
        assertEquals(0, mutations)
        field("Meal name", "Unsaved bowl")
        screenshot("draft")
        tap("Cancel")
        confirm("Discard")
        awaitText("Soup")
        compose.onNodeWithText("Planned · Soup").performScrollTo().assertIsDisplayed()
        tap("Home")
        tap("Spaces")
        awaitText("Soup")
        compose.onNodeWithText("Planned · Soup").assertIsDisplayed()
    }

    @Test fun liveResizePreservesEditorAndExpandedBoardIsReadable() {
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
            launchPlanner()
            tap("Add meal")
            field("Meal name", "Resized meal")
            shell("wm size 2560x1800")
            shell("wm density 320")
            awaitText("Resized meal")
            screenshot("resized-editor")
            tap("Save meal")
            awaitText("Resized meal")
            val largeText =
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext.resources.configuration.fontScale >
                    1.2f
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText("View Mon").fetchSemanticsNodes().isNotEmpty() !=
                    largeText
            }
            screenshot("board")
            assertEquals(1, mutations)
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
            compose.waitForIdle()
        }
    }
}
