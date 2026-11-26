package dev.abhishekan.pulse.core.data

import dev.abhishekan.pulse.core.domain.model.FitnessToday
import dev.abhishekan.pulse.core.domain.model.RecentActivityEntry
import dev.abhishekan.pulse.core.domain.repository.DashboardFeedRepository
import dev.abhishekan.pulse.core.network.DashboardFeedApi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.Instant
import java.time.LocalDate

class PulseDashboardFeedRepository(
    private val api: DashboardFeedApi,
    private val auth: PulseAuthRepository,
) : DashboardFeedRepository {
    override suspend fun fitnessToday() =
        auth.authenticatedRequest {
            val response = api.fitnessToday()
            FitnessToday(
                LocalDate.parse(response.date),
                response.steps,
                response.moveCalories,
                response.moveCaloriesGoal,
                response.exerciseMinutes,
                response.exerciseMinutesGoal,
                response.standHours,
                response.standHoursGoal,
                response.asleepSeconds,
            )
        }

    override suspend fun recentActivity() =
        auth.authenticatedRequest {
            api.recentActivity().mapNotNull { item ->
                val date = runCatching { Instant.parse(item.recordedAt) }.getOrNull() ?: return@mapNotNull null
                val details =
                    item.details
                        .orEmpty()
                        .mapNotNull { (key, value) ->
                            (value as? JsonPrimitive)?.contentOrNull?.let { key to it }
                        }.toMap()
                RecentActivityEntry(item.id, item.kind, date, details)
            }
        }
}
