package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.*
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface MealPlanRepository {
    val changes: StateFlow<Long>
    suspend fun meals(start: LocalDate, end: LocalDate): Outcome<List<PlannedMeal>>
    suspend fun times(): Outcome<List<MealTime>>
    suspend fun apply(change: MealChange): Outcome<Unit>
    suspend fun consumed(id: Long, value: Boolean): Outcome<PlannedMeal>
}
