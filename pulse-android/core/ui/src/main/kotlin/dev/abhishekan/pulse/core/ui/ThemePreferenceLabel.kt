package dev.abhishekan.pulse.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.ThemePreference

val ThemePreference.label: String @Composable get() =
    when (this) {
        ThemePreference.System -> stringResource(R.string.pulse_ui_theme_system)
        ThemePreference.Light -> stringResource(R.string.pulse_ui_theme_light)
        ThemePreference.Dark -> stringResource(R.string.pulse_ui_theme_dark)
    }
