package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestSettingsDto(
    @SerialName("gym_workout") val gymWorkout: Boolean,
    @SerialName("step_target") val stepTarget: Int?,
    @SerialName("activity_rings") val activityRings: Boolean,
    @SerialName("sleep_minutes") val sleepMinutes: Int?,
    @SerialName("daily_meals") val dailyMeals: Boolean,
)

@Serializable
data class QuestSettingsViewDto(
    val configured: Boolean,
    val settings: QuestSettingsDto,
    @SerialName("settings_effective") val effective: String? = null,
    @SerialName("has_workout_days") val hasWorkoutDays: Boolean,
)

@Serializable
data class QuestOverviewDto(
    val configured: Boolean,
    val settings: QuestSettingsDto,
    @SerialName("settings_effective") val effective: String? = null,
    @SerialName("has_workout_days") val hasWorkoutDays: Boolean,
    val today: String,
    @SerialName("next_reset_at") val nextResetAt: String,
    val week: List<QuestDayDto>,
    @SerialName("previous_week") val previousWeek: String? = null,
    @SerialName("next_week") val nextWeek: String? = null,
    @SerialName("today_quests") val todayQuests: List<DailyQuestDto>? = null,
    val progress: QuestProgressDto,
)

@Serializable
data class DailyQuestDto(
    val id: String,
    val target: Int,
    val progress: Int,
    val xp: Int,
    val hearts: Int,
    val completed: Boolean,
)

@Serializable
data class QuestDayDto(
    val date: String,
    val status: String,
    val completed: Int,
    val total: Int,
)

@Serializable
data class QuestProgressDto(
    @SerialName("lifetime_xp") val lifetimeXp: Int,
    val hearts: Int,
    val level: Int,
    @SerialName("today_xp") val todayXp: Int,
    @SerialName("today_xp_target") val todayXpTarget: Int,
    @SerialName("today_complete") val todayComplete: Int,
    @SerialName("week_xp") val weekXp: Int,
    @SerialName("week_hearts") val weekHearts: Int,
    @SerialName("week_active") val weekActive: Int,
    val streak: Int,
    @SerialName("best_streak") val bestStreak: Int,
)

@Serializable
data class QuestActivityDto(
    val today: String,
    val days: List<QuestActivityDayDto>? = null,
)

@Serializable
data class QuestActivityDayDto(
    val date: String,
    @SerialName("completed_ids") val completedIds: List<String>? = null,
)
