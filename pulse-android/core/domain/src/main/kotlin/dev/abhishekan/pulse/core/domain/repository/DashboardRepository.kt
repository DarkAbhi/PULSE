package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.WorkoutToday

interface DashboardRepository {
    suspend fun profile(): Outcome<Profile>
    suspend fun workout(): Outcome<WorkoutToday>
    suspend fun horizon(): Outcome<Horizon>
    suspend fun notifications(): Outcome<List<Notification>>
}
