package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption
import dev.abhishekan.pulse.core.domain.model.garageVehicleKinds
import dev.abhishekan.pulse.core.domain.repository.VehicleCatalogRepository

class SearchVehicleCatalog(
    private val repository: VehicleCatalogRepository,
) {
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
