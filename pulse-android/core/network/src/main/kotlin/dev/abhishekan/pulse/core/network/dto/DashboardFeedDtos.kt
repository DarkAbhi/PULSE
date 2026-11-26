package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class FitnessTodayDto(
    val date: String,
    @SerialName("steps_count") val steps: Int? = null,
    @SerialName("move_calories") val moveCalories: Double? = null,
    @SerialName("move_calories_goal") val moveCaloriesGoal: Double? = null,
    @SerialName("exercise_minutes") val exerciseMinutes: Int? = null,
    @SerialName("exercise_minutes_goal") val exerciseMinutesGoal: Int? = null,
    @SerialName("stand_hours") val standHours: Int? = null,
    @SerialName("stand_hours_goal") val standHoursGoal: Int? = null,
    @SerialName("total_asleep_seconds") val asleepSeconds: Long? = null,
)

@Serializable
data class RecentActivityDto(
    val id: String,
    val kind: String,
    @SerialName("recorded_at") val recordedAt: String,
    val details: JsonObject? = null,
)
