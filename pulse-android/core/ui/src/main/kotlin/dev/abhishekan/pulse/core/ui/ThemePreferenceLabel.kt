package dev.abhishekan.pulse.core.ui

import dev.abhishekan.pulse.core.domain.model.ThemePreference

val ThemePreference.label: UiText get() = when (this) {
    ThemePreference.System -> uiText(R.string.pulse_ui_theme_system)
    ThemePreference.Light -> uiText(R.string.pulse_ui_theme_light)
    ThemePreference.Dark -> uiText(R.string.pulse_ui_theme_dark)
}
