package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveTheme(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<ThemePreference> = repository.theme
}
