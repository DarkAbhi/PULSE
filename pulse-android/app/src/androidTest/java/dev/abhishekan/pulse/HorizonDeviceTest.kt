package dev.abhishekan.pulse
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.abhishekan.pulse.core.data.EncryptedSessionStore
import dev.abhishekan.pulse.core.data.PulseAuthRepository
import dev.abhishekan.pulse.core.data.PulseDashboardRepository
import dev.abhishekan.pulse.core.data.PulseHorizonRepository
import dev.abhishekan.pulse.core.data.di.dataModule
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
                                put("total", 12.5)
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
                currency =
                    input.getValue("currency").jsonPrimitive.content
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
            compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(text: String) {
        awaitText(text)
        val node = compose.onNodeWithText(text)
        if (text !in listOf("Home", "Spaces", "You")) node.performScrollTo()
        node.performClick()
    }

    private fun field(
        label: String,
        value: String,
    ) {
        compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)
    }

    private fun confirm(text: String) {
        compose.onNode(hasText(text) and hasAnyAncestor(isDialog())).performClick()
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
        tap("Open Financial Horizon")
        awaitText("Your room to move")
    }

    private fun addBudget(name: String = "Travel") {
        tap("Budgets")
        tap("Add budget")
        field("Name", name)
        field("Amount (USD)", "150.25")
        tap("Save budget")
        awaitText(name)
    }

    private fun screenshot(
        name: String,
        dialog: Boolean = false,
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
            .performClick()
    }

    @Test fun configurationBudgetsAndObligationsWorkAndHomeRefreshes() {
        launchHorizon()
        screenshot("overview")
        tap("Configure starting pool")
        field("Amount (USD)", "2000.25")
        field("Currency", "USD")
        tap("Save starting pool")
        awaitText("USD 1910.25")
        tap("Home")
        awaitText("Asha")
        compose.onNodeWithText("USD 1910.25").performScrollTo().assertIsDisplayed()
        tap("Spaces")
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
        field("Amount (USD)", "175.50")
        tap("Save budget")
        awaitText("Travel fund")
        tap("Obligations")
        tap("Add obligation")
        field("Name", "Internet")
        field("Amount (USD)", "50.25")
        field("Due day (1–31, optional)", "31")
        pick("Budget", "None", "Travel fund")
        tap("Active obligation")
        tap("Save fixed obligation")
        awaitText("Internet")
        val obligation = backend.items("deductions").values.last()
        assertEquals(false, obligation["is_active"]?.jsonPrimitive?.boolean)
        assertEquals(31, obligation["due_day"]?.jsonPrimitive?.int)
        cardAction("Internet", "Edit")
        tap("Active obligation")
        tap("Save fixed obligation")
        awaitText("Internet")
        tap("Budgets")
        cardAction("Travel fund", "Delete")
        awaitText("Confirm deletion")
        confirm("Delete")
        awaitText("Everyday")
        assertEquals(JsonNull, backend.items("deductions").values.last()["budget_id"])
        tap("Obligations")
        cardAction("Internet", "Delete")
        confirm("Delete")
        awaitText("Rent")
        assertEquals(1, backend.items("deductions").size)
    }

    @Test fun transactionPaginationCategoryAssociationsAndCrud() {
        launchHorizon()
        tap("Transactions")
        tap("Next page")
        awaitText("Page 2 of 2 · 21 transactions")
        tap("Create category")
        field("Name", "Coffee")
        tap("Save category")
        awaitText("The everyday ledger")
        tap("Add transaction")
        field("Name", "Coffee refund")
        field("Amount (USD)", "12.50")
        pick("Type", "Debit", "Credit")
        pick("Category", "None", "Coffee")
        pick("Budget", "None", "Everyday")
        pick("Subscription", "None", "Music")
        field("Notes (optional)", "Receipt refunded")
        screenshot("transaction-editor")
        tap("Save transaction")
        awaitText("The everyday ledger")
        field("Search transactions", "Coffee refund")
        tap("Apply search")
        awaitText("Coffee refund")
        screenshot("transactions")
        val tx = backend.items("transactions").values.last()
        assertEquals("credit", tx["type"]?.jsonPrimitive?.content)
        assertEquals(1L, tx["budget_id"]?.jsonPrimitive?.long)
        assertEquals(4L, tx["subscription_id"]?.jsonPrimitive?.long)
        cardAction("Coffee refund", "Edit")
        field("Name", "Coffee expense")
        pick("Type", "Credit", "Debit")
        pick("Subscription", "Music", "None")
        tap("Save transaction")
        field("Search transactions", "Coffee expense")
        tap("Apply search")
        awaitText("Coffee expense")
        cardAction("Coffee expense", "Delete")
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
        tap("Add subscription")
        field("Name", "Streaming")
        field("Amount (USD)", "99.99")
        pick("Billing cycle", "Monthly", "Yearly")
        pick("Status", "Active", "Paused")
        pick("Budget", "None", "Everyday")
        pick("Fixed obligation", "None", "Rent")
        field("Billing day (1–31, optional)", "20")
        tap("Save subscription")
        awaitText("Streaming")
        cardAction("Streaming", "Edit")
        pick("Status", "Paused", "Active")
        tap("Save subscription")
        awaitText("Streaming")
        tap("View payments for Streaming")
        awaitText("Link an existing payment")
        compose.onNodeWithText("Link Ledger payment 2 · USD 2.50").assertDoesNotExist()
        compose.onNodeWithText("Link Ledger payment 3 · USD 2.50").assertDoesNotExist()
        tap("Link Ledger payment 1 · USD 2.50")
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
        cardAction("Streaming", "Delete")
        confirm("Delete")
        awaitText("Music")
        compose.onNodeWithText("Close payments").assertDoesNotExist()
        assertEquals(21, backend.items("transactions").size)
        assertEquals(JsonNull, backend.items("transactions").getValue(1)["subscription_id"])
        assertTrue(backend.items("deductions").containsKey(2))
    }

    @Test fun plannedPurchasesRequireConfirmationForIndividualAndClear() {
        launchHorizon()
        tap("Planned purchases")
        awaitText("Nothing planned yet")
        for (name in listOf("Desk lamp", "Backpack")) {
            tap("Add planned purchase")
            compose.onNodeWithText("Add planned purchase").assertIsDisplayed()
            field("Name", name)
            field("Amount (USD)", "12.50")
            field("URL (optional)", "https://example.test/item")
            tap("Save planned purchase")
            awaitText(name)
        }
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

    @Test fun draftsSurviveRotationTabsAndKeyboardAndWarnBeforeDiscard() {
        launchHorizon()
        tap("Transactions")
        tap("Add transaction")
        field("Name", "Unsaved finance")
        field("Amount (USD)", "0.001")
        tap("Save transaction")
        assertEquals(0, backend.mutations)
        scenario!!.recreate()
        awaitText("Unsaved finance")
        tap("Home")
        tap("Spaces")
        awaitText("Unsaved finance")
        tap("You")
        tap("Log out")
        awaitText("Log out and discard edits?")
        confirm("Cancel")
        tap("Spaces")
        awaitText("Unsaved finance")
        compose.onNodeWithText("Name").performScrollTo().performImeAction()
        screenshot("draft")
        tap("Cancel editing")
        awaitText("Discard unsaved changes?")
        confirm("Discard edits")
        awaitText("The everyday ledger")
    }

    @Test fun uncertainSaveRefreshesAndBlocksDuplicatesUntilRecordsReviewed() {
        launchHorizon()
        tap("Budgets")
        tap("Add budget")
        field("Name", "Lost response")
        field("Amount (USD)", "30.25")
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
        awaitText("Give every priority room")
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
            tap("Add transaction")
            field("Name", "Resized draft")
            field("Amount (USD)", "75.25")
            compose.onNodeWithText("Name").performScrollTo().performImeAction()
            compose.onNodeWithText("Amount (USD)").assertIsFocused()
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
            awaitText("The everyday ledger")
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
        compose
            .onAllNodesWithText(
                "Currency unavailable",
                substring = true,
            ).onFirst()
            .performScrollTo()
            .assertIsDisplayed()
        screenshot("partial-transactions")
        tap("Overview")
        backend.summaryError = false
        tap("Try again")
        awaitText("UNCOMMITTED THIS MONTH")
    }
}
