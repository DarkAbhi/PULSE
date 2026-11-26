package dev.abhishekan.pulse.core.domain.model

import java.time.Instant
import java.time.LocalDate

data class FitnessToday(
    val date: LocalDate,
    val steps: Int?,
    val moveCalories: Double?,
    val moveCaloriesGoal: Double?,
    val exerciseMinutes: Int?,
    val exerciseMinutesGoal: Int?,
    val standHours: Int?,
    val standHoursGoal: Int?,
    val asleepSeconds: Long?,
)

data class RecentActivityEntry(
    val id: String,
    val kind: String,
    val recordedAt: Instant,
    val details: Map<String, String>,
)
