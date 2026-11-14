package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository

class DeleteWorkout(
    private val repository: FitnessRepository,
) {
    suspend operator fun invoke(id: Long): Outcome<Unit> =
        if (id >
            0
        ) {
            repository.deleteWorkout(id)
        } else {
            Outcome.Error(Failure.InvalidInput)
        }
}
