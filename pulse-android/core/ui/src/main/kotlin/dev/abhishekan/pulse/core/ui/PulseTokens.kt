package dev.abhishekan.pulse.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object PulseSpacing {
    val none = 0.dp
    val tiny = 4.dp
    val small = 8.dp
    val compact = 12.dp
    val medium = 16.dp
    val comfortable = 20.dp
    val cardPadding = 24.dp
    val large = 32.dp
    val extraLarge = 40.dp
    val target = 48.dp
    val spacious = 64.dp
}

object PulseShapes {
    val none = RoundedCornerShape(0.dp)
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val extraLarge = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(50)
}

object PulseElevation {
    val none = 0.dp
    val low = 2.dp
    val medium = 4.dp
    val high = 8.dp
}

// Keep existing layout dimensions and callers stable during the theme migration.
object PulseTokens {
    val focusWidth = 2.dp
    val tiny = PulseSpacing.tiny
    val small = PulseSpacing.small
    val medium = PulseSpacing.medium
    val cardPadding = PulseSpacing.cardPadding
    val large = PulseSpacing.large
    val target = PulseSpacing.target
    val formColumnsWidth = 480.dp
    val minimumNavigationHeight = 240.dp
    val readingWidth = 560.dp
    val contentWidth = 1280.dp
    val cardShape = PulseShapes.large
    val controlShape = PulseShapes.small
    val elevation = PulseElevation.none
    const val MOTION_MILLIS = 180
}
