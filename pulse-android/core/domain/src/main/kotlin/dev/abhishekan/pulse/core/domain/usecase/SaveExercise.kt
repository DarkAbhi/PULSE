package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.ExerciseInput
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository

class SaveExercise(
    private val repository: FitnessRepository,
) {
    suspend operator fun invoke(
        visitId: Long,
        exerciseId: Long?,
        input: ExerciseInput,
    ): Outcome<Unit> =
        if (visitId <= 0 ||
            (exerciseId != null && exerciseId <= 0) ||
            input.issue != null
        ) {
            Outcome.Error(Failure.InvalidInput)
        } else {
            repository.saveExercise(visitId, exerciseId, input.copy(name = input.name.trim()))
        }

    suspend fun batch(
        visitId: Long,
        inputs: List<ExerciseInput>,
    ): Outcome<Unit> =
        if (visitId <= 0 || inputs.size !in 1..20 || inputs.any { it.issue != null }) {
            Outcome.Error(Failure.InvalidInput)
        } else {
            repository.saveExercises(visitId, inputs.map { it.copy(name = it.name.trim()) })
        }
}
