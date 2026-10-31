package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.VehicleCatalogOption

interface VehicleCatalogRepository {
    suspend fun search(
        kind: String,
        makeSlug: String?,
        query: String,
    ): Outcome<List<VehicleCatalogOption>>
}
