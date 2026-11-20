package dev.abhishekan.pulse.appearance

import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.UiText

internal data class AppearanceUiState(
    val theme: ThemePreference = ThemePreference.System,
    val error: UiText? = null,
)
