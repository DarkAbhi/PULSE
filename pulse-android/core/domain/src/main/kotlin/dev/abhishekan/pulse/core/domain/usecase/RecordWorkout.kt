package dev.abhishekan.pulse.core.domain.usecase
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
import java.time.LocalDate

class RecordWorkout(
    private val repository: FitnessRepository,
) {
    suspend operator fun invoke(date: LocalDate?): Outcome<Long> =
        if (date != null &&
            date.year !in 0..9999
        ) {
            Outcome.Error(Failure.InvalidInput)
        } else {
            repository.record(date)
        }
}
