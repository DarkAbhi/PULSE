package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogMatch
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption

interface VehicleCatalogRepository {
    suspend fun match(
        kind: String,
        makeName: String,
        modelName: String,
    ): Outcome<VehicleCatalogMatch?>

    suspend fun search(
        kind: String,
        makeSlug: String?,
        query: String,
    ): Outcome<List<VehicleCatalogOption>>
}
