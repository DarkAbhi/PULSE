package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.repository.DashboardRepository

class GetWorkoutToday(private val repository: DashboardRepository) { suspend operator fun invoke() = repository.workout() }
