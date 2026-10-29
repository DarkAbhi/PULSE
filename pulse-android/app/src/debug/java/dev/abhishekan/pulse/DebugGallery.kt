package dev.abhishekan.pulse
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.gallery.PulseComponentGallery
import org.koin.androidx.compose.koinViewModel

@Composable
fun DebugGalleryAction(onClick: () -> Unit) {
    PulseButton(stringResource(R.string.pulse_app_component_gallery), onClick, style = PulseButtonStyle.Text)
}

fun NavGraphBuilder.debugGalleryDestination(onBack: () -> Unit) {
    composable("gallery") {
        val viewModel: AppearanceViewModel = koinViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        PulseComponentGallery(state.theme.name, {
            viewModel.selectTheme(ThemePreference.valueOf(it))
        }, state.error, onBack = onBack)
    }
}
