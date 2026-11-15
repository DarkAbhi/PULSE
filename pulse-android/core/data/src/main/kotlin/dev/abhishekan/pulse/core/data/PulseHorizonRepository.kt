package dev.abhishekan.pulse.core.data
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.PlannedPurchase
import dev.abhishekan.pulse.core.domain.model.PlannedPurchases
import dev.abhishekan.pulse.core.domain.model.StatementBatch
import dev.abhishekan.pulse.core.domain.model.StatementCommit
import dev.abhishekan.pulse.core.domain.model.StatementPreview
import dev.abhishekan.pulse.core.domain.model.StatementResponse
import dev.abhishekan.pulse.core.domain.model.StatementRow
import dev.abhishekan.pulse.core.domain.model.StatementRows
import dev.abhishekan.pulse.core.domain.repository.FinancialAccountsRepository
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import dev.abhishekan.pulse.core.domain.repository.StatementImportRepository
import dev.abhishekan.pulse.core.network.HorizonApi
import dev.abhishekan.pulse.core.network.dto.StatementBatchDto
import dev.abhishekan.pulse.core.network.dto.StatementErrorDto
import dev.abhishekan.pulse.core.network.dto.StatementRowDto
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal

class PulseHorizonRepository(
    private val api: HorizonApi,
    private val auth: PulseAuthRepository,
) : HorizonRepository,
    FinancialAccountsRepository,
    StatementImportRepository {
    private val revision = MutableStateFlow(0L)
    override val changes = revision.asStateFlow()

    private suspend fun <T> request(
        mutation: Boolean = false,
        block: suspend () -> T,
    ): Outcome<T> =
        auth.authenticatedRequest(
            onResult = { if (mutation) revision.update { it + 1 } },
        ) {
            withContext(Dispatchers.Default) { block() }
        }

    override suspend fun accounts(includeArchived: Boolean) =
        request {
            api.accounts(includeArchived).accounts.map {
                FinancialAccount(
                    it.id,
                    it.displayName,
                    it.institutionName,
                    it.accountType,
                    it.bankAccountSubtype,
                    it.currency,
                    it.countryCode,
                    it.lastFour,
                    it.archivedAt != null,
                )
            }
        }

    override suspend fun saveAccount(account: FinancialAccount) =
        request {
            api.save(
                "horizon/accounts",
                account.id,
                buildJsonObject {
                    put("display_name", account.displayName)
                    put("institution_name", account.institutionName)
                    put("account_type", account.accountType)
                    put("bank_account_subtype", account.bankAccountSubtype)
                    put("currency", account.currency)
                    put("country_code", account.countryCode)
                    put("last_four", account.lastFour)
                },
            )
        }

    override suspend fun archiveAccount(
        id: Long,
        archived: Boolean,
    ) = request { api.archiveAccount(id, archived) }

    private suspend fun <T> statementRequest(block: suspend () -> T): StatementResponse<T> {
        val version = auth.version
        val result =
            try {
                StatementResponse.Success(block())
            } catch (error: Exception) {
                val failure = remoteFailure(error)
                if (failure == Failure.Unauthorized) {
                    auth.expire(version)
                    return StatementResponse.Failed(failure)
                }
                val code =
                    if (error is ResponseException) {
                        try {
                            error.response.body<StatementErrorDto>().code
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            ""
                        }
                    } else {
                        ""
                    }
                if (code.isBlank()) StatementResponse.Failed(failure) else StatementResponse.Rejected(code)
            }
        return if (version == auth.version) result else StatementResponse.Failed(Failure.Unauthorized)
    }

    override suspend fun uploadStatement(
        accountId: Long,
        pdf: ByteArray,
        progress: (Float?) -> Unit,
    ) = statementRequest {
        val result = api.uploadStatement(accountId, pdf, progress)
        StatementPreview(
            result.statementBatch(),
            StatementRows(result.rows.map { it.statementRow() }, result.page, result.pageSize, result.total),
        )
    }

    override suspend fun statementBatch(id: String) = statementRequest { api.statementBatch(id).statementBatch() }

    override suspend fun statementRows(
        id: String,
        page: Int,
    ) = statementRequest {
        val result = api.statementRows(id, page)
        StatementRows(result.rows.map { it.statementRow() }, result.page, result.pageSize, result.total)
    }

    override suspend fun editStatement(
        batch: StatementBatch,
        confirmed: Boolean,
        changes: List<Pair<StatementRow, StatementRow>>,
    ) = statementRequest {
        val body =
            buildJsonObject {
                put("revision", batch.revision)
                put("account_confirmed", confirmed)
                put(
                    "changes",
                    JsonArray(
                        changes.map { (original, edited) ->
                            buildJsonObject {
                                put("id", edited.id)
                                if (original.date != edited.date) put("date", edited.date)
                                if (original.description != edited.description) put("description", edited.description)
                                if (original.type != edited.type) put("type", edited.type)
                                if (original.amount != edited.amount) put("amount", edited.amount)
                                if (original.included != edited.included) put("included", edited.included)
                                if (original.categoryId != edited.categoryId) {
                                    put("category_id", edited.categoryId?.let(::JsonPrimitive) ?: JsonNull)
                                }
                            }
                        },
                    ),
                )
            }
        api.editStatement(batch.id, body).statementBatch()
    }

    override suspend fun commitStatement(batch: StatementBatch) =
        statementRequest {
            val result = api.commitStatement(batch.id, buildJsonObject { put("revision", batch.revision) })
            revision.update { it + 1 }
            StatementCommit(result.imported, result.duplicatesSkipped, result.excluded)
        }

    override suspend fun cancelStatement(id: String) = statementRequest { api.cancelStatement(id) }

    private fun StatementBatchDto.statementBatch() =
        StatementBatch(id, currency, revision, ocrUsed, accountConfirmationRequired, counts.parsed, counts.duplicates)

    private fun StatementRowDto.statementRow() =
        StatementRow(id, date, description, amount, type, categoryId, included, duplicateStatus == "skipped", warnings)

    override suspend fun overview() = request { api.overview().domain() }

    override suspend fun transactions(
        page: Int,
        search: String,
        type: String?,
        categoryId: Long?,
        pageSize: Int,
    ) = request {
        val dto = api.transactions(page, search, type, categoryId, pageSize)
        require(dto.page > 0 && dto.pageSize in 1..100 && dto.total >= 0 && dto.totalPages >= 0)
        val items = dto.transactions.orEmpty().map { it.domain() }
        require(items.size <= dto.pageSize)
        HorizonTransactionPage(items, dto.page, dto.pageSize, dto.total, dto.totalPages)
    }

    override suspend fun purchases() =
        request {
            val dto = api.purchases()
            PlannedPurchases(
                dto.items.orEmpty().map {
                    require(
                        it.id > 0 && it.name.isNotBlank() && it.price.horizonDecimal().signum() >= 0,
                    )
                    PlannedPurchase(it.id, it.name, it.price.horizonDecimal(), it.url)
                },
                dto.total.horizonDecimal(),
            )
        }

    override suspend fun payments(subscriptionId: Long) = request { api.payments(subscriptionId).map { it.domain() } }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun apply(change: HorizonChange): Outcome<Unit> =
        request(true) {
            fun BigDecimal.numeric() = JsonUnquotedLiteral(toPlainString())

            fun JsonObjectBuilder.ref(
                key: String,
                id: Long?,
            ) {
                put(
                    key,
                    id?.let(::JsonPrimitive) ?: JsonNull,
                )
            }

            fun JsonObjectBuilder.text(
                key: String,
                value: String?,
            ) {
                put(
                    key,
                    value?.let(::JsonPrimitive) ?: JsonNull,
                )
            }
            when (change) {
                is HorizonChange.Config ->
                    api.config(
                        buildJsonObject {
                            put("base_amount", change.amount.numeric())
                            put("currency", change.currency.trim())
                        },
                    )
                is HorizonChange.Budget ->
                    api.save(
                        "horizon/budgets",
                        change.id,
                        buildJsonObject {
                            put("name", change.name.trim())
                            put("allocated_amount", change.amount.numeric())
                        },
                    )
                is HorizonChange.Deduction ->
                    api.save(
                        "horizon/deductions",
                        change.id,
                        buildJsonObject {
                            put("name", change.name.trim())
                            put("amount", change.amount.numeric())
                            put("category", change.category)
                            put("due_day", change.dueDay?.let(::JsonPrimitive) ?: JsonNull)
                            put("is_active", change.active)
                            ref("budget_id", change.budgetId)
                        },
                    )
                is HorizonChange.Transaction ->
                    api.save(
                        "horizon/transactions",
                        change.id,
                        buildJsonObject {
                            put("name", change.name.trim())
                            put("amount", change.amount.numeric())
                            put("type", change.type)
                            put("transaction_date", change.date.toString())
                            ref("category_id", change.categoryId)
                            ref("budget_id", change.budgetId)
                            ref("subscription_id", change.subscriptionId)
                            if (change.setAccount) ref("account_id", change.accountId)
                            text("notes", change.notes)
                        },
                    )
                is HorizonChange.Subscription ->
                    api.save(
                        "horizon/subscriptions",
                        change.id,
                        buildJsonObject {
                            put("name", change.name.trim())
                            put("amount", change.amount.numeric())
                            put("billing_cycle", change.cycle)
                            put("status", change.status)
                            put("billing_day", change.billingDay?.let(::JsonPrimitive) ?: JsonNull)
                            text("renewal_date", change.renewalDate?.toString())
                            ref("category_id", change.categoryId)
                            ref("budget_id", change.budgetId)
                            ref("deduction_id", change.deductionId)
                            text("notes", change.notes)
                        },
                    )
                is HorizonChange.Category ->
                    api.save(
                        "horizon/categories",
                        null,
                        buildJsonObject {
                            put("name", change.name.trim())
                            put("icon", change.icon.trim())
                            put("color", change.color.trim())
                        },
                    )
                is HorizonChange.Purchase ->
                    api.save(
                        "next-month-purchases",
                        null,
                        buildJsonObject {
                            put("name", change.name.trim())
                            put("price", change.amount.numeric())
                            text("url", change.url)
                        },
                    )
                is HorizonChange.LinkPayment ->
                    api.save(
                        "horizon/subscriptions/${change.subscriptionId}/transactions",
                        null,
                        buildJsonObject {
                            put("transaction_id", change.transactionId)
                        },
                    )
                is HorizonChange.Delete ->
                    api.delete(
                        when (change.kind) {
                            HorizonRecord.Budget -> "horizon/budgets"
                            HorizonRecord.Deduction -> "horizon/deductions"
                            HorizonRecord.Transaction -> "horizon/transactions"
                            HorizonRecord.Subscription -> "horizon/subscriptions"
                            HorizonRecord.Purchase -> "next-month-purchases"
                        } + "/${change.id}",
                    )
                HorizonChange.ClearPurchases -> api.delete("next-month-purchases")
            }
        }
}
