package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
class GetWorkoutDetail(private val repository: FitnessRepository) { suspend operator fun invoke(id: Long): Outcome<WorkoutDetail> = if (id > 0) repository.workout(id) else Outcome.Error(Failure.InvalidInput) }
