package dev.abhishekan.pulse.debugtools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.abhishekan.pulse.R
import dev.abhishekan.pulse.appearance.AppearanceUiState
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.gallery.PulseComponentGallery
import dev.abhishekan.pulse.navigation.AppDestination

@Composable
internal fun DebugGalleryAction(onClick: () -> Unit) {
    PulseButton(stringResource(R.string.pulse_app_component_gallery), onClick, style = PulseButtonStyle.Text)
}

internal fun NavGraphBuilder.debugGalleryDestination(
    appearance: State<AppearanceUiState>,
    onTheme: (ThemePreference) -> Unit,
    onBack: () -> Unit,
) {
    composable(AppDestination.Gallery.route) {
        val state = appearance.value
        PulseComponentGallery(state.theme.name, {
            onTheme(ThemePreference.valueOf(it))
        }, state.error, onBack = onBack)
    }
}
