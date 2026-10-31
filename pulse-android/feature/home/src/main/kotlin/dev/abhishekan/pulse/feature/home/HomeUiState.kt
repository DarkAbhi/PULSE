package dev.abhishekan.pulse.feature.home

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.WorkoutToday

sealed interface Section<out T> {
    data object Loading : Section<Nothing>

    data object Empty : Section<Nothing>

    data class Content<T>(
        val value: T,
    ) : Section<T>

    data class Error(
        val failure: Failure,
    ) : Section<Nothing>
}

data class HomeUiState(
    val profile: Section<Profile> = Section.Loading,
    val workout: Section<WorkoutToday> = Section.Loading,
    val horizon: Section<Horizon> = Section.Loading,
    val refreshing: Set<String> = emptySet(),
    val refreshErrors: Map<String, Failure> = emptyMap(),
    val notificationCount: Int = 0,
)
