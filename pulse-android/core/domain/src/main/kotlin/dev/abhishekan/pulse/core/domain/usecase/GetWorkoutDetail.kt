package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.WorkoutDetail
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository

class GetWorkoutDetail(
    private val repository: FitnessRepository,
) {
    suspend operator fun invoke(id: Long): Outcome<WorkoutDetail> =
        if (id >
            0
        ) {
            repository.workout(id)
        } else {
            Outcome.Error(Failure.InvalidInput)
        }
}
