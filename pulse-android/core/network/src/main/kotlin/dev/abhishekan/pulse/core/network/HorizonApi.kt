package dev.abhishekan.pulse.core.network
import dev.abhishekan.pulse.core.network.dto.HorizonOverviewDto
import dev.abhishekan.pulse.core.network.dto.HorizonTransactionDto
import dev.abhishekan.pulse.core.network.dto.HorizonTransactionPageDto
import dev.abhishekan.pulse.core.network.dto.PlannedPurchasesDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonObject

class HorizonApi(
    private val client: HttpClient,
) {
    suspend fun overview(): HorizonOverviewDto = client.get("horizon").body()

    suspend fun transactions(
        page: Int,
        search: String,
        type: String?,
    ): HorizonTransactionPageDto =
        client
            .get("horizon/transactions") {
                parameter("page", page)
                parameter("page_size", 20)
                parameter("search", search)
                parameter("type", type)
            }.body()

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
