package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = PulseTheme.shapes.large,
        containerColor = PulseTheme.colors.primary,
        contentColor = PulseTheme.colors.onPrimary,
        elevation =
            FloatingActionButtonDefaults.elevation(
                defaultElevation = PulseTheme.elevation.low,
                pressedElevation = PulseTheme.elevation.medium,
                focusedElevation = PulseTheme.elevation.low,
                hoveredElevation = PulseTheme.elevation.medium,
            ),
        content = content,
    )
}

@Composable
fun PulseExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = PulseTheme.shapes.large,
        containerColor = PulseTheme.colors.primary,
        contentColor = PulseTheme.colors.onPrimary,
        elevation =
            FloatingActionButtonDefaults.elevation(
                defaultElevation = PulseTheme.elevation.low,
                pressedElevation = PulseTheme.elevation.medium,
                focusedElevation = PulseTheme.elevation.low,
                hoveredElevation = PulseTheme.elevation.medium,
            ),
    ) {
        icon()
        Spacer(Modifier.width(PulseTokens.small))
        // The text/icon overload clears the text's semantics in Material 3.
        // Keep the visible label accessible and allow long labels to wrap.
        Box(Modifier.weight(1f, fill = false)) { text() }
    }
}
