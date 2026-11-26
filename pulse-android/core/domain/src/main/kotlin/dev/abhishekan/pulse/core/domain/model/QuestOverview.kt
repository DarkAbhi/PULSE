package dev.abhishekan.pulse.core.domain.model

import java.time.Instant
import java.time.LocalDate

data class QuestSettings(
    val gymWorkout: Boolean = false,
    val stepTarget: Int? = null,
    val activityRings: Boolean = false,
    val sleepMinutes: Int? = null,
    val dailyMeals: Boolean = false,
)

data class QuestSettingsView(
    val configured: Boolean,
    val settings: QuestSettings,
    val effective: LocalDate?,
    val hasWorkoutDays: Boolean,
)

data class QuestOverview(
    val settings: QuestSettingsView,
    val today: LocalDate,
    val nextResetAt: Instant,
    val week: List<QuestDay>,
    val previousWeek: LocalDate?,
    val nextWeek: LocalDate?,
    val todayQuests: List<DailyQuest>,
    val progress: QuestProgress,
)

data class DailyQuest(
    val id: String,
    val target: Int,
    val progress: Int,
    val xp: Int,
    val hearts: Int,
    val completed: Boolean,
)

data class QuestDay(
    val date: LocalDate,
    val status: String,
    val completed: Int,
    val total: Int,
)

data class QuestProgress(
    val lifetimeXp: Int,
    val hearts: Int,
    val level: Int,
    val todayXp: Int,
    val todayXpTarget: Int,
    val todayComplete: Int,
    val weekXp: Int,
    val weekHearts: Int,
    val weekActive: Int,
    val streak: Int,
    val bestStreak: Int,
)

data class QuestActivity(
    val today: LocalDate,
    val days: List<QuestActivityDay>,
)

data class QuestActivityDay(
    val date: LocalDate,
    val completedIds: List<String>,
)
