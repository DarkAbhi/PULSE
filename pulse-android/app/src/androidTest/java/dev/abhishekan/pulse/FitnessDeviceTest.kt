package dev.abhishekan.pulse
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.data.EncryptedSessionStore
import dev.abhishekan.pulse.core.data.PulseAuthRepository
import dev.abhishekan.pulse.core.data.PulseDashboardRepository
import dev.abhishekan.pulse.core.data.PulseFitnessRepository
import dev.abhishekan.pulse.core.data.di.dataModule
import dev.abhishekan.pulse.core.domain.model.FitnessDates
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
import dev.abhishekan.pulse.core.network.FitnessApi
import dev.abhishekan.pulse.core.network.NetworkConfig
import dev.abhishekan.pulse.core.network.PersistentSessionCookies
import dev.abhishekan.pulse.core.network.PulseApi
import dev.abhishekan.pulse.core.network.pulseHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.After
import org.junit.Assert.assertEquals
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Test-only server fixtures exercise the actual HTTP, persistence, repositories, use cases and UI. */
class FitnessDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var overrides: Module
    private lateinit var store: EncryptedSessionStore
    private lateinit var client: HttpClient
    private lateinit var backend: Backend

    private class Backend {
        data class Visit(
            val id: Long,
            val instant: Instant,
            val imported: Boolean = false,
        )

        val today = LocalDate.now(FitnessDates.zone)
        val visits = linkedMapOf<Long, Visit>()
        val exercises = mutableMapOf<Long, MutableList<JsonObject>>()
        val imports = mutableSetOf<Long>()
        var nextVisit = 100L
        var nextExercise = 1000L

        @Volatile var offlineReads = false

        @Volatile var loseSave = false

        @Volatile var loseRecord = false

        @Volatile var saves = 0

        @Volatile var records = 0

        @Volatile var reads = 0

        fun importedVisit() {
            visits[7] =
                Visit(
                    7,
                    today
                        .minusDays(1)
                        .atTime(7, 0)
                        .atZone(FitnessDates.zone)
                        .toInstant(),
                    true,
                )
            imports +=
                7
        }

        private fun catalog() =
            buildJsonObject {
                put("id", "fixture-squat")
                put("name", "Fixture barbell squat")
                put("equipment", "barbell")
                putJsonObject("data") {
                    putJsonArray("primaryMuscles") { add("quadriceps") }
                    putJsonArray("instructions") { add("Use a controlled movement.") }
                }
            }

        data class Reply(
            val body: String,
            val status: HttpStatusCode = HttpStatusCode.OK,
            val login: Boolean = false,
        )

        @Synchronized fun reply(request: HttpRequestData): Reply {
            val path = request.url.encodedPath.removePrefix("/api/")
            val method = request.method
            val body by lazy {
                Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            }
            if (method == HttpMethod.Get &&
                path.startsWith("fitness") ||
                method == HttpMethod.Get &&
                path.startsWith("gym-visits")
            ) {
                reads++
                if (offlineReads) throw IOException("Test fixture offline")
            }
            return when {
                path == "auth/login" -> Reply("""{"username":"fitness-fixture"}""", login = true)
                path == "auth/session" -> Reply("""{"username":"fitness-fixture"}""")
                path == "auth/logout" -> Reply("{}")
                path == "profile" -> Reply("""{"has_profile":true,"name":"Asha"}""")
                path == "horizon" ->
                    Reply(
                        """{"currency":"INR","base_amount":100,"remaining_amount":80,"total_deductions":20}""",
                    )
                path == "notifications" -> Reply("[]")
                path == "workout/today" && method == HttpMethod.Get ->
                    Reply(
                        """{"visited":${visits.values.any {
                            it.instant.atZone(FitnessDates.zone).toLocalDate() == today
                        }}}""",
                    )
                path == "fitness/overview" ->
                    Reply(
                        buildJsonObject {
                            put("today", today.toString())
                            putJsonArray("cards") {
                                add(
                                    buildJsonObject {
                                        put("id", "workouts")
                                        put("label", "Total workouts")
                                        put("value", visits.size.toString())
                                        put("subtitle", "test-server total")
                                    },
                                )
                            }
                            putJsonObject("calendar_workouts") { }
                        }.toString(),
                    )
                path == "gym-visits" && method == HttpMethod.Get ->
                    Reply(
                        buildJsonArray {
                            visits.values
                                .sortedByDescending {
                                    it.instant
                                }.forEach { visit ->
                                    add(
                                        buildJsonObject {
                                            put("id", visit.id)
                                            put("created_at", visit.instant.toString())
                                        },
                                    )
                                }
                        }.toString(),
                    )
                (path == "workout/today" || path == "gym-visits") && method == HttpMethod.Post -> {
                    records++
                    val date =
                        if (path ==
                            "workout/today"
                        ) {
                            today
                        } else {
                            LocalDate.parse(body["date"]!!.jsonPrimitive.content)
                        }
                    val id = nextVisit++
                    visits[id] =
                        Visit(
                            id,
                            if (date ==
                                today
                            ) {
                                Instant.now()
                            } else {
                                date.atStartOfDay(FitnessDates.zone).toInstant()
                            },
                        )
                    if (loseRecord) {
                        loseRecord = false
                        offlineReads = true
                        throw IOException("Response lost after server commit")
                    }
                    Reply("""{"id":$id}""", HttpStatusCode.Created)
                }
                path.startsWith("fitness-workouts/") -> {
                    val visit =
                        visits[path.substringAfterLast('/').toLong()]
                            ?: return Reply("{}", HttpStatusCode.NotFound)
                    Reply(
                        buildJsonObject {
                            put("id", visit.id)
                            put("created_at", visit.instant.toString())
                            put("synced_from_apple_watch", visit.imported)
                            if (visit.imported) {
                                put("start_time", visit.instant.toString())
                                put("end_time", visit.instant.plusSeconds(3600).toString())
                                put("duration_seconds", 3600)
                                put("calories_burned", 420.5)
                            }
                        }.toString(),
                    )
                }
                path.startsWith("exercise-catalog") ->
                    Reply(
                        buildJsonObject {
                            putJsonArray("candidates") {
                                if (request.url.parameters["q"].orEmpty().contains(
                                        "squat",
                                        true,
                                    )
                                ) {
                                    add(catalog())
                                }
                            }
                        }.toString(),
                    )
                path.startsWith("gym-visits/") -> {
                    val parts = path.split('/')
                    val visitId = parts[1].toLong()
                    if (!visits.containsKey(visitId)) return Reply("{}", HttpStatusCode.NotFound)
                    if (parts.size == 2 && method == HttpMethod.Delete) {
                        visits.remove(visitId)
                        exercises.remove(visitId)
                        return Reply("", HttpStatusCode.NoContent)
                    }
                    val list = exercises.getOrPut(visitId) { mutableListOf() }
                    when (method) {
                        HttpMethod.Get -> Reply(JsonArray(list.toList()).toString())
                        HttpMethod.Delete -> {
                            list.removeAll {
                                it["id"]!!.jsonPrimitive.long ==
                                    parts.last().toLong()
                            }
                            Reply("", HttpStatusCode.NoContent)
                        }
                        else -> {
                            saves++
                            val exerciseId =
                                if (method ==
                                    HttpMethod.Put
                                ) {
                                    parts.last().toLong()
                                } else {
                                    nextExercise++
                                }
                            val exercise =
                                buildJsonObject {
                                    put("id", exerciseId)
                                    put("name", body["name"]!!)
                                    put("exercise_catalog_id", body["exercise_catalog_id"]!!)
                                    if (body["exercise_catalog_id"] !=
                                        JsonNull
                                    ) {
                                        put("catalog_exercise", catalog())
                                    }
                                    putJsonArray("sets") {
                                        body["sets"]!!.jsonArray.forEachIndexed { i, set ->
                                            add(
                                                buildJsonObject {
                                                    put("set_number", i + 1)
                                                    put("reps", set.jsonObject["reps"]!!)
                                                    put("weight", set.jsonObject["weight"]!!)
                                                },
                                            )
                                        }
                                    }
                                }
                            list.removeAll { it["id"]!!.jsonPrimitive.long == exerciseId }
                            list +=
                                exercise
                            if (loseSave) {
                                loseSave = false
                                throw IOException("Response lost after server commit")
                            }
                            Reply(
                                exercise.toString(),
                                if (method ==
                                    HttpMethod.Post
                                ) {
                                    HttpStatusCode.Created
                                } else {
                                    HttpStatusCode.OK
                                },
                            )
                        }
                    }
                }
                else -> error("Unexpected test request $method $path")
            }
        }
    }

    @Before fun setup() {
        store = GlobalContext.get().get()
        runBlocking { store.clear() }
        backend = Backend()
        val config = NetworkConfig("https://fitness-fixture.example.test/api/", false)
        val clock = Clock.systemUTC()
        val cookies = PersistentSessionCookies(store, config, clock)
        client =
            pulseHttpClient(
                config,
                MockEngine { request ->
                    val result = backend.reply(request)
                    respond(
                        result.body,
                        result.status,
                        Headers.build {
                            append(HttpHeaders.ContentType, "application/json")
                            if (result.login) {
                                append(
                                    HttpHeaders.SetCookie,
                                    "life_session=fitness-test-only; Path=/; Max-Age=3600; Secure; HttpOnly",
                                )
                            }
                        },
                    )
                },
                cookies,
            )
        val api = PulseApi(client)
        val auth = PulseAuthRepository(api, cookies, clock)
        val dashboard = PulseDashboardRepository(api, auth)
        val fitness = PulseFitnessRepository(FitnessApi(client), auth)
        overrides =
            module {
                single<AuthRepository> { auth }
                single<DashboardRepository> { dashboard }
                single<dev.abhishekan.pulse.core.domain.repository.ProfileRepository> {
                    dev.abhishekan.pulse.core.data
                        .PulseProfileRepository(api, auth, dashboard)
                }
                single<dev.abhishekan.pulse.core.domain.repository.NotificationsRepository> {
                    dev.abhishekan.pulse.core.data
                        .PulseNotificationsRepository(api, auth, dashboard)
                }
                single<FitnessRepository> { fitness }
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
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(text: String) {
        if (text == "You") {
            compose.openProfileFromNavigation()
            return
        }
        if (text in listOf("Previous month", "Next month", "Choose date")) {
            compose.onNodeWithContentDescription(text).performScrollTo().performClick()
            return
        }
        awaitText(text)
        val node = compose.onNode(hasText(text, substring = false) and hasClickAction())
        if (text !in listOf("Home", "Fitness", "Horizon", "Garage", "Meals") || usesNavigationRail()) node.performScrollTo()
        node.performClick()
    }

    private fun selectCalendarDate(description: String) {
        val date = compose.onNodeWithContentDescription(description)
        val content =
            compose.onNode(
                hasScrollAction() and hasAnyDescendant(hasText("Workouts for this day")),
            )
        val offset =
            (date.getUnclippedBoundsInRoot().top - content.getUnclippedBoundsInRoot().top).value *
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext.resources.displayMetrics.density
        content.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) {
            it(0f, offset)
        }
        date.performScrollTo().performClick()
    }

    private fun confirm(text: String) {
        compose.onNode(hasText(text) and hasAnyAncestor(isDialog())).performClick()
    }

    private fun launchFitness() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitText("Welcome back")
        compose.onNodeWithText("Username").performScrollTo().performTextInput("fitness-fixture")
        compose.onNodeWithText("Password").performScrollTo().performTextInput("test-only-password")
        tap("Continue")
        awaitText("Asha")
        tap("Fitness")
        awaitText("Add workout")
    }

    private fun recordAndOpen() {
        tap("Add workout")
        tap("Workout")
        awaitText("Add exercise")
    }

    private fun enterOwnExercise(name: String = "Custom mobility") {
        tap("Add exercise")
        compose.onNodeWithText("Exercise name").performScrollTo().performTextInput(name)
        compose.onNodeWithText("Reps, set 1").performScrollTo().performTextInput("12")
        compose.onNodeWithText("Weight (kg), set 1").performScrollTo().performTextInput("20.5")
    }

    private fun screenshot(
        name: String,
        dialog: Boolean = false,
    ) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = File(instrumentation.targetContext.externalCacheDir, "pulse-phase3-$name.png")
        val bitmap =
            (
                if (dialog) {
                    compose.onNode(
                        isDialog(),
                    )
                } else {
                    compose.onRoot()
                }
            ).captureToImage().asAndroidBitmap()
        image.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        ParcelFileDescriptor
            .AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand(
                    "cp ${image.absolutePath} /sdcard/Download/${image.name}",
                ),
            ).use { it.readBytes() }
    }

    @Test fun pullToRefreshLoadsUpdatedActivity() {
        launchFitness()
        compose.onNodeWithText("Refresh activity").assertDoesNotExist()
        compose.runOnIdle {
            backend.visits[100] =
                Backend.Visit(
                    100,
                    backend.today
                        .atTime(12, 0)
                        .atZone(FitnessDates.zone)
                        .toInstant(),
                )
        }
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Workouts for this day"))).performTouchInput { swipeDown() }
        awaitText("Workout")
    }

    @Test fun calendarAndExerciseCrudRefreshHomeWithoutRestart() {
        launchFitness()
        tap("Previous month")
        tap("Today")
        val monthHeading = compose.onNodeWithContentDescription(backend.today.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
        monthHeading.assertIsDisplayed()
        val monthBounds = monthHeading.getUnclippedBoundsInRoot()
        listOf("Previous month", "Next month", "Choose date").forEach { description ->
            val control = compose.onNodeWithContentDescription(description)
            control.assertIsDisplayed()
            val bounds = control.getUnclippedBoundsInRoot()
            assertTrue(kotlin.math.abs((bounds.top + bounds.bottom).value / 2 - (monthBounds.top + monthBounds.bottom).value / 2) < 2f)
        }
        val label = "${backend.today.format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"),
        )}, today, 0 workouts recorded"
        val day = compose.onNodeWithContentDescription(label)
        day.performScrollTo().assertHasClickAction()
        val bounds = day.getUnclippedBoundsInRoot()
        assertTrue((bounds.right - bounds.left).value >= 48f)
        assertTrue((bounds.bottom - bounds.top).value >= 48f)
        compose.onNodeWithText("SELECTED DAY").assertExists()
        compose.onNodeWithText("0 workouts logged").assertExists()
        compose.onNodeWithText("Total duration").assertDoesNotExist()
        compose.onNodeWithText("Moderate").assertDoesNotExist()
        compose.onNodeWithText("Workout streak").assertDoesNotExist()
        compose.onNodeWithText("Dates follow Asia/Kolkata.", substring = true).assertDoesNotExist()
        screenshot("overview")
        tap("Add workout")
        screenshot("day")
        tap("Home")
        awaitText("Asha")
        compose.onNodeWithText("Workout recorded").performScrollTo().assertIsDisplayed()
        tap("Fitness")
        tap("Workout")
        awaitText("Add exercise")
        enterOwnExercise("Squat")
        tap("Fixture barbell squat · barbell")
        tap("Save exercise")
        awaitText("Add exercise")
        tap("Edit Fixture barbell squat")
        compose
            .onNodeWithText(
                "Exercise name",
            ).performScrollTo()
            .performTextReplacement("Personal squat")
        compose.onNodeWithText("Reps, set 1").performScrollTo().performTextReplacement("8")
        compose
            .onNodeWithText(
                "Weight (kg), set 1",
            ).performScrollTo()
            .performTextReplacement("22.5")
        tap("Remember this name for future workouts")
        tap("Save changes")
        awaitText("Add exercise")
        compose.onNodeWithText("Set 1 · 8 reps · 22.5 kg").performScrollTo().assertIsDisplayed()
        tap("Delete Personal squat")
        awaitText("Delete this exercise?")
        assertEquals(1, backend.exercises[100]?.size)
        confirm("Delete exercise")
        awaitText("Start with your first exercise")
        enterOwnExercise()
        tap("Save exercise")
        awaitText("Add exercise")
        tap("Delete workout")
        awaitText("Delete this workout?")
        compose
            .onNodeWithText(
                "This permanently removes this gym visit and all its exercises and sets. Any linked Apple Watch workout import and your exercise catalogue aliases are kept.",
            ).assertIsDisplayed()
        confirm("Delete workout")
        awaitText("Add workout")
        assertTrue(backend.visits.isEmpty())
        assertTrue(backend.exercises.isEmpty())
        tap("Home")
        awaitText("Asha")
        compose.onNodeWithText("No workout recorded today.").performScrollTo().assertIsDisplayed()
        assertEquals(1, backend.records)
        assertEquals(3, backend.saves)
        tap("Fitness")
        compose.onNodeWithContentDescription("Back to Spaces").assertDoesNotExist()
        tap("Home")
        tap("Fitness")
        awaitText("Add workout")
    }

    @Test fun editorSurvivesRotationTabsAndKeyboardAndWarnsBeforeDiscard() {
        launchFitness()
        recordAndOpen()
        tap("Add exercise")
        tap("Save exercise")
        awaitText("Use an exercise name with 1–100 characters.")
        compose
            .onNodeWithText(
                "Exercise name",
            ).performScrollTo()
            .performTextInput("Custom mobility")
        compose.onNodeWithText("Exercise name").performImeAction()
        compose.onNodeWithText("Reps, set 1").performScrollTo().performTextInput("12")
        compose.onNodeWithText("Reps, set 1").performImeAction()
        compose.onNodeWithText("Weight (kg), set 1").assertIsFocused().performTextInput("0")
        compose.onNodeWithText("Weight (kg), set 1").performImeAction()
        scenario!!.recreate()
        awaitText("Add an exercise")
        compose.onNodeWithText("Custom mobility").performScrollTo().assertIsDisplayed()
        screenshot("editor")
        tap("Home")
        awaitText("Asha")
        tap("You")
        tap("Log out")
        awaitText("Log out and discard edits?")
        screenshot("logout-dialog", dialog = true)
        confirm("Cancel")
        tap("Fitness")
        awaitText("Add an exercise")
        compose.onNodeWithText("Custom mobility").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back to Fitness").performClick()
        awaitText("Discard exercise edits?")
        confirm("Cancel")
        compose.onNodeWithText("Custom mobility").performScrollTo().assertIsDisplayed()
        tap("Cancel")
        awaitText("Discard exercise edits?")
        confirm("Discard edits")
        awaitText("Add exercise")
        assertEquals(0, backend.saves)
    }

    @Test fun lostSaveResponseShowsServerRecordsAndBlocksBlindResubmission() {
        launchFitness()
        recordAndOpen()
        enterOwnExercise()
        backend.loseSave = true
        tap("Save exercise")
        awaitText("I’ve reviewed the latest records")
        compose.onNodeWithText("Save exercise").performScrollTo().assertIsNotEnabled()
        assertEquals(1, backend.saves)
        compose.onNodeWithText("Latest recorded exercises").performScrollTo().assertIsDisplayed()
        screenshot("recovery")
        tap("I’ve reviewed the latest records")
        tap("Cancel")
        confirm("Discard edits")
        awaitText("Add exercise")
        compose.onNodeWithText("Custom mobility").performScrollTo().assertIsDisplayed()
        assertEquals(1, backend.exercises[100]?.size)
    }

    @Test fun importedDetailsAreReadOnlyAndPreviousDayIsAccessible() {
        backend.importedVisit()
        launchFitness()
        val date = backend.today.minusDays(1)
        val label = "${date.format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"),
        )}, 1 workout recorded"
        selectCalendarDate(label)
        tap("Workout")
        awaitText("Linked Apple Watch workout")
        compose.onNodeWithText("Energy · 420.5 kcal").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText(
                "Imported timing and energy are read-only. You can manage this visit’s recorded exercises below.",
            ).performScrollTo()
            .assertIsDisplayed()
        screenshot("imported")
        tap("Delete workout")
        confirm("Delete workout")
        awaitText("Add workout")
        assertTrue(backend.imports.contains(7))
    }

    @Test fun uncertainVisitCreationRequiresSuccessfulRefreshBeforeAnotherRecord() {
        launchFitness()
        backend.loseRecord = true
        tap("Add workout")
        awaitText("Refresh server records")
        compose.onNodeWithText("Add workout").performScrollTo().assertIsNotEnabled()
        assertEquals(1, backend.records)
        backend.offlineReads = false
        tap("Refresh server records")
        awaitText("I’ve reviewed the latest records")
        tap("I’ve reviewed the latest records")
        tap("Workout")
        awaitText("Add exercise")
        assertEquals(1, backend.records)
    }

    @Test fun futureVisitAndDatePickerKeepTheChosenCalendarDate() {
        launchFitness()
        tap("Next month")
        val expected =
            YearMonth
                .from(backend.today)
                .plusMonths(1)
                .atDay(1)
        tap("Choose date")
        compose.onNode(isDialog()).assertExists()
        confirm("Choose date")
        tap("Add workout")
        tap("Workout")
        awaitText("Add exercise")
        assertEquals(
            expected,
            backend.visits[100]!!
                .instant
                .atZone(FitnessDates.zone)
                .toLocalDate(),
        )
        assertEquals(1, backend.records)
    }

    @Test fun liveWindowResizePreservesDraftAndAllowsOneSave() {
        fun shell(command: String): String =
            ParcelFileDescriptor
                .AutoCloseInputStream(
                    InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command),
                ).use {
                    it.readBytes().decodeToString()
                }
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
            launchFitness()
            recordAndOpen()
            enterOwnExercise()
            shell("wm size 2048x1800")
            shell("wm density 320")
            awaitText("Add an exercise")
            compose.onNodeWithText("Custom mobility").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("20.5").performScrollTo().assertIsDisplayed()
            assertEquals(0, backend.saves)
            screenshot("resized-editor")
            tap("Save exercise")
            awaitText("Add exercise")
            assertEquals(1, backend.saves)
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
            compose.waitForIdle()
        }
    }
}
