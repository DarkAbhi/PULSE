package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.GarageRepository

class GetVehicleHistory(private val repository: GarageRepository) {
    suspend operator fun invoke(vehicle: Long) = if (vehicle > 0) repository.history(vehicle) else Outcome.Error(Failure.InvalidInput)
}
