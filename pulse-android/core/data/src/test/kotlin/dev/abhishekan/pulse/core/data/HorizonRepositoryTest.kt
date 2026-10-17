package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.network.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import java.io.Closeable
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class HorizonRepositoryTest {
    private class Harness : Closeable {
        private val store = object : SessionMaterialStore {
            var bytes: ByteArray? = null
            override suspend fun read() = bytes
            override suspend fun write(material: ByteArray) { bytes = material }
            override suspend fun clear() { bytes = null }
        }
        private val config = NetworkConfig("https://finance.example.test/api/", false)
        private val clock = Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC)
        private val cookies = PersistentSessionCookies(store, config, clock)
        var status = HttpStatusCode.OK
        var failure: Exception? = null
        var malformed = false
        var query: Parameters = Parameters.Empty
        val writes = mutableListOf<Pair<String, String>>()
        private val transaction = """{"id":5,"name":"Tea","amount":12.1234567890123456789,"type":"debit","transaction_date":"2026-10-17T00:30:00Z","category_id":null,"budget_id":3,"subscription_id":null,"notes":null}"""
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            if (path != "/api/auth/login") failure?.let { throw it }
            val mutation = request.method != HttpMethod.Get && path != "/api/auth/login"
            if (mutation) writes += "${request.method.value} $path" to ((request.body as? TextContent)?.text ?: "")
            val body = when {
                path == "/api/auth/login" -> """{"username":"fixture-user"}"""
                malformed -> """{"currency":null}"""
                mutation -> "{}"
                path == "/api/horizon" -> """{"currency":"USD","base_amount":100.1234567890123456789,"remaining_amount":-1.25,"total_deductions":80,"total_transactions":22,"total_budgets_allocated":25,"total_subscription_burn":21.3734567890123456789,"committed_ratio":101.25,"budgets":[{"id":3,"name":"Food","allocated_amount":25,"used_amount":12,"available_amount":13,"usage_percentage":48}],"deductions":null,"categories":null,"subscriptions":null,"projections":[{"months":3,"label":"3 Months","cumulative_uncommitted":0}]}"""
                path == "/api/horizon/transactions" -> { query = request.url.parameters; """{"transactions":[$transaction],"page":3,"page_size":20,"total":41,"total_pages":3}""" }
                path.endsWith("/transactions") -> "[$transaction]"
                path == "/api/next-month-purchases" -> """{"items":null,"total":0}"""
                else -> error("Unexpected fixture path $path")
            }
            respond(body, if (path == "/api/auth/login") HttpStatusCode.OK else status, Headers.build {
                append(HttpHeaders.ContentType, "application/json")
                if (path == "/api/auth/login") append(HttpHeaders.SetCookie, "life_session=test-fixture; Path=/; Max-Age=3600; Secure; HttpOnly")
            })
        }
        val client = pulseHttpClient(config, engine, cookies)
        val auth = PulseAuthRepository(PulseApi(client), cookies, clock)
        val repo = PulseHorizonRepository(HorizonApi(client), auth)
        suspend fun login() { auth.restore(); assertEquals(Outcome.Success(Unit), auth.login("fixture-user", "test-only")) }
        override fun close() { client.close() }
    }
    @Test fun summaryUsesBackendValuesExactDecimalsAndNullCollections() = runTest {
        Harness().use { h ->
            val data = (h.repo.overview() as Outcome.Success).value
            assertEquals("100.1234567890123456789", data.baseAmount.toPlainString())
            assertEquals("-1.25", data.remaining.toPlainString()); assertTrue(data.subscriptions.isEmpty())
            assertEquals("13", data.budgets.single().available.toPlainString())
            assertEquals("0", data.projections.single().amount.toPlainString())
        }
    }
    @Test fun paginatedSearchEncodesFiltersAndHonorsServerClampedPage() = runTest {
        Harness().use { h ->
            val page = (h.repo.transactions(99, "tea & cake", "debit") as Outcome.Success).value
            assertEquals(3, page.page); assertEquals(41, page.total); assertEquals(3, page.totalPages)
            assertEquals("99", h.query["page"]); assertEquals("20", h.query["page_size"]); assertEquals("tea & cake", h.query["search"]); assertEquals("debit", h.query["type"])
            assertEquals("12.1234567890123456789", page.items.single().amount.toPlainString())
        }
    }
    @Test fun nonPositiveTransactionAmountsAreRejectedAsMalformed() {
        val dto = dev.abhishekan.pulse.core.network.dto.HorizonTransactionDto(1, "Invalid", JsonPrimitive(-1), date = "2026-10-17T00:00:00Z")
        try { dto.domain(); fail("Negative transaction accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun optionalPurchaseAndLinkedTransactionResponsesAreMapped() = runTest {
        Harness().use { h ->
            assertTrue((h.repo.purchases() as Outcome.Success).value.items.isEmpty())
            val tx = (h.repo.payments(2) as Outcome.Success).value.single()
            assertNull(tx.subscriptionId); assertEquals(3L, tx.budgetId); assertNull(tx.notes)
        }
    }
    @Test fun writesKeepNumericPrecisionExplicitNullAndAllAssociations() = runTest {
        Harness().use { h ->
            val amount = "123.1234567890123456789".toBigDecimal()
            h.repo.apply(HorizonChange.Transaction(5, "Edited", amount, "credit", Instant.parse("2026-10-17T01:02:03.123Z"), null, 2, null, null))
            val (path, body) = h.writes.single()
            assertEquals("PUT /api/horizon/transactions/5", path)
            assertTrue(body.contains("123.1234567890123456789"))
            val json = Json.parseToJsonElement(body).jsonObject
            assertEquals(JsonNull, json["subscription_id"]); assertEquals(JsonNull, json["category_id"]); assertEquals(JsonPrimitive(2), json["budget_id"])
            assertEquals("credit", json["type"]?.jsonPrimitive?.content)
            assertEquals("2026-10-17T01:02:03.123Z", json["transaction_date"]?.jsonPrimitive?.content)
        }
    }
    @Test fun everyFinanceMutationUsesExistingApiAndInvalidatesObservers() = runTest {
        Harness().use { h ->
            val one = "1.25".toBigDecimal()
            val changes = listOf(HorizonChange.Config(one, "USD"), HorizonChange.Budget(null, "B", one), HorizonChange.Budget(1, "B", one),
                HorizonChange.Deduction(null, "D", one, "bill", 31, false, 1), HorizonChange.Deduction(2, "D", one, "bill", null, true, null),
                HorizonChange.Subscription(null, "S", one, "yearly", null, LocalDate.of(2028,2,29), "paused", 1, 2, 3, "Note"),
                HorizonChange.Subscription(3, "S", one, "monthly", 1, null, "cancelled", null, null, null, null),
                HorizonChange.Category("Category", "tag", "#ffffff"), HorizonChange.Purchase("P", one, null), HorizonChange.LinkPayment(3, 5),
                HorizonChange.Delete(HorizonRecord.Budget, 1), HorizonChange.Delete(HorizonRecord.Deduction, 2),
                HorizonChange.Delete(HorizonRecord.Transaction, 5), HorizonChange.Delete(HorizonRecord.Subscription, 3), HorizonChange.Delete(HorizonRecord.Purchase, 4), HorizonChange.ClearPurchases)
            changes.forEach { assertEquals(Outcome.Success(Unit), h.repo.apply(it)) }
            assertEquals(changes.size.toLong(), h.repo.changes.value)
            assertEquals(listOf("PUT /api/horizon/config", "POST /api/horizon/budgets", "PUT /api/horizon/budgets/1", "POST /api/horizon/deductions", "PUT /api/horizon/deductions/2",
                "POST /api/horizon/subscriptions", "PUT /api/horizon/subscriptions/3", "POST /api/horizon/categories", "POST /api/next-month-purchases", "POST /api/horizon/subscriptions/3/transactions",
                "DELETE /api/horizon/budgets/1", "DELETE /api/horizon/deductions/2", "DELETE /api/horizon/transactions/5", "DELETE /api/horizon/subscriptions/3", "DELETE /api/next-month-purchases/4", "DELETE /api/next-month-purchases"), h.writes.map { it.first })
            val subscription = Json.parseToJsonElement(h.writes[5].second).jsonObject
            assertEquals(JsonPrimitive(3), subscription["deduction_id"]); assertEquals(JsonPrimitive("2028-02-29"), subscription["renewal_date"])
        }
    }
    @Test fun lostResponsesInvalidateButCancellationPropagates() = runTest {
        Harness().use { h ->
            h.failure = SocketTimeoutException()
            assertEquals(Outcome.Error(Failure.Timeout), h.repo.apply(HorizonChange.ClearPurchases)); assertEquals(1L, h.repo.changes.value)
            h.failure = CancellationException("cancelled")
            try { h.repo.apply(HorizonChange.ClearPurchases); fail("Cancellation swallowed") } catch (_: CancellationException) { }
            assertEquals(1L, h.repo.changes.value)
        }
    }
    @Test fun networkServerMalformedAndExpiredSessionsStayDomainOutcomes() = runTest {
        Harness().use { h ->
            h.login(); h.failure = IOException(); assertEquals(Outcome.Error(Failure.Connection), h.repo.overview())
            h.failure = null; h.status = HttpStatusCode.InternalServerError; assertEquals(Outcome.Error(Failure.Server), h.repo.overview())
            h.status = HttpStatusCode.OK; h.malformed = true; assertEquals(Outcome.Error(Failure.Malformed), h.repo.overview())
            h.status = HttpStatusCode.Unauthorized; assertEquals(Outcome.Error(Failure.Unauthorized), h.repo.overview())
            assertTrue(h.auth.session.value is SessionState.SignedOut)
        }
    }
}
