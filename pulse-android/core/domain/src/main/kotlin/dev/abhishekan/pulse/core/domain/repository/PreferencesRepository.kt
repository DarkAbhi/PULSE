package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val theme: Flow<ThemePreference>

    suspend fun setTheme(theme: ThemePreference): Outcome<Unit>
}
