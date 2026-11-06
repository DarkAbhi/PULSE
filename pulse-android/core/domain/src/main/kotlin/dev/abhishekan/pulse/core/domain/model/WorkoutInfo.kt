package dev.abhishekan.pulse.core.domain.model

import java.math.BigDecimal
import java.time.Instant

data class WorkoutInfo(
    val id: Long,
    val createdAt: Instant,
    val start: Instant?,
    val end: Instant?,
    val durationSeconds: BigDecimal?,
    val calories: BigDecimal?,
    val imported: Boolean,
)
