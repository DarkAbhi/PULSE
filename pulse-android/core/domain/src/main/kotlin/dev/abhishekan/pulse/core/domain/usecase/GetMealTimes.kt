package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository

class GetMealTimes(
    private val repository: MealPlanRepository,
) {
    suspend operator fun invoke() = repository.times()
}
