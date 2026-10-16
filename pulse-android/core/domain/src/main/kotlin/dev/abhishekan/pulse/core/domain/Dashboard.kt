package dev.abhishekan.pulse.core.domain

import java.math.BigDecimal
import java.time.Instant

data class Profile(val name: String?)
data class WorkoutToday(val visited: Boolean)
data class Horizon(val currency: String, val baseAmount: BigDecimal, val remainingAmount: BigDecimal, val deductions: BigDecimal)
data class Notification(val id: Long, val title: String, val body: String?, val createdAt: Instant?)

interface DashboardRepository {
    suspend fun profile(): Outcome<Profile>
    suspend fun workout(): Outcome<WorkoutToday>
    suspend fun horizon(): Outcome<Horizon>
    suspend fun notifications(): Outcome<List<Notification>>
}

class GetProfile(private val repository: DashboardRepository) { suspend operator fun invoke() = repository.profile() }
class GetWorkoutToday(private val repository: DashboardRepository) { suspend operator fun invoke() = repository.workout() }
class GetHorizon(private val repository: DashboardRepository) { suspend operator fun invoke() = repository.horizon() }
class GetNotifications(private val repository: DashboardRepository) { suspend operator fun invoke() = repository.notifications() }
