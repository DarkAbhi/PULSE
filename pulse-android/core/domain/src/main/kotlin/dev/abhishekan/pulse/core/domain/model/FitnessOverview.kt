package dev.abhishekan.pulse.core.domain.model

import java.time.LocalDate

data class FitnessOverview(
    val today: LocalDate,
    val metrics: List<FitnessMetric>,
    val workouts: List<WorkoutSummary>,
)
