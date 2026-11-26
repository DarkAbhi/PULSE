package dev.abhishekan.pulse.feature.home

import java.time.ZoneId

sealed interface HomeAction {
    data class Retry(
        val section: HomeSection,
    ) : HomeAction

    data class TimeZoneChanged(
        val zone: ZoneId,
    ) : HomeAction

    data object Resumed : HomeAction
}

enum class HomeSection {
    Profile,
    Workout,
    Horizon,
    FitnessToday,
    Quests,
    QuestSettings,
    RecentActivity,
}
