package dev.abhishekan.pulse.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Material supplies interaction mechanics; every visual role comes from Pulse tokens.
internal fun PulseColors.materialColors(dark: Boolean) =
    ColorScheme(
        primary = action,
        onPrimary = if (dark) onPrimary else surface,
        primaryContainer = primarySoft,
        onPrimaryContainer = onPrimarySoft,
        inversePrimary = inverseAction,
        secondary = textSecondary,
        onSecondary = background,
        secondaryContainer = surfaceMuted,
        onSecondaryContainer = textPrimary,
        tertiary = success,
        onTertiary = if (dark) background else surface,
        tertiaryContainer = successSoft,
        onTertiaryContainer = success,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = textSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseText,
        error = error,
        onError = if (dark) background else surface,
        errorContainer = errorSoft,
        onErrorContainer = error,
        outline = outline,
        outlineVariant = border,
        scrim = Color.Black,
        surfaceBright = surface,
        surfaceDim = surfaceStrong,
        surfaceContainer = surface,
        surfaceContainerLow = surface,
        surfaceContainerLowest = background,
        surfaceContainerHigh = surfaceMuted,
        surfaceContainerHighest = surfaceStrong,
        primaryFixed = primary,
        primaryFixedDim = primaryPressed,
        onPrimaryFixed = onPrimary,
        onPrimaryFixedVariant = onPrimary,
        secondaryFixed = surfaceMuted,
        secondaryFixedDim = surfaceStrong,
        onSecondaryFixed = textPrimary,
        onSecondaryFixedVariant = textSecondary,
        tertiaryFixed = successSoft,
        tertiaryFixedDim = successSoft,
        onTertiaryFixed = success,
        onTertiaryFixedVariant = success,
    )

private val lightCompatibilityColors = PulseLightColors.materialColors(false)
private val darkCompatibilityColors = PulseDarkColors.materialColors(true)
private val compatibilityTypography =
    with(PulseTypography) {
        Typography(
            displayLarge = displayXL,
            displayMedium = displayLarge,
            displaySmall = displayMedium,
            headlineLarge = heading1,
            headlineMedium = heading2,
            headlineSmall = heading3,
            titleLarge = title1,
            titleMedium = title3,
            titleSmall = label1,
            bodyLarge = body1,
            bodyMedium = body2,
            bodySmall = body3,
            labelLarge = label1,
            labelMedium = label2,
            labelSmall = caption,
        )
    }
private val compatibilityShapes =
    Shapes(
        extraSmall = PulseShapes.extraSmall,
        small = PulseShapes.small,
        medium = PulseShapes.medium,
        large = PulseShapes.large,
        extraLarge = PulseShapes.extraLarge,
    )

@Composable
internal fun PulseMaterialBridge(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides PulseTheme.colors.textPrimary) {
        MaterialTheme(
            colorScheme = if (dark) darkCompatibilityColors else lightCompatibilityColors,
            typography = compatibilityTypography,
            shapes = compatibilityShapes,
            content = content,
        )
    }
}
