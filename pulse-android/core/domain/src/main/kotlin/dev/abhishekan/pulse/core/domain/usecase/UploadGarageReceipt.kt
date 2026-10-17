package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.domain.repository.GarageRepository

class UploadGarageReceipt(private val repository: GarageRepository) {
    suspend operator fun invoke(vehicle: Long, record: Long, source: String, progress: (Float) -> Unit) = if (vehicle > 0 && record > 0 && source.isNotBlank()) repository.upload(vehicle, record, source, progress) else Outcome.Error(Failure.InvalidInput)
}
