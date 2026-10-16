package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.DashboardRepository

class GetProfile(private val repository: DashboardRepository) { suspend operator fun invoke() = repository.profile() }
