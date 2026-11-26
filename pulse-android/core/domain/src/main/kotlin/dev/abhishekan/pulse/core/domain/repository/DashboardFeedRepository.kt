package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.FitnessToday
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.RecentActivityEntry

interface DashboardFeedRepository {
    suspend fun fitnessToday(): Outcome<FitnessToday>

    suspend fun recentActivity(): Outcome<List<RecentActivityEntry>>
}
