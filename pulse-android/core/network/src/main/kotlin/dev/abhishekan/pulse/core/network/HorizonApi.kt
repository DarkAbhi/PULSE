package dev.abhishekan.pulse.core.network
import dev.abhishekan.pulse.core.network.dto.FinancialAccountsDto
import dev.abhishekan.pulse.core.network.dto.HorizonOverviewDto
import dev.abhishekan.pulse.core.network.dto.HorizonTransactionDto
import dev.abhishekan.pulse.core.network.dto.HorizonTransactionPageDto
import dev.abhishekan.pulse.core.network.dto.PlannedPurchasesDto
import dev.abhishekan.pulse.core.network.dto.StatementBatchDto
import dev.abhishekan.pulse.core.network.dto.StatementCommitDto
import dev.abhishekan.pulse.core.network.dto.StatementRowsDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.JsonObject

class HorizonApi(
    private val client: HttpClient,
) {
    suspend fun overview(): HorizonOverviewDto = client.get("horizon").body()

    suspend fun transactions(
        page: Int,
        search: String,
        type: String?,
        categoryId: Long?,
        pageSize: Int,
    ): HorizonTransactionPageDto =
        client
            .get("horizon/transactions") {
                parameter("page", page)
                parameter("page_size", pageSize)
                parameter("search", search)
                parameter("type", type)
                parameter("category_id", categoryId)
            }.body()

    suspend fun accounts(includeArchived: Boolean): FinancialAccountsDto =
        client.get("horizon/accounts") { parameter("include_archived", includeArchived) }.body()

    suspend fun archiveAccount(
        id: Long,
        archived: Boolean,
    ) {
        val action = if (archived) "archive" else "restore"
        client.post("horizon/accounts/$id/$action")
    }

    suspend fun uploadStatement(
        accountId: Long,
        pdf: ByteArray,
        progress: (Float?) -> Unit,
    ): StatementBatchDto =
        client
            .post("horizon/statement-imports") {
                timeout {
                    requestTimeoutMillis = 185_000
                    socketTimeoutMillis = 185_000
                }
                onUpload { sent, total ->
                    progress(total?.takeIf { it > 0 }?.let { sent.toFloat() / it })
                }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("account_id", accountId.toString())
                            append(
                                "file",
                                pdf,
                                Headers.build {
                                    append(HttpHeaders.ContentDisposition, "filename=statement.pdf")
                                    append(HttpHeaders.ContentType, "application/pdf")
                                },
                            )
                        },
                    ),
                )
            }.body()

    suspend fun statementBatch(id: String): StatementBatchDto = client.get("horizon/statement-imports/$id").body()

    suspend fun statementRows(
        id: String,
        page: Int,
    ): StatementRowsDto = client.get("horizon/statement-imports/$id/rows") { parameter("page", page) }.body()

    suspend fun editStatement(
        id: String,
        changes: JsonObject,
    ): StatementBatchDto = client.patch("horizon/statement-imports/$id/rows") { setBody(changes) }.body()

    suspend fun commitStatement(
        id: String,
        revision: JsonObject,
    ): StatementCommitDto = client.post("horizon/statement-imports/$id/commit") { setBody(revision) }.body()

    suspend fun cancelStatement(id: String) {
        client.delete("horizon/statement-imports/$id")
    }

    suspend fun purchases(): PlannedPurchasesDto = client.get("next-month-purchases").body()

    suspend fun payments(id: Long): List<HorizonTransactionDto> = client.get("horizon/subscriptions/$id/transactions").body()

    suspend fun save(
        path: String,
        id: Long?,
        body: JsonObject,
    ) {
        if (id ==
            null
        ) {
            client.post(path) { setBody(body) }
        } else {
            client.put("$path/$id") { setBody(body) }
        }
    }

    suspend fun config(body: JsonObject) {
        client.put("horizon/config") { setBody(body) }
    }

    suspend fun delete(path: String) {
        client.delete(path)
    }
}
