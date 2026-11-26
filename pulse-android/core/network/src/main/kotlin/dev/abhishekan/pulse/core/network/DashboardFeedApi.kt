package dev.abhishekan.pulse.core.network

import dev.abhishekan.pulse.core.network.dto.FitnessTodayDto
import dev.abhishekan.pulse.core.network.dto.RecentActivityDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class DashboardFeedApi(
    private val client: HttpClient,
) {
    suspend fun fitnessToday(): FitnessTodayDto = client.get("fitness/today").body()

    suspend fun recentActivity(): List<RecentActivityDto> = client.get("activity/recent").body()
}
