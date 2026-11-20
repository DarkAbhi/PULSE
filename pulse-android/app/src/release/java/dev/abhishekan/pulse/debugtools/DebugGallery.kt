package dev.abhishekan.pulse.debugtools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.navigation.NavGraphBuilder
import dev.abhishekan.pulse.appearance.AppearanceUiState
import dev.abhishekan.pulse.core.domain.model.ThemePreference

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DebugGalleryAction(onClick: () -> Unit) { }

@Suppress("UNUSED_PARAMETER")
internal fun NavGraphBuilder.debugGalleryDestination(
    appearance: State<AppearanceUiState>,
    onTheme: (ThemePreference) -> Unit,
    onBack: () -> Unit,
) { }
