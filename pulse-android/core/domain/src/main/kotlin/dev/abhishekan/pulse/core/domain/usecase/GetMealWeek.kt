package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository
import java.time.LocalDate

class GetMealWeek(private val repository: MealPlanRepository) {
    val changes get() = repository.changes
    suspend operator fun invoke(start: LocalDate): Outcome<List<PlannedMeal>> =
        if (start.year !in 1..9999 || start.plusDays(6).year !in 1..9999) Outcome.Error(Failure.InvalidInput)
        else repository.meals(start, start.plusDays(6))
}
