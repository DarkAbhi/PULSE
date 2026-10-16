package dev.abhishekan.pulse.core.domain

import kotlinx.coroutines.flow.Flow

enum class ThemePreference { System, Light, Dark }

interface PreferencesRepository {
    val theme: Flow<ThemePreference>
    suspend fun setTheme(theme: ThemePreference)
}

class ObserveTheme(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<ThemePreference> = repository.theme
}

class SetTheme(private val repository: PreferencesRepository) {
    suspend operator fun invoke(theme: ThemePreference) = repository.setTheme(theme)
}
