package dev.abhishekan.pulse.feature.home

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FitnessToday
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.QuestActivity
import dev.abhishekan.pulse.core.domain.model.QuestSettingsView
import dev.abhishekan.pulse.core.domain.model.RecentActivityEntry
import dev.abhishekan.pulse.core.domain.model.WorkoutToday

sealed interface Section<out T> {
    sealed interface Displayed<out T> : Section<T>

    sealed interface Available<out T> : Displayed<T>

    data object Loading : Displayed<Nothing>

    data object Empty : Available<Nothing>

    data class Content<T>(
        val value: T,
    ) : Available<T>

    data class Error(
        val failure: Failure,
    ) : Displayed<Nothing>

    data class Refreshing<T>(
        val previous: Available<T>,
    ) : Section<T>

    data class RefreshFailed<T>(
        val previous: Available<T>,
        val failure: Failure,
    ) : Section<T>

    val displayed: Displayed<T>
        get() =
            when (this) {
                is Displayed -> this
                is Refreshing -> previous
                is RefreshFailed -> previous
            }

    val refreshFailure: Failure?
        get() = (this as? RefreshFailed)?.failure
}

data class HomeUiState(
    val profile: Section<Profile> = Section.Loading,
    val workout: Section<WorkoutToday> = Section.Loading,
    val horizon: Section<Horizon> = Section.Loading,
    val fitnessToday: Section<FitnessToday> = Section.Loading,
    val quests: Section<QuestActivity> = Section.Loading,
    val questSettings: Section<QuestSettingsView> = Section.Loading,
    val recentActivity: Section<List<RecentActivityEntry>> = Section.Loading,
    val notificationCount: Int = 0,
) {
    val isInitialLoading: Boolean
        get() = profile == Section.Loading && workout == Section.Loading && horizon == Section.Loading

    val profileName: String?
        get() = (profile.displayed as? Section.Content)?.value?.name

    val needsProfile: Boolean
        get() = (profile.displayed as? Section.Content)?.value?.hasProfile == false
}
