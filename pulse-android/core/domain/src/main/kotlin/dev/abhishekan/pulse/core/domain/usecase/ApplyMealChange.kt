package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.MealPlanRepository

class ApplyMealChange(private val repository: MealPlanRepository) {
    fun valid(change: MealChange): Boolean {
        fun String.validName(max: Int) = trim().let { it.isNotEmpty() && it.codePointCount(0, it.length) <= max && '\u0000' !in it }
        return when (change) {
            is MealChange.Save -> change.name.validName(255) && change.date.year in 1..9999 && (change.id == null || change.id > 0) && (change.timeId == null || change.timeId > 0)
            is MealChange.Delete -> change.id > 0
            // The API accepts overnight and equal endpoints; do not impose an unsupported ordering rule.
            is MealChange.CreateTime -> change.name.validName(100) && change.start.nano == 0 && change.end.nano == 0
            is MealChange.DeleteTime -> change.time.id > 0 && !change.time.isDefault
        }
    }
    suspend operator fun invoke(change: MealChange): Outcome<Unit> = if (valid(change)) repository.apply(change) else Outcome.Error(Failure.InvalidInput)
}
