package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.GarageRepository

class GetVehicles(private val repository: GarageRepository) {
    suspend operator fun invoke() = repository.vehicles()
}
