package dev.abhishekan.pulse
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEnabled
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
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.data.EncryptedSessionStore
import dev.abhishekan.pulse.core.data.PulseAuthRepository
import dev.abhishekan.pulse.core.data.PulseDashboardRepository
import dev.abhishekan.pulse.core.data.PulseHorizonRepository
import dev.abhishekan.pulse.core.data.di.dataModule
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.repository.AuthRepository
import dev.abhishekan.pulse.core.domain.repository.DashboardRepository
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import dev.abhishekan.pulse.core.network.HorizonApi
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
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
import java.math.BigDecimal
import java.time.Clock

/** Source-derived fixtures exercise the real HTTP/session/repository/navigation path. */
class HorizonDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var overrides: Module
    private lateinit var store: EncryptedSessionStore
    private lateinit var client: HttpClient
    private lateinit var backend: Backend

    private class Backend {
        val records = mutableMapOf<String, LinkedHashMap<Long, JsonObject>>()
        var currency = "USD"
        var base = "1000.00"
        var nextId = 100L

        @Volatile var loseSave = false

        @Volatile var offline = false

        @Volatile var summaryError = false
        var mutations = 0
        val reads = mutableListOf<String>()

        init {
            listOf(
                "budgets",
                "deductions",
                "subscriptions",
                "categories",
                "transactions",
                "purchases",
            ).forEach {
                records[it] =
                    linkedMapOf()
            }
            save("budgets", 1, """{"name":"Everyday","allocated_amount":200}""")
            save(
                "deductions",
                2,
                """{"name":"Rent","amount":80,"category":"housing","due_day":1,"is_active":true,"budget_id":1}""",
            )
            save(
                "categories",
                3,
                """{"name":"Other","icon":"tag","color":"#64748b","is_default":true}""",
            )
            save(
                "subscriptions",
                4,
                """{"name":"Music","amount":10,"billing_cycle":"monthly","billing_day":5,"status":"active"}""",
            )
            for (i in 1..21) {
                save(
                    "transactions",
                    i.toLong(),
                    """{"name":"Ledger payment $i","amount":2.50,"type":"debit","transaction_date":"2026-10-17T10:20:30Z","category_id":3}""",
                )
            }
        }

        fun items(kind: String) = records.getValue(kind)

        private fun number(value: BigDecimal): JsonElement = Json.parseToJsonElement(value.toPlainString())

        private fun save(
            kind: String,
            id: Long,
            raw: String,
        ): JsonObject {
            val input = Json.parseToJsonElement(raw).jsonObject
            val result =
                buildJsonObject {
                    input.forEach { (k, v) -> put(k, v) }
                    put("id", id)
                    when (kind) {
                        "budgets" -> {
                            put("used_amount", 20)
                            put("available_amount", 180)
                            put("usage_percentage", 10)
                        }
                        "subscriptions" -> {
                            put("monthly_equivalent_amount", input["amount"] ?: JsonPrimitive(0))
                            put("next_renewal_date", "2026-11-05T00:00:00Z")
                            put(
                                "linked_transaction_count",
                                items("transactions").values.count {
                                    it["subscription_id"]?.jsonPrimitive?.longOrNull ==
                                        id
                                },
                            )
                            put("total_spent", 0)
                        }
                        "transactions" -> {
                            put("category_name", "Other")
                            put("budget_name", "Everyday")
                        }
                    }
                }
            items(kind)[id] = result
            return result
        }

        private fun summary(): String {
            val deductions =
                items("deductions")
                    .values
                    .filter {
                        it["is_active"]?.jsonPrimitive?.booleanOrNull !=
                            false
                    }.fold(BigDecimal.ZERO) { a, v ->
                        a +
                            v
                                .getValue("amount")
                                .jsonPrimitive.content
                                .toBigDecimal()
                    }
            val subscriptions =
                items("subscriptions")
                    .values
                    .filter {
                        it["status"]?.jsonPrimitive?.content ==
                            "active"
                    }.fold(BigDecimal.ZERO) { a, v ->
                        a +
                            v
                                .getValue("amount")
                                .jsonPrimitive.content
                                .toBigDecimal()
                    }
            return buildJsonObject {
                put("currency", currency)
                put("base_amount", number(base.toBigDecimal()))
                put(
                    "remaining_amount",
                    number(
                        base.toBigDecimal() - deductions - subscriptions,
                    ),
                )
                put("total_deductions", number(deductions))
                put("total_subscription_burn", number(subscriptions))
                put("total_transactions", 52.5)
                put("total_budgets_allocated", 200)
                put("committed_ratio", 9)
                for (kind in listOf(
                    "budgets",
                    "deductions",
                    "categories",
                    "subscriptions",
                )) {
                    put(kind, JsonArray(items(kind).values.toList()))
                }
                put(
                    "projections",
                    Json.parseToJsonElement(
                        """[{"months":3,"label":"3 Months","cumulative_uncommitted":2730},{"months":6,"label":"6 Months","cumulative_uncommitted":5460}]""",
                    ),
                )
            }.toString()
        }

        data class Reply(
            val body: String,
            val status: HttpStatusCode = HttpStatusCode.OK,
            val login: Boolean = false,
        )

        @Synchronized fun reply(request: HttpRequestData): Reply {
            val path = request.url.encodedPath.removePrefix("/api/")
            val method = request.method
            val raw = (request.body as? TextContent)?.text ?: "{}"
            if (path ==
                "auth/login"
            ) {
                return Reply("""{"username":"horizon-fixture"}""", login = true)
            }
            if (method == HttpMethod.Get) {
                reads += path
                if (offline) throw IOException("Offline fixture")
                return when (path) {
                    "auth/session" -> Reply("""{"username":"horizon-fixture"}""")
                    "profile" -> Reply("""{"has_profile":true,"name":"Asha"}""")
                    "workout/today" -> Reply("""{"visited":false}""")
                    "notifications" -> Reply("[]")
                    "horizon" ->
                        if (summaryError) {
                            Reply(
                                "{}",
                                HttpStatusCode.InternalServerError,
                            )
                        } else {
                            Reply(summary())
                        }
                    "next-month-purchases" ->
                        Reply(
                            buildJsonObject {
                                put("items", JsonArray(items("purchases").values.toList()))
                                put(
                                    "total",
                                    JsonPrimitive(
                                        items("purchases").values.fold(BigDecimal.ZERO) { total, item ->
                                            total +
                                                item
                                                    .getValue("price")
                                                    .jsonPrimitive.content
                                                    .toBigDecimal()
                                        },
                                    ),
                                )
                            }.toString(),
                        )
                    "horizon/transactions" -> {
                        val type = request.url.parameters["type"].orEmpty()
                        val search = request.url.parameters["search"].orEmpty()
                        val all =
                            items("transactions").values.filter {
                                (
                                    type.isBlank() ||
                                        it["type"]?.jsonPrimitive?.content == type
                                ) &&
                                    it
                                        .getValue("name")
                                        .jsonPrimitive.content
                                        .contains(search, true)
                            }
                        val totalPages = (all.size + 19) / 20
                        val page =
                            (request.url.parameters["page"]?.toIntOrNull() ?: 1).coerceIn(
                                1,
                                maxOf(1, totalPages),
                            )
                        Reply(
                            buildJsonObject {
                                put("transactions", JsonArray(all.drop((page - 1) * 20).take(20)))
                                put("page", page)
                                put("page_size", 20)
                                put("total", all.size)
                                put("total_pages", totalPages)
                            }.toString(),
                        )
                    }
                    else ->
                        if (path.startsWith("horizon/subscriptions/") &&
                            path.endsWith("/transactions")
                        ) {
                            val id = path.split('/')[2].toLong()
                            Reply(
                                JsonArray(
                                    items("transactions").values.filter {
                                        it["subscription_id"]?.jsonPrimitive?.longOrNull ==
                                            id
                                    },
                                ).toString(),
                            )
                        } else {
                            error("Unexpected fixture read $path")
                        }
                }
            }
            if (path == "auth/logout") return Reply("{}")
            mutations++
            if (path == "horizon/config") {
                val input = Json.parseToJsonElement(raw).jsonObject
                base =
                    input.getValue("base_amount").jsonPrimitive.content
            } else if (path.startsWith("horizon/subscriptions/") &&
                path.endsWith("/transactions")
            ) {
                val subscriptionId = path.split('/')[2].toLong()
                val id =
                    Json
                        .parseToJsonElement(
                            raw,
                        ).jsonObject
                        .getValue("transaction_id")
                        .jsonPrimitive.long
                val tx = items("transactions").getValue(id)
                if (tx["type"]?.jsonPrimitive?.content != "debit" ||
                    tx["subscription_id"]?.jsonPrimitive?.longOrNull != null
                ) {
                    return Reply("{}", HttpStatusCode.BadRequest)
                }
                save(
                    "transactions",
                    id,
                    JsonObject(
                        tx + ("subscription_id" to JsonPrimitive(subscriptionId)),
                    ).toString(),
                )
            } else {
                val parts = path.split('/')
                val kind =
                    if (parts[0] ==
                        "next-month-purchases"
                    ) {
                        "purchases"
                    } else {
                        parts[1]
                    }
                val id = parts.last().toLongOrNull()
                if (method == HttpMethod.Delete) {
                    if (id == null) items(kind).clear() else items(kind).remove(id)
                    if (kind in listOf("budgets", "deductions", "subscriptions") && id != null) {
                        val key =
                            when (kind) {
                                "budgets" -> "budget_id"
                                "deductions" -> "deduction_id"
                                else -> "subscription_id"
                            }
                        for (target in listOf("transactions", "deductions", "subscriptions")) {
                            items(target).toMap().forEach { (rowId, row) ->
                                if (row[key]?.jsonPrimitive?.longOrNull ==
                                    id
                                ) {
                                    items(target)[rowId] = JsonObject(row + (key to JsonNull))
                                }
                            }
                        }
                    }
                } else {
                    save(kind, id ?: nextId++, raw)
                }
            }
            if (loseSave) {
                loseSave = false
                offline = true
                throw IOException("Lost response after commit")
            }
            return Reply("{}")
        }
    }

    @Before fun setup() {
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.uiAutomation.grantRuntimePermission(
                instrumentation.targetContext.packageName,
                android.Manifest.permission.ACCESS_LOCAL_NETWORK,
            )
        }
        store = GlobalContext.get().get()
        runBlocking { store.clear() }
        backend = Backend()
        val config = NetworkConfig("https://horizon-fixture.example.test/api/", false)
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
                                    "life_session=horizon-test-only; Path=/; Max-Age=3600; Secure; HttpOnly",
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
        val horizon = PulseHorizonRepository(HorizonApi(client), auth)
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
                single<HorizonRepository> { horizon }
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
            runCatching { compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
    }

    private fun tap(text: String) {
        if (text == "You") {
            compose.openProfileFromNavigation()
            return
        }
        scenario?.onActivity { activity ->
            activity
                .getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        val button = (hasText(text) or hasContentDescription(text)) and hasClickAction() and isEnabled()
        compose.waitUntil(15_000) { compose.onAllNodes(button).fetchSemanticsNodes().isNotEmpty() }
        val node = compose.onAllNodes(button)[0]
        if (text !in listOf("Home", "Fitness", "Horizon", "Garage", "Meals") || usesNavigationRail()) node.performScrollTo()
        node.assertIsDisplayed().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
    }

    private fun quickAction(label: String) {
        val (tab, action) =
            when (label) {
                "Log a transaction" -> "Transactions" to label
                "Add a category tag" -> "Transactions" to "Add category type"
                "Add a monthly budget" -> "Budgets" to "Add budget"
                "Add fixed obligation" -> "Obligations" to "Add obligation"
                "Add subscription" -> "Subscriptions" to label
                "Add planned purchase" -> "Planned purchases" to "Add purchase"
                else -> error("Unknown Horizon action: $label")
            }
        tap(tab)
        if (label == "Add planned purchase") {
            val create = (hasText("Add purchase") or hasText("Plan your first purchase")) and hasClickAction() and isEnabled()
            compose.waitUntil(15_000) { compose.onAllNodes(create).fetchSemanticsNodes().isNotEmpty() }
            val empty = compose.onAllNodes(hasText("Plan your first purchase") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
            tap(if (empty) "Plan your first purchase" else action)
        } else {
            tap(action)
        }
    }

    private fun field(
        label: String,
        value: String,
    ) {
        val field = (hasText(label) or hasContentDescription(label)) and hasSetTextAction()
        compose.waitUntil(15_000) { compose.onAllNodes(field).fetchSemanticsNodes().isNotEmpty() }
        compose
            .onNode(field)
            .performScrollTo()
            .performClick()
            .performTextReplacement(value)
    }

    private fun confirm(text: String) {
        val button =
            hasText(text) and hasAnyAncestor(isDialog()) and !hasAnyAncestor(hasTestTag("subscription-payments-sheet")) and isEnabled()
        compose.waitUntil(15_000) { compose.onAllNodes(button).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(button).performClick()
    }

    private fun pick(
        label: String,
        current: String,
        option: String,
    ) {
        tap("$label: $current")
        try {
            awaitText(option)
        } catch (
            error: Exception,
        ) {
            throw AssertionError("Selection $label: $current → $option did not open", error)
        }
        compose.onNodeWithText(option).performScrollTo().performClick()
    }

    private fun launchHorizon() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitText("Welcome back")
        field("Username", "horizon-fixture")
        field("Password", "test-only-password")
        tap("Continue")
        awaitText("Asha")
        listOf(
            "Recent notifications",
            "Your spaces",
            "Open Financial Horizon",
            "Open Fitness",
            "Today follows India time (Asia/Kolkata).",
            "Starting pool",
            "Fixed obligations",
        ).forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
        compose.onNodeWithContentDescription("Financial Horizon").assertHasClickAction()
        screenshot("home-dashboard")
        compose.onNodeWithContentDescription("Financial Horizon").performScrollTo().performClick()
        awaitText("Overview")
    }

    private fun addBudget(name: String = "Travel") {
        tap("Budgets")
        quickAction("Add a monthly budget")
        field("Name", name)
        field("Amount (${UserCurrency.symbol()})", "150.25")
        tap("Save budget")
        awaitText(name)
    }

    private fun screenshot(
        name: String,
        dialog: Boolean = false,
        tag: String? = null,
    ) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.externalCacheDir, "pulse-phase4-$name.png")
        val bitmap =
            (
                if (dialog) {
                    compose.onNode(
                        isDialog(),
                    )
                } else if (tag != null) {
                    compose.onNode(hasTestTag(tag))
                } else {
                    compose.onRoot()
                }
            ).captureToImage().asAndroidBitmap()
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        shell("cp ${file.absolutePath} /sdcard/Download/${file.name}")
    }

    private fun shell(command: String) =
        ParcelFileDescriptor
            .AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command),
            ).use {
                String(it.readBytes()).trim()
            }

    private fun cardAction(
        name: String,
        action: String,
    ) {
        if (backend.items("budgets").values.any { it["name"]?.jsonPrimitive?.content == name }) {
            compose.onNodeWithContentDescription("Actions for $name").performScrollTo().performClick()
            compose.onNodeWithContentDescription("$action $name").assertIsDisplayed().performClick()
            return
        }
        val id =
            backend.records.values
                .flatMap { it.values }
                .first {
                    it["name"]?.jsonPrimitive?.content ==
                        name
                }.getValue("id")
                .jsonPrimitive.long
        compose
            .onNode(
                hasText(action) and hasAnyAncestor(hasTestTag("horizon-record-$id")),
            ).performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) { it() }
    }

    @Test fun budgetCardsAndMenusAdaptToPhoneTabletAndLargeText() {
        val size = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = shell("wm density").lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(": ")
        val fontScale = shell("settings get system font_scale").trim()
        val names = listOf("Home", "Food & Dining", "Transport", "Shopping", "Entertainment", "Miscellaneous")
        val allocations = listOf(25000, 8000, 5000, 6000, 4000, 3000)
        val spending = listOf(18000, 4200, 1800, 2500, 1200, 600)
        backend.currency = "INR"
        backend.items("budgets").clear()
        names.forEachIndexed { index, name ->
            val id = 200L + index
            backend.items("budgets")[id] =
                buildJsonObject {
                    put("id", id)
                    put("name", name)
                    put("allocated_amount", allocations[index])
                    put("used_amount", spending[index])
                    put("available_amount", allocations[index] - spending[index])
                    put("usage_percentage", spending[index] * 100.0 / allocations[index])
                }
        }
        try {
            launchHorizon()
            tap("Budgets")
            for ((layout, dimensions) in listOf("phone" to "720x1600", "tablet" to "2560x1800", "large-text" to "720x1600")) {
                shell("wm size $dimensions")
                shell("wm density 320")
                shell("settings put system font_scale ${if (layout == "large-text") "1.8" else "1.0"}")
                compose.onNode(hasText("Home") and hasAnyAncestor(hasTestTag("budget-grid"))).performScrollTo().assertIsDisplayed()
                screenshot("budget-cards-$layout")
                names.forEach { name ->
                    compose.onNodeWithContentDescription("Actions for $name").performScrollTo().performClick()
                    compose.onNodeWithContentDescription("Edit $name").assertIsDisplayed()
                    compose.onNodeWithContentDescription("Delete $name").assertIsDisplayed()
                    androidx.test.espresso.Espresso
                        .pressBack()
                }
            }
            assertEquals(0, backend.mutations)
        } finally {
            shell(if (size == null) "wm size reset" else "wm size $size")
            shell(if (density == null) "wm density reset" else "wm density $density")
            shell("settings put system font_scale $fontScale")
        }
    }

    @Test fun sectionActionsOpenEveryFormOnPhoneAndTablet() {
        val size = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = shell("wm density").lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(": ")
        try {
            launchHorizon()
            for ((layout, dimensions) in listOf("phone" to "720x1600", "tablet" to "2048x1800")) {
                shell("wm size $dimensions")
                shell("wm density 320")
                awaitText("Overview")
                val actions =
                    listOf(
                        "Log a transaction" to "Add transaction",
                        "Add fixed obligation" to "Add fixed obligation",
                        "Add planned purchase" to "Add planned purchase",
                        "Add subscription" to "Add subscription",
                        "Add a monthly budget" to "Add budget",
                        "Add a category tag" to "Add category",
                    )
                quickAction("Add a category tag")
                awaitText("Add category")
                compose.onNodeWithContentDescription("Financial Horizon quick actions").assertDoesNotExist()
                tap("Cancel editing")
                actions.forEach { (label, title) ->
                    quickAction(label)
                    awaitText(title)
                    if (label == "Add fixed obligation") {
                        compose.onNodeWithContentDescription("Financial Horizon quick actions").assertDoesNotExist()
                        compose.onNodeWithContentDescription("Obligation actions").assertDoesNotExist()
                        screenshot("obligation-$layout")
                        compose.onNode(hasTestTag("obligation-due-day")).performScrollTo().performClick()
                        awaitText("Choose due day")
                        screenshot("obligation-days-$layout", tag = "obligation-day-picker")
                        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                        compose.onNode(hasTestTag("obligation-category-other")).performScrollTo().assertIsDisplayed()
                    }
                    compose.onNodeWithContentDescription("Back to Financial Horizon").assertIsDisplayed().performClick()
                    awaitText("Overview")
                    compose.onNodeWithContentDescription("Back to Financial Horizon").assertDoesNotExist()
                }
                compose.onNodeWithText("Your room to move").assertDoesNotExist()
                screenshot("clean-overview-$layout")
                tap("Transactions")
                compose.onNodeWithText("The everyday ledger").assertDoesNotExist()
                quickAction("Add a monthly budget")
                awaitText("Add budget")
                tap("Cancel editing")
                tap("Overview")
            }
            assertEquals(0, backend.mutations)
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
        }
    }

    @Test fun homeDashboardOpensFinancialHorizon() {
        launchHorizon()
        awaitText("Starting Pool (Base Income)")
        compose.onNodeWithContentDescription("Configure starting pool").assertHasClickAction()
        screenshot("overview-starting-pool")
        compose.onNodeWithText("Total Committed").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Net Available Pool").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Surplus").performScrollTo().assertIsDisplayed()
        screenshot("overview-available-pool")
        compose.onNodeWithText("Monthly commitments").assertDoesNotExist()
        compose.onNodeWithText("Budget allocation:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Recent transaction total:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Future Horizon Projections").performScrollTo().assertIsDisplayed()
        screenshot("overview-projections")
    }

    @Test fun configurationBudgetsAndObligationsWorkAndHomeRefreshes() {
        launchHorizon()
        screenshot("overview")
        compose.onNodeWithContentDescription("Configure starting pool").performScrollTo().performClick()
        field("Amount (${UserCurrency.symbol()})", "2000.25")
        tap("Save starting pool")
        awaitText(
            UserCurrency
                .format(BigDecimal("1910.25")),
        )
        tap("Home")
        awaitText("Asha")
        compose
            .onNodeWithText(
                UserCurrency
                    .format(BigDecimal("1910.25")),
            ).performScrollTo()
            .assertIsDisplayed()
        tap("Horizon")
        addBudget()
        assertEquals(
            "150.25",
            backend
                .items("budgets")
                .values
                .last()["allocated_amount"]
                ?.jsonPrimitive
                ?.content,
        )
        cardAction("Travel", "Edit")
        field("Name", "Travel fund")
        field("Amount (${UserCurrency.symbol()})", "175.50")
        tap("Save budget")
        awaitText("Travel fund")
        tap("Obligations")
        quickAction("Add fixed obligation")
        compose.onNode(hasTestTag("obligation-category-investment")).performScrollTo().performClick()
        field("Name", "Internet")
        field("Amount (${UserCurrency.symbol()})", "50.25")
        compose.onNode(hasTestTag("obligation-due-day")).performScrollTo().performClick()
        awaitText("Choose due day")
        compose.onNode(hasTestTag("obligation-day-31")).performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("obligation-day-picker")).fetchSemanticsNodes().isEmpty()
        }
        compose.onNode(hasTestTag("obligation-budget")).performScrollTo().performClick()
        tap("Travel fund")
        tap("Active obligation")
        tap("Save fixed obligation")
        awaitText("Internet")
        val obligation = backend.items("deductions").values.last()
        assertEquals("investment", obligation["category"]?.jsonPrimitive?.content)
        assertEquals(false, obligation["is_active"]?.jsonPrimitive?.boolean)
        assertEquals(31, obligation["due_day"]?.jsonPrimitive?.int)
        assertEquals(
            backend
                .items("budgets")
                .entries
                .single { it.value["name"]?.jsonPrimitive?.content == "Travel fund" }
                .key,
            obligation["budget_id"]?.jsonPrimitive?.long,
        )
        cardAction("Internet", "Edit")
        awaitText("Edit fixed obligation")
        compose.onNode(hasContentDescription("Name") and hasSetTextAction()).assertTextEquals("Internet")
        compose.onNode(hasContentDescription("Amount (${UserCurrency.symbol()})") and hasSetTextAction()).assertTextContains("50.25")
        compose.onNode(hasTestTag("obligation-due-day")).assertTextContains("31")
        compose.onNode(hasTestTag("obligation-due-day")).performScrollTo().performClick()
        compose.onNode(hasTestTag("obligation-day-31")).assertIsSelected()
        tap("Clear due day")
        compose.onNode(hasTestTag("obligation-due-day")).assertTextContains("Not set")
        compose.onNode(hasTestTag("obligation-due-day")).performClick()
        awaitText("Choose due day")
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("obligation-day-picker")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Discard unsaved changes?").assertDoesNotExist()
        compose.onNode(hasTestTag("obligation-due-day")).assertTextContains("Not set")
        compose.onNode(hasTestTag("obligation-due-day")).performClick()
        compose.onNode(hasTestTag("obligation-day-31")).performScrollTo().performClick()
        compose.onNode(hasTestTag("obligation-category-investment")).assertIsSelected()
        compose.onNode(hasTestTag("obligation-budget")).assertTextContains("Travel fund")
        compose.onNodeWithText("Active obligation").assertIsOff()
        screenshot("obligation-edit")
        compose.onNodeWithContentDescription("Obligation actions").performClick()
        tap("Delete obligation")
        awaitText("Confirm deletion")
        compose.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()
        awaitText("Edit fixed obligation")
        assertEquals(2, backend.items("deductions").size)
        tap("Active obligation")
        tap("Save changes")
        awaitText("Internet")
        tap("Budgets")
        cardAction("Travel fund", "Delete")
        awaitText("Confirm deletion")
        confirm("Delete")
        awaitText("Everyday")
        assertEquals(JsonNull, backend.items("deductions").values.last()["budget_id"])
        tap("Obligations")
        cardAction("Internet", "Edit")
        field("Name", "Unsaved name")
        compose.onNodeWithContentDescription("Obligation actions").performClick()
        tap("Delete obligation")
        confirm("Delete")
        awaitText("Rent")
        assertEquals(1, backend.items("deductions").size)
        compose.onNode(hasTestTag("fixed-obligation-editor")).assertDoesNotExist()
    }

    @Test fun transactionLedgerAdaptsBetweenPhoneAndTablet() {
        val size = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = shell("wm density").lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(": ")
        listOf("Bills & Utilities", "Food & Dining", "Entertainment", "Health & Fitness", "Transport").forEachIndexed { index, name ->
            val id = 10L + index
            backend.items("categories")[id] =
                buildJsonObject {
                    put("id", id)
                    put("name", name)
                    put("icon", "tag")
                    put("color", "#6B65B8")
                    put("is_default", false)
                }
        }
        try {
            shell("wm size 720x1600")
            shell("wm density 320")
            launchHorizon()
            tap("Transactions")
            awaitText("Ledger payment 1")
            compose.onNode(hasTestTag("ledger-category-filter")).performScrollTo().performClick()
            awaitText("Choose a category")
            screenshot("filter-categories-phone", tag = "ledger-category-sheet")
            compose.onNodeWithText("Search categories").performTextReplacement("Transport")
            compose.onNode(hasTestTag("ledger-category-option-14")).performClick()
            awaitText("No transactions")
            compose.onNode(hasTestTag("ledger-category-filter")).performScrollTo().performClick()
            awaitText("Choose a category")
            compose.onNodeWithText("Search categories").performTextReplacement("No such category")
            awaitText("No categories match. Try another name.")
            compose.onNodeWithText("Search categories").performTextReplacement("Other")
            compose.onNode(hasTestTag("ledger-category-option-3")).performClick()
            awaitText("Ledger payment 1")
            compose.onNode(hasTestTag("ledger-type-filter")).performScrollTo().performClick()
            awaitText("Transaction type")
            compose.onNode(hasText("Credit") and hasClickAction() and hasAnyAncestor(hasTestTag("ledger-category-sheet"))).performClick()
            awaitText("No transactions")
            compose.onNode(hasTestTag("ledger-type-filter")).performScrollTo().performClick()
            awaitText("Transaction type")
            compose.onNode(hasText("All types") and hasClickAction() and hasAnyAncestor(hasTestTag("ledger-category-sheet"))).performClick()
            awaitText("Ledger payment 1")
            compose.onNode(hasTestTag("ledger-category-filter") and hasText("Other")).assertIsDisplayed()
            screenshot("ledger-phone")
            compose.onNode(hasTestTag("ledger-sort")).assertDoesNotExist()
            shell("wm size 2400x1800")
            shell("wm density 320")
            compose.onNode(hasTestTag("ledger-category-more")).performScrollTo().performClick()
            awaitText("Choose a category")
            screenshot("filter-categories-tablet", tag = "ledger-category-sheet")
            compose
                .onNode(
                    hasText("All categories") and hasClickAction() and hasAnyAncestor(hasTestTag("ledger-category-sheet")),
                ).performClick()
            awaitText("Ledger payment 1")
            compose.onNodeWithText("Bills & Utilities", useUnmergedTree = true).assertIsDisplayed()
            screenshot("ledger-tablet")
            tap("Ledger payment 1")
            awaitText("Edit")
            compose.onNodeWithContentDescription("Edit Ledger payment 1").performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription("Delete Ledger payment 1").performScrollTo().assertIsDisplayed()
            assertEquals(0, backend.mutations)
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
        }
    }

    @Test fun transactionPaginationCategoryAssociationsAndCrud() {
        launchHorizon()
        tap("Transactions")
        tap("Next page")
        awaitText("Page 2 of 2 · 21 transactions")
        quickAction("Add a category tag")
        field("Name", "Coffee")
        tap("Save category")
        awaitText("Search transactions")
        quickAction("Log a transaction")
        field("Name", "Coffee refund")
        field("Amount (${UserCurrency.symbol()})", "12.50")
        pick("Type", "Debit", "Credit")
        pick("Category", "None", "Coffee")
        pick("Budget", "None", "Everyday")
        pick("Subscription", "None", "Music")
        field("Notes (optional)", "Receipt refunded")
        screenshot("transaction-editor")
        tap("Save transaction")
        awaitText("Search transactions")
        field("Search transactions", "Coffee refund")
        awaitText("Page 1 of 1 · 1 transaction")
        awaitText("Coffee refund")
        screenshot("transactions")
        val tx = backend.items("transactions").values.last()
        assertEquals("credit", tx["type"]?.jsonPrimitive?.content)
        assertEquals(1L, tx["budget_id"]?.jsonPrimitive?.long)
        assertEquals(4L, tx["subscription_id"]?.jsonPrimitive?.long)
        compose.onNode(hasTestTag("horizon-record-${backend.items("transactions").keys.last()}")).performScrollTo().performClick()
        tap("Edit")
        field("Name", "Coffee expense")
        pick("Type", "Credit", "Debit")
        pick("Subscription", "Music", "None")
        tap("Save transaction")
        field("Search transactions", "Coffee expense")
        awaitText("Page 1 of 1 · 1 transaction")
        awaitText("Coffee expense")
        compose.onNode(hasTestTag("horizon-record-${backend.items("transactions").keys.last()}")).performScrollTo().performClick()
        tap("Delete")
        confirm("Delete")
        awaitText("No transactions")
        assertEquals(21, backend.items("transactions").size)
    }

    @Test fun subscriptionsLinkExistingDebitsAndPreserveTransactionsOnDelete() {
        backend.items("transactions")[2] =
            JsonObject(
                backend.items("transactions").getValue(2) + ("type" to JsonPrimitive("credit")),
            )
        backend.items("transactions")[3] =
            JsonObject(
                backend.items("transactions").getValue(3) + ("subscription_id" to JsonPrimitive(4)),
            )
        launchHorizon()
        tap("Subscriptions")
        quickAction("Add subscription")
        field("Subscription name *", "Streaming")
        field("Amount (${UserCurrency.symbol()}) *", "99.99")
        compose.onNodeWithText("Date:", substring = true).assertDoesNotExist()
        compose.onNode(hasText("Add subscription") and hasClickAction()).assertExists()
        compose.onNodeWithText("Basic details").assertIsDisplayed()
        screenshot("subscription-basic")
        compose.onNode(hasTestTag("subscription-due-day")).performScrollTo().performClick()
        compose.onNode(hasTestTag("subscription-day-20")).performScrollTo().performClick()
        tap("Yearly")
        compose.onNodeWithText("Billing day of month (1–31) *").assertDoesNotExist()
        compose.onNodeWithText("Renewal date *").assertExists()
        tap("Paused")
        compose.onNodeWithContentDescription("Fixed obligation: None").performScrollTo()
        compose.onNodeWithContentDescription("Fixed obligation: None").assertIsDisplayed()
        pick("Link to budget", "None", "Everyday")
        pick("Fixed obligation", "None", "Rent")
        field("Notes (optional)", "Shared family plan\nRenew online")
        screenshot("subscription-notes")
        compose
            .onNode(
                hasText("Add subscription") and hasClickAction(),
            ).performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) {
                it()
            }
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("subscription-editor")).fetchSemanticsNodes().isEmpty() }
        awaitText("Streaming")
        assertEquals(
            2L,
            backend
                .items("subscriptions")
                .getValue(100)["deduction_id"]
                ?.jsonPrimitive
                ?.long,
        )
        assertEquals(
            "Shared family plan\nRenew online",
            backend
                .items("subscriptions")
                .getValue(100)["notes"]
                ?.jsonPrimitive
                ?.content,
        )
        cardAction("Streaming", "Edit")
        tap("Active")
        compose
            .onNodeWithText(
                "Save changes",
            ).performScrollTo()
            .assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) {
                it()
            }
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("subscription-editor")).fetchSemanticsNodes().isEmpty() }
        awaitText("Streaming")
        tap("View payments for Streaming")
        awaitText("Link an existing payment")
        compose
            .onNodeWithText(
                "Link Ledger payment 2 · ${UserCurrency.format(BigDecimal("2.50"))}",
            ).assertDoesNotExist()
        compose
            .onNodeWithText(
                "Link Ledger payment 3 · ${UserCurrency.format(BigDecimal("2.50"))}",
            ).assertDoesNotExist()
        tap("Link Ledger payment 1 · ${UserCurrency.format(BigDecimal("2.50"))}")
        confirm("Link payment")
        awaitText("Ledger payment 1")
        assertEquals(
            100L,
            backend
                .items("transactions")
                .getValue(1)["subscription_id"]
                ?.jsonPrimitive
                ?.long,
        )
        screenshot("payments")
        tap("More actions for Streaming")
        tap("Delete Streaming")
        confirm("Delete")
        awaitText("Music")
        compose.onNodeWithText("Close payments").assertDoesNotExist()
        assertEquals(21, backend.items("transactions").size)
        assertEquals(JsonNull, backend.items("transactions").getValue(1)["subscription_id"])
        assertTrue(backend.items("deductions").containsKey(2))
    }

    @Test fun subscriptionPaymentsOpenInSheetAndLinkDebits() {
        val size = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = shell("wm density").lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(": ")
        backend.items("transactions")[1] =
            JsonObject(backend.items("transactions").getValue(1) + ("subscription_id" to JsonPrimitive(4)))
        backend.items("transactions")[2] =
            JsonObject(backend.items("transactions").getValue(2) + ("type" to JsonPrimitive("credit")))
        backend.items("transactions")[3] =
            JsonObject(backend.items("transactions").getValue(3) + ("subscription_id" to JsonPrimitive(99)))
        try {
            shell("wm size 720x1600")
            shell("wm density 320")
            launchHorizon()
            tap("Subscriptions")
            tap("View payments for Music")
            awaitText("Payment history")
            val sheet = hasTestTag("subscription-payments-sheet")
            compose.onNode(sheet).assertIsDisplayed()
            compose.onNode(hasText("Ledger payment 1") and hasAnyAncestor(sheet)).assertExists()
            listOf(1, 2, 3).forEach { id ->
                compose
                    .onNodeWithContentDescription("Link Ledger payment $id · ${UserCurrency.format(BigDecimal("2.50"))}")
                    .assertDoesNotExist()
            }
            tap("Link Ledger payment 4 · ${UserCurrency.format(BigDecimal("2.50"))}")
            awaitText("Link payment?")
            compose.onNode(hasText("Link payment") and hasAnyAncestor(isDialog()) and !hasAnyAncestor(sheet)).performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodes(hasText("2 linked payments") and hasAnyAncestor(sheet)).fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(
                4L,
                backend
                    .items("transactions")
                    .getValue(4)["subscription_id"]
                    ?.jsonPrimitive
                    ?.long,
            )
            compose.onNode(hasTestTag("subscription-payments-summary")).performScrollTo().assertIsDisplayed()
            screenshot("subscription-payments-phone", tag = "subscription-payments-sheet")
            shell("wm size 2400x1800")
            shell("wm density 320")
            compose.onNode(sheet).assertIsDisplayed()
            screenshot("subscription-payments-tablet", tag = "subscription-payments-sheet")
            shell("input keyevent KEYCODE_BACK")
            compose.waitUntil(5_000) { compose.onAllNodes(sheet).fetchSemanticsNodes().isEmpty() }
            tap("View payments for Music")
            awaitText("Payment history")
            compose.onNodeWithContentDescription("Close payments").performClick()
            compose.onNode(sheet).assertDoesNotExist()
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
        }
    }

    @Test fun subscriptionListAdaptsAndUsesExistingApiData() {
        val size = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = shell("wm density").lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(": ")
        val fontScale = shell("settings get system font_scale").trim()
        val today = java.time.LocalDate.now(dev.abhishekan.pulse.core.domain.model.UserTimeZone.zone)
        backend.items("subscriptions")[4] =
            JsonObject(
                backend.items("subscriptions").getValue(4) +
                    mapOf(
                        "next_renewal_date" to JsonPrimitive("${today.plusDays(3)}T00:00:00Z"),
                        "category_id" to JsonPrimitive(3),
                        "linked_transaction_count" to JsonPrimitive(2),
                        "total_spent" to JsonPrimitive(20),
                    ),
            )
        backend.items("subscriptions")[5] =
            JsonObject(
                backend.items("subscriptions").getValue(4) +
                    mapOf(
                        "id" to JsonPrimitive(5),
                        "name" to JsonPrimitive("Annual cloud storage"),
                        "billing_cycle" to JsonPrimitive("yearly"),
                        "status" to JsonPrimitive("paused"),
                        "next_renewal_date" to JsonNull,
                        "billing_day" to JsonNull,
                    ),
            )
        try {
            launchHorizon()
            tap("Subscriptions")
            for ((layout, dimensions) in listOf("phone" to "720x1600", "tablet" to "2560x1800", "large-text" to "720x1600")) {
                shell("wm size $dimensions")
                shell("wm density 320")
                shell("settings put system font_scale ${if (layout == "large-text") "1.8" else "1.0"}")
                awaitText("Your subscriptions")
                compose.onNodeWithText("in 3 days").performScrollTo().assertIsDisplayed()
                compose
                    .onNode(
                        hasText("2 linked payments") and hasAnyAncestor(hasTestTag("horizon-record-4")),
                    ).performScrollTo()
                    .assertIsDisplayed()
                compose.onNodeWithContentDescription("View payments for Music").performScrollTo().assertIsDisplayed()
                screenshot("subscription-list-$layout-card")
                compose.onNodeWithText("No active renewal").performScrollTo().assertIsDisplayed()
                compose.onNodeWithText("Monthly subscription spend").performScrollTo().assertIsDisplayed()
                screenshot("subscription-list-$layout-summary")
                compose.onNodeWithText("Sort by: Next renewal").assertDoesNotExist()
                compose.onNode(hasText("Add subscription") and hasClickAction()).performScrollTo().assertIsEnabled()
            }
            assertEquals(0, backend.mutations)
        } finally {
            shell(if (size == null) "wm size reset" else "wm size $size")
            shell(if (density == null) "wm density reset" else "wm density $density")
            shell("settings put system font_scale $fontScale")
        }
    }

    @Test fun subscriptionFormSectionsStayReachableOnPhoneTabletAndLargeText() {
        val size = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = shell("wm density").lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(": ")
        val fontScale = shell("settings get system font_scale").trim()
        try {
            launchHorizon()
            quickAction("Add subscription")
            for ((layout, dimensions) in listOf("phone" to "720x1600", "tablet" to "2048x1800", "large-text" to "720x1600")) {
                shell("wm size $dimensions")
                shell("wm density 320")
                shell("settings put system font_scale ${if (layout == "large-text") "1.8" else "1.0"}")
                compose.waitForIdle()
                compose.onNode(hasTestTag("subscription-section-menu")).assertDoesNotExist()
                compose.onNode(hasTestTag("subscription-notes")).performScrollTo()
                compose.onNode(hasTestTag("subscription-notes")).assertIsDisplayed()
                compose.onNode(hasTestTag("horizon-editor-actions")).performScrollTo().assertIsDisplayed()
                screenshot("subscription-$layout-notes")
                compose.onNode(hasTestTag("subscription-section-menu")).assertDoesNotExist()
                compose.onNodeWithText("Subscription name *").performScrollTo()
                compose.onNodeWithText("Subscription name *").assertIsDisplayed()
                if (layout == "phone") compose.onNode(hasText("Add subscription") and hasClickAction()).assertIsNotDisplayed()
                compose.onNodeWithText("Monthly").performScrollTo().assertIsDisplayed()
                screenshot("subscription-$layout-basic")
            }
            assertEquals(0, backend.mutations)
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
            shell("settings put system font_scale ${fontScale.takeUnless { it == "null" || it.isEmpty() } ?: "1.0"}")
        }
    }

    @Test fun categoryFormAdaptsRetainsAppearanceAndSaves() {
        val size = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = shell("wm density").lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(": ")
        val fontScale = shell("settings get system font_scale").trim()
        try {
            launchHorizon()
            quickAction("Add a category tag")
            compose.onNodeWithText("Add category").assertIsDisplayed()
            compose.onNodeWithContentDescription("Financial Horizon quick actions").assertDoesNotExist()
            tap("Save category")
            compose.onNode(hasTestTag("category-name")).assertIsFocused()
            awaitText("Enter a category name of up to 100 characters.")
            field("Name", "Weekend outings")
            compose.onNode(hasTestTag("category-icon-shopping-cart")).performScrollTo().performClick()
            compose.onNode(hasTestTag("category-color-#d96a4f")).performScrollTo().performClick()
            scenario!!.recreate()
            awaitText("Add category")
            compose.onNode(hasTestTag("category-icon-shopping-cart")).assertIsSelected()
            compose.onNode(hasTestTag("category-color-#d96a4f")).assertIsSelected()
            compose.onNode(hasTestTag("category-name")).assertTextContains("Weekend outings")
            for ((layout, dimensions) in listOf("phone" to "720x1600", "tablet" to "2048x1800", "large-text" to "720x1600")) {
                shell("wm size $dimensions")
                shell("wm density 320")
                shell("settings put system font_scale ${if (layout == "large-text") "1.8" else "1.0"}")
                compose.waitForIdle()
                compose.onNode(hasTestTag("category-name")).performScrollTo().assertIsDisplayed()
                screenshot("category-$layout-details")
                compose.onNode(hasTestTag("category-color-#d96a4f")).performScrollTo().assertIsDisplayed()
                compose.onNode(hasTestTag("horizon-editor-actions")).performScrollTo().assertIsDisplayed()
                assertEquals(
                    compose
                        .onNodeWithText("Cancel")
                        .fetchSemanticsNode()
                        .boundsInRoot.width,
                    compose
                        .onNodeWithText("Save category")
                        .fetchSemanticsNode()
                        .boundsInRoot.width,
                    1f,
                )
                screenshot("category-$layout-actions")
            }
            tap("Save category")
            compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("category-editor")).fetchSemanticsNodes().isEmpty() }
            val category = backend.items("categories").values.last()
            assertEquals("Weekend outings", category["name"]?.jsonPrimitive?.content)
            assertEquals("shopping-cart", category["icon"]?.jsonPrimitive?.content)
            assertEquals("#d96a4f", category["color"]?.jsonPrimitive?.content)
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
            shell("settings put system font_scale ${fontScale.takeUnless { it == "null" || it.isEmpty() } ?: "1.0"}")
        }
    }

    @Test fun budgetFormAdaptsAndSavesSpendingLimit() {
        val size = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = shell("wm density").lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(": ")
        val fontScale = shell("settings get system font_scale").trim()
        try {
            launchHorizon()
            quickAction("Add a monthly budget")
            compose.onNodeWithText("Add budget").assertIsDisplayed()
            compose.onNodeWithContentDescription("Financial Horizon quick actions").assertDoesNotExist()
            compose
                .onNodeWithText(
                    "Save budget",
                ).performScrollTo()
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) {
                    it()
                }
            compose.onNode(hasTestTag("budget-name")).assertIsFocused()
            field("Name", "Groceries")
            field("Amount (${UserCurrency.symbol()})", "250.50")
            compose.onNode(hasTestTag("budget-amount")).assertTextContains("250.50")
            for ((layout, dimensions) in listOf(
                "phone" to "720x1600",
                "tablet" to "2048x1800",
                "landscape" to "1280x720",
                "large-text" to "720x1600",
            )) {
                shell("wm size $dimensions")
                shell("wm density 320")
                shell("settings put system font_scale ${if (layout == "large-text") "1.8" else "1.0"}")
                compose.waitForIdle()
                if (layout == "tablet") {
                    compose.onNode(hasTestTag("budget-preview")).performScrollTo().assertIsDisplayed()
                } else {
                    compose.onNode(hasTestTag("budget-preview")).assertDoesNotExist()
                }
                compose.onNode(hasTestTag("budget-amount")).performScrollTo().assertIsDisplayed()
                compose.onNode(hasTestTag("budget-amount")).assertTextContains("250.50")
                compose.onNodeWithText("Save budget").performScrollTo().assertIsDisplayed()
                compose.onNode(hasTestTag("horizon-editor-actions")).performScrollTo().assertIsDisplayed()
                screenshot("budget-$layout")
            }
            compose
                .onNodeWithText(
                    "Save budget",
                ).performScrollTo()
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) {
                    it()
                }
            compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("budget-editor")).fetchSemanticsNodes().isEmpty() }
            assertEquals(
                "250.50",
                backend
                    .items("budgets")
                    .values
                    .last()["allocated_amount"]
                    ?.jsonPrimitive
                    ?.content,
            )
            assertEquals(
                "Groceries",
                backend
                    .items("budgets")
                    .values
                    .last()["name"]
                    ?.jsonPrimitive
                    ?.content,
            )
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
            shell("settings put system font_scale ${fontScale.takeUnless { it == "null" || it.isEmpty() } ?: "1.0"}")
        }
    }

    @Test fun plannedPurchasesRequireConfirmationForIndividualAndClear() {
        launchHorizon()
        tap("Planned purchases")
        awaitText("Nothing planned yet")
        compose.onNodeWithText("Clear planned purchases").assertDoesNotExist()
        compose.onNodeWithText("Sort by", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Plan your first purchase").performScrollTo().assertHasClickAction()
        screenshot("purchase-list-empty")
        for (name in listOf("Desk lamp", "Backpack")) {
            if (name == "Desk lamp") tap("Plan your first purchase") else tap("Add purchase")
            compose.onNodeWithText("Add planned purchase").assertIsDisplayed()
            field("Purchase name *", name)
            field("Estimated purchase amount *", "12.50")
            field("Product link (optional)", "https://example.test/item")
            tap("Add purchase")
            awaitText(name)
        }
        awaitText("2 planned purchases")
        screenshot("purchase-list-filled")
        tap("Delete Desk lamp")
        assertEquals(2, backend.items("purchases").size)
        confirm("Delete")
        awaitText("Backpack")
        tap("Clear planned purchases")
        assertEquals(1, backend.items("purchases").size)
        confirm("Delete")
        awaitText("Nothing planned yet")
        assertTrue(backend.items("purchases").isEmpty())
    }

    @Test fun purchaseListAdaptsToPhoneTabletAndLargeText() {
        backend.currency = "INR"
        val size = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = shell("wm density").lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(": ")
        val fontScale = shell("settings get system font_scale").trim()
        try {
            launchHorizon()
            compose.runOnIdle { UserCurrency.select("INR") }
            tap("Planned purchases")
            tap("Plan your first purchase")
            field("Purchase name *", "New monitor")
            field("Estimated purchase amount *", "20000")
            tap("Add purchase")
            awaitText("New monitor")
            for ((layout, dimensions) in listOf("phone" to "720x1600", "tablet" to "2048x1800", "large-text" to "720x1600")) {
                shell("wm size $dimensions")
                shell("wm density 320")
                shell("settings put system font_scale ${if (layout == "large-text") "1.8" else "1.0"}")
                awaitText("1 planned purchase")
                compose.onNodeWithText("Add purchase").performScrollTo().assertIsDisplayed()
                compose
                    .onNodeWithContentDescription("Delete New monitor")
                    .performScrollTo()
                    .assertIsDisplayed()
                    .assertHasClickAction()
                compose.onNode(hasTestTag("purchase-summary")).performScrollTo()
                screenshot("purchase-list-$layout")
            }
            assertEquals(1, backend.items("purchases").size)
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
            shell("settings put system font_scale ${fontScale.takeUnless { it == "null" || it.isEmpty() } ?: "1.0"}")
        }
    }

    @Test fun purchaseFormValidatesRetainsDraftAndAllowsAnEmptyLink() {
        launchHorizon()
        tap("Planned purchases")
        quickAction("Add planned purchase")
        compose.onNodeWithText("Add planned purchase").assertIsDisplayed()
        compose.onNodeWithContentDescription("Financial Horizon quick actions").assertDoesNotExist()
        compose.onNode(hasText("Add planned purchase") and hasAnyAncestor(hasTestTag("planned-purchase-editor"))).assertDoesNotExist()
        compose.onNodeWithText("Plan a purchase for your upcoming financial forecast.").assertDoesNotExist()
        compose.onNodeWithText("Planning for").assertDoesNotExist()
        tap("Add purchase")
        compose.onNode(hasTestTag("purchase-name")).assertIsFocused()
        awaitText("Enter a purchase name of up to 160 characters.")
        assertTrue(backend.items("purchases").isEmpty())
        field("Purchase name *", "Monitor")
        field("Estimated purchase amount *", "125000.50")
        field("Product link (optional)", "not-a-link")
        tap("Add purchase")
        compose.onNode(hasTestTag("purchase-url")).assertIsFocused()
        awaitText("Enter a valid http or https link.")
        assertTrue(backend.items("purchases").isEmpty())
        field("Product link (optional)", "https://example.test/item")
        awaitText("Valid link. You can reference this purchase later.")
        scenario!!.recreate()
        awaitText("Add planned purchase")
        compose.onNode(hasTestTag("purchase-name")).assertTextContains("Monitor")
        compose.onNodeWithContentDescription("Clear product link").performScrollTo().performClick()
        tap("Add purchase")
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag("planned-purchase-editor")).fetchSemanticsNodes().isEmpty() }
        awaitText("Monitor")
        assertEquals(1, backend.items("purchases").size)
        assertEquals(JsonNull, backend.items("purchases").values.single()["url"])
        compose.onNodeWithContentDescription("Financial Horizon quick actions").assertDoesNotExist()
    }

    @Test fun purchaseFormAdaptsToPhoneTabletLandscapeAndLargeText() {
        backend.currency = "INR"
        val size = shell("wm size").lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(": ")
        val density = shell("wm density").lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(": ")
        val fontScale = shell("settings get system font_scale").trim()
        try {
            launchHorizon()
            compose.runOnIdle { UserCurrency.select("INR") }
            quickAction("Add planned purchase")
            field("Purchase name *", "New monitor")
            field("Estimated purchase amount *", "125000.50")
            val layouts =
                listOf(
                    "phone" to "720x1600",
                    "tablet" to "2048x1800",
                    "landscape" to "1600x720",
                    "large-text" to "720x1600",
                )
            for ((layout, dimensions) in layouts) {
                shell("wm size $dimensions")
                shell("wm density 320")
                shell("settings put system font_scale ${if (layout == "large-text") "1.8" else "1.0"}")
                awaitText("Add planned purchase")
                compose.onNode(hasTestTag("purchase-name")).performScrollTo().assertTextContains("New monitor")
                compose.onNode(hasTestTag("purchase-amount")).performScrollTo().assertIsDisplayed()
                scenario?.onActivity { activity ->
                    activity
                        .getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                        .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
                }
                compose.waitForIdle()
                compose.onNode(hasTestTag("purchase-url")).performScrollTo().assertIsDisplayed()
                compose.onNodeWithText("Add purchase").performScrollTo().assertIsDisplayed()
                compose.onNodeWithText("Planning for").assertDoesNotExist()
                compose.onNodeWithText("Add planned purchase").assertIsDisplayed()
                compose.onNodeWithContentDescription("Financial Horizon quick actions").assertDoesNotExist()
                screenshot("purchase-$layout")
            }
            assertEquals(0, backend.mutations)
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
            shell("settings put system font_scale ${fontScale.takeUnless { it == "null" || it.isEmpty() } ?: "1.0"}")
        }
    }

    @Test fun draftsSurviveRotationTabsAndKeyboardAndWarnBeforeDiscard() {
        launchHorizon()
        tap("Transactions")
        quickAction("Log a transaction")
        field("Name", "Unsaved finance")
        field("Amount (${UserCurrency.symbol()})", "0.001")
        tap("Save transaction")
        assertEquals(0, backend.mutations)
        scenario!!.recreate()
        awaitText("Unsaved finance")
        tap("Home")
        tap("Horizon")
        awaitText("Unsaved finance")
        tap("You")
        tap("Log out")
        awaitText("Log out and discard edits?")
        confirm("Cancel")
        tap("Horizon")
        awaitText("Unsaved finance")
        compose.onNodeWithText("Name").performScrollTo().performImeAction()
        screenshot("draft")
        compose.onNodeWithContentDescription("Back to Financial Horizon").assertIsDisplayed().performClick()
        awaitText("Discard unsaved changes?")
        confirm("Cancel")
        awaitText("Unsaved finance")
        compose.onNodeWithContentDescription("Back to Financial Horizon").performClick()
        awaitText("Discard unsaved changes?")
        confirm("Discard edits")
        awaitText("Search transactions")
    }

    @Test fun uncertainSaveRefreshesAndBlocksDuplicatesUntilRecordsReviewed() {
        launchHorizon()
        tap("Budgets")
        quickAction("Add a monthly budget")
        field("Name", "Lost response")
        field("Amount (${UserCurrency.symbol()})", "30.25")
        backend.loseSave =
            true
        tap("Save budget")
        awaitText("Review before another change")
        compose.onNodeWithText("Save budget").assertIsNotEnabled()
        compose.onNodeWithText("I reviewed the server records").assertIsNotEnabled()
        assertEquals(1, backend.mutations)
        backend.offline = false
        tap("Refresh server records")
        tap("View server records")
        awaitText("Lost response")
        screenshot("recovery")
        tap("I reviewed the server records")
        tap("Resume unsaved edits")
        tap("Cancel editing")
        confirm("Discard edits")
        awaitText("Budgets")
        assertEquals(1, backend.mutations)
        assertEquals(
            1,
            backend.items("budgets").values.count {
                it["name"]?.jsonPrimitive?.content ==
                    "Lost response"
            },
        )
    }

    @Test fun resizingPreservesDraftAndNativeDateTimePickersWork() {
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
            launchHorizon()
            tap("Transactions")
            quickAction("Log a transaction")
            field("Name", "Resized draft")
            field("Amount (${UserCurrency.symbol()})", "75.25")
            compose.onNodeWithText("Name").performScrollTo().performImeAction()
            compose.onNodeWithText("Amount (${UserCurrency.symbol()})").assertIsFocused()
            compose.onNodeWithText("Date:", substring = true).performScrollTo().performClick()
            compose.onNode(isDialog()).assertExists()
            screenshot("date-picker", true)
            confirm("Choose date")
            compose.onNodeWithText("Time:", substring = true).performScrollTo().performClick()
            compose.onNode(isDialog()).assertExists()
            screenshot("time-picker", true)
            confirm("Choose time")
            shell("wm size 2048x1800")
            shell("wm density 320")
            awaitText("Resized draft")
            compose.onNodeWithText("75.25").performScrollTo().assertIsDisplayed()
            screenshot("resized-editor")
            assertEquals(0, backend.mutations)
            tap("Save transaction")
            awaitText("Search transactions")
            assertEquals(1, backend.mutations)
            assertEquals(
                "Resized draft",
                backend
                    .items("transactions")
                    .values
                    .last()["name"]
                    ?.jsonPrimitive
                    ?.content,
            )
        } finally {
            shell("wm size ${size ?: "reset"}")
            shell("wm density ${density ?: "reset"}")
            compose.waitForIdle()
        }
    }

    @Test fun overviewFailureDoesNotBlankTransactionHistoryAndRetryWorks() {
        backend.summaryError = true
        launchHorizon()
        tap("Transactions")
        awaitText("Ledger payment 1")
        compose.onNodeWithText("Currency unavailable", substring = true).assertDoesNotExist()
        screenshot("partial-transactions")
        tap("Overview")
        backend.summaryError = false
        tap("Try again")
        awaitText("Starting Pool (Base Income)")
    }
}
