package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.FitnessRepository
class ObserveFitnessChanges(private val repository: FitnessRepository) { operator fun invoke() = repository.changes }
