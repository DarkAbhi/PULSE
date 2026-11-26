package dev.abhishekan.pulse.core.network

import dev.abhishekan.pulse.core.network.dto.QuestActivityDto
import dev.abhishekan.pulse.core.network.dto.QuestOverviewDto
import dev.abhishekan.pulse.core.network.dto.QuestSettingsDto
import dev.abhishekan.pulse.core.network.dto.QuestSettingsViewDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class QuestApi(
    private val client: HttpClient,
) {
    suspend fun overview(week: String?): QuestOverviewDto =
        client
            .get("quests/overview") {
                week?.let { parameter("week", it) }
            }.body()

    suspend fun activity(): QuestActivityDto = client.get("quests/activity").body()

    suspend fun settings(): QuestSettingsViewDto = client.get("quests/settings").body()

    suspend fun saveSettings(settings: QuestSettingsDto) {
        client.put("quests/settings") { setBody(settings) }
    }
}
