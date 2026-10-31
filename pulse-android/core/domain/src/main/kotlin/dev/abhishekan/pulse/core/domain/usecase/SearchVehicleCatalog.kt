package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogMatch
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.domain.model.garageVehicleKinds
import dev.abhishekan.pulse.core.domain.repository.VehicleCatalogRepository

class SearchVehicleCatalog(
    private val repository: VehicleCatalogRepository,
) {
    suspend fun match(
        kind: String,
        makeName: String,
        modelName: String,
    ): Outcome<VehicleCatalogMatch?> =
        if (kind !in garageVehicleKinds || listOf(makeName, modelName).any { it.isBlank() || it.codePointCount(0, it.length) > 200 }) {
            Outcome.Error(Failure.InvalidInput)
        } else {
            repository.match(kind, makeName.trim(), modelName.trim())
        }

    suspend operator fun invoke(
        kind: String,
        makeSlug: String?,
        query: String,
    ): Outcome<List<VehicleCatalogOption>> =
        if (kind !in garageVehicleKinds || query.codePointCount(0, query.length) > 100 || makeSlug?.isBlank() == true) {
            Outcome.Error(Failure.InvalidInput)
        } else {
            repository.search(kind, makeSlug, query.trim())
        }
}
