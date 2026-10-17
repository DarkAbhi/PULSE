package dev.abhishekan.pulse.core.domain.usecase

import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.domain.repository.PreferencesRepository

class SetTheme(
    private val repository: PreferencesRepository,
) {
    suspend operator fun invoke(theme: ThemePreference) = repository.setTheme(theme)
}
