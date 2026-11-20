package dev.abhishekan.pulse.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalPulseColors = staticCompositionLocalOf { PulseLightColors }
private val LocalPulseDarkTheme = staticCompositionLocalOf { false }

object PulseTheme {
    val isDark: Boolean
        @Composable get() = LocalPulseDarkTheme.current
    val colors: PulseColors
        @Composable get() = LocalPulseColors.current
    val typography = PulseTypography
    val spacing = PulseSpacing
    val shapes = PulseShapes
    val elevation = PulseElevation
}

@Composable
fun PulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) PulseDarkColors else PulseLightColors
    CompositionLocalProvider(LocalPulseColors provides colors, LocalPulseDarkTheme provides darkTheme) {
        PulseMaterialBridge(darkTheme, content)
    }
}
