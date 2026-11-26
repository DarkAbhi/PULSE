package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.WorkoutClassification
import dev.abhishekan.pulse.core.domain.model.WorkoutType
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository

class UpdateWorkoutClassification(
    private val fitness: FitnessRepository,
) {
    suspend operator fun invoke(
        id: Long,
        classification: WorkoutClassification,
    ): Outcome<Unit> =
        if (id <= 0 || classification.type == WorkoutType.RestDay || !classification.validFocus) {
            Outcome.Error(Failure.InvalidInput)
        } else {
            fitness.updateClassification(id, classification)
        }
}
