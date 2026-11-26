package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository

class DeleteExercise(
    private val repository: FitnessRepository,
) {
    suspend operator fun invoke(
        visitId: Long,
        exerciseId: Long,
    ): Outcome<Unit> =
        if (visitId >
            0 &&
            exerciseId > 0
        ) {
            repository.deleteExercise(visitId, exerciseId)
        } else {
            Outcome.Error(Failure.InvalidInput)
        }
}
