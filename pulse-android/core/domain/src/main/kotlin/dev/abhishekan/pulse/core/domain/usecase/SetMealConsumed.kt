package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.PlannedMeal
import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository

class SetMealConsumed(
    private val repository: MealPlanRepository,
) {
    suspend operator fun invoke(
        id: Long,
        value: Boolean,
    ): Outcome<PlannedMeal> = if (id > 0) repository.consumed(id, value) else Outcome.Error(Failure.InvalidInput)
}
