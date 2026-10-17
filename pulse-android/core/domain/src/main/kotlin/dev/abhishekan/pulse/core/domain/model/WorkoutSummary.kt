package dev.abhishekan.pulse.core.domain.model

import java.time.LocalDate

data class WorkoutSummary(
    val id: Long,
    val date: LocalDate,
    val timeLabel: String,
)
