package dev.abhishekan.pulse.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PulseTokens {
    val focusWidth = 2.dp
    val tiny = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val cardPadding = 24.dp
    val large = 32.dp
    val target = 48.dp
    val formColumnsWidth = 480.dp
    val readingWidth = 560.dp
    val contentWidth = 1280.dp
    val cardShape = RoundedCornerShape(24.dp)
    val controlShape = RoundedCornerShape(16.dp)
    val elevation = 0.dp
    const val motionMillis = 180
}

val PulseLightColors = lightColorScheme(
    primary = Color(0xFFC2410C), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4B5), onPrimaryContainer = Color(0xFF482400),
    secondary = Color(0xFF70543C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1E4D5), onSecondaryContainer = Color(0xFF2D2219),
    tertiary = Color(0xFF45634B), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDEBDA), onTertiaryContainer = Color(0xFF213B27),
    background = Color(0xFFFAF6F0), onBackground = Color(0xFF2D2219),
    surface = Color(0xFFFFFDFA), onSurface = Color(0xFF2D2219),
    surfaceVariant = Color(0xFFF1E4D5), onSurfaceVariant = Color(0xFF665447),
    surfaceContainer = Color(0xFFF5EEE5), surfaceContainerLow = Color(0xFFFFFDFA),
    surfaceContainerHigh = Color(0xFFF1E4D5), surfaceContainerHighest = Color(0xFFE9DED1),
    surfaceContainerLowest = Color(0xFFFFFDFA), surfaceBright = Color(0xFFFFFDFA), surfaceDim = Color(0xFFE9DED1),
    outline = Color(0xFF806B5C), outlineVariant = Color(0xFFD5C7B8),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF2D2219), inverseOnSurface = Color(0xFFFAF4EB), inversePrimary = Color(0xFFFDBA74),
)
val PulseDarkColors = darkColorScheme(
    primary = Color(0xFFF59E0B), onPrimary = Color(0xFF2D1800),
    primaryContainer = Color(0xFF573500), onPrimaryContainer = Color(0xFFFFE4B5),
    secondary = Color(0xFFDFC2A5), onSecondary = Color(0xFF38281A),
    secondaryContainer = Color(0xFF493728), onSecondaryContainer = Color(0xFFF1E4D5),
    tertiary = Color(0xFFB7D0B5), onTertiary = Color(0xFF213B27),
    tertiaryContainer = Color(0xFF304A35), onTertiaryContainer = Color(0xFFDDEBDA),
    background = Color(0xFF140F0C), onBackground = Color(0xFFFAF4EB),
    surface = Color(0xFF1E1713), onSurface = Color(0xFFFAF4EB),
    surfaceVariant = Color(0xFF3B2D24), onSurfaceVariant = Color(0xFFD8C5B5),
    surfaceContainer = Color(0xFF271E18), surfaceContainerLow = Color(0xFF1E1713),
    surfaceContainerHigh = Color(0xFF32261F), surfaceContainerHighest = Color(0xFF3B2D24),
    surfaceContainerLowest = Color(0xFF140F0C), surfaceBright = Color(0xFF3B2D24), surfaceDim = Color(0xFF1E1713),
    outline = Color(0xFFAD9582), outlineVariant = Color(0xFF5C493B),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF6E211C), onErrorContainer = Color(0xFFF9DEDC),
    inverseSurface = Color(0xFFFAF4EB), inverseOnSurface = Color(0xFF2D2219), inversePrimary = Color(0xFFC2410C),
)

private val body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
private val label = body.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
private val title = body.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
private val display = body.copy(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold)
val PulseTypography = Typography(
    displayLarge = display, displayMedium = display, displaySmall = display,
    headlineLarge = display, headlineMedium = title, headlineSmall = title,
    titleLarge = title, titleMedium = body.copy(fontWeight = FontWeight.SemiBold), titleSmall = label,
    bodyLarge = body, bodyMedium = body, bodySmall = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = label, labelMedium = label, labelSmall = label,
)

@Composable
fun PulseTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) PulseDarkColors else PulseLightColors,
        typography = PulseTypography,
        shapes = Shapes(small = PulseTokens.controlShape, medium = PulseTokens.controlShape, large = PulseTokens.cardShape),
        content = content,
    )
}
