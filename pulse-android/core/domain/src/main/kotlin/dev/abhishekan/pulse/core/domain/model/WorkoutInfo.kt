package dev.abhishekan.pulse.core.domain.model

import java.time.Instant
import java.math.BigDecimal

data class WorkoutInfo(val id: Long, val createdAt: Instant, val start: Instant?, val end: Instant?,
    val durationSeconds: BigDecimal?, val calories: BigDecimal?, val imported: Boolean)
