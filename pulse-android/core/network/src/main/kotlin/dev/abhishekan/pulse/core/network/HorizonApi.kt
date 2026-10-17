package dev.abhishekan.pulse.core.network

import dev.abhishekan.pulse.core.network.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonObject

class HorizonApi(private val client: HttpClient) {
    suspend fun overview(): HorizonOverviewDto = client.get("horizon").body()
    suspend fun transactions(page: Int, search: String, type: String?): HorizonTransactionPageDto = client.get("horizon/transactions") {
        parameter("page", page); parameter("page_size", 20); parameter("search", search); parameter("type", type)
    }.body()
    suspend fun purchases(): PlannedPurchasesDto = client.get("next-month-purchases").body()
    suspend fun payments(id: Long): List<HorizonTransactionDto> = client.get("horizon/subscriptions/$id/transactions").body()
    suspend fun save(path: String, id: Long?, body: JsonObject) {
        if (id == null) client.post(path) { setBody(body) } else client.put("$path/$id") { setBody(body) }
    }
    suspend fun config(body: JsonObject) { client.put("horizon/config") { setBody(body) } }
    suspend fun delete(path: String) { client.delete(path) }
}
