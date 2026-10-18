package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.DashboardRepository

class ObserveDashboardChanges(
    private val repository: DashboardRepository,
) {
    operator fun invoke() = repository.changes
}
