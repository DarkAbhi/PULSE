package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
class DeleteWorkout(private val repository: FitnessRepository) { suspend operator fun invoke(id: Long): Outcome<Unit> = if (id > 0) repository.deleteWorkout(id) else Outcome.Error(Failure.InvalidInput) }
