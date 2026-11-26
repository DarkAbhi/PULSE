package dev.abhishekan.pulse.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalPulseColors = staticCompositionLocalOf { PulseLightColors }
private val LocalPulseFeatureTones = staticCompositionLocalOf { PulseLightFeatureTones }
private val LocalPulseDarkTheme = staticCompositionLocalOf { false }

object PulseTheme {
    val isDark: Boolean
        @Composable get() = LocalPulseDarkTheme.current
    val colors: PulseColors
        @Composable get() = LocalPulseColors.current
    val featureTones: PulseFeatureTones
        @Composable get() = LocalPulseFeatureTones.current
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
    val tones = if (darkTheme) PulseDarkFeatureTones else PulseLightFeatureTones
    CompositionLocalProvider(
        LocalPulseColors provides colors,
        LocalPulseFeatureTones provides tones,
        LocalPulseDarkTheme provides darkTheme,
    ) {
        PulseMaterialBridge(darkTheme, content)
    }
}
