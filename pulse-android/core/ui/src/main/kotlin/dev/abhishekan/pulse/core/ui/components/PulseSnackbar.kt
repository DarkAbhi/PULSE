package dev.abhishekan.pulse.core.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTheme

@Composable
fun PulseSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
) {
    Snackbar(
        data,
        modifier,
        shape = PulseTheme.shapes.medium,
        containerColor = PulseTheme.colors.inverseSurface,
        contentColor = PulseTheme.colors.inverseText,
        actionColor = PulseTheme.colors.inverseAction,
        dismissActionContentColor = PulseTheme.colors.inverseText,
    )
}
