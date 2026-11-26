package dev.abhishekan.pulse.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colour roles shared with the web design system.
 *
 * Values mirror `life-tracker-frontend/app/globals.css` and `DESIGN.md`; role names
 * match the web's CSS variables (for example `surfaceStrong` is `--surface-strong`).
 * Translucent web roles `--divider` and `--chip` keep their alpha; `--accent` (primarySoft)
 * is pre-blended over the card surface so contrast checks stay exact.
 */
@Immutable
data class PulseColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val primaryPressed: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val onPrimarySoft: Color,
    val action: Color,
    val border: Color,
    val outline: Color,
    val divider: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val error: Color,
    val errorSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val coral: Color,
    val sage: Color,
    val sky: Color,
    val lavender: Color,
    val inverseSurface: Color,
    val inverseText: Color,
    val inverseAction: Color,
    val brand: Color,
    val ring: Color,
    val chip: Color,
    val horizonSurface: Color,
    val onHorizonSurface: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

/** Fixed brand colours, identical in light and dark (web `--brand-*`). */
object PulseBrandColors {
    val blue = Color(0xFF4C6BFF)
    val cyan = Color(0xFF19D3F3)
    val pink = Color(0xFFFF2FD0)
    val gold = Color(0xFFFFC83D)
    val onGold = Color(0xFF3A2A00)
    val violet = Color(0xFFB22BD8)
    val indigo = Color(0xFF2B1B8F)
    val purple = Color(0xFF5B2CE0)
    val purpleBright = Color(0xFF7B2CE0)
    val orange = Color(0xFFF0603A)
    val magenta = Color(0xFFD91CAB)
}

/** A two-stop feature gradient with its foreground (web quick-action tile tones). */
@Immutable
data class PulseTone(
    val start: Color,
    val end: Color,
    val foreground: Color = Color.White,
)

/** Feature tile tones from `features/dashboard/styles/dashboard-theme.css`. */
@Immutable
data class PulseFeatureTones(
    val fitness: PulseTone,
    val garage: PulseTone,
    val meal: PulseTone,
    val finance: PulseTone,
    val questLevels: List<Color>,
)

val PulseLightFeatureTones =
    PulseFeatureTones(
        fitness = PulseTone(PulseBrandColors.orange, Color(0xFFD8336A)),
        garage = PulseTone(Color(0xFF2440E0), Color(0xFF0F75B3)),
        meal = PulseTone(Color(0xFFC91A8E), Color(0xFFCF3C36)),
        finance = PulseTone(Color(0xFFFFE08A), Color(0xFFFFE08A), PulseBrandColors.onGold),
        questLevels = listOf(Color(0xFF6FDDF2), PulseBrandColors.blue, Color(0xFF8B3FE0), Color(0xFFD41FB0)),
    )

val PulseDarkFeatureTones =
    PulseFeatureTones(
        fitness = PulseTone(PulseBrandColors.orange, Color(0xFFC42D60)),
        garage = PulseTone(Color(0xFF2440E0), Color(0xFF0F75B3)),
        meal = PulseTone(Color(0xFFC91A8E), Color(0xFFCF3C36)),
        finance = PulseTone(PulseBrandColors.gold, PulseBrandColors.gold, PulseBrandColors.onGold),
        questLevels = listOf(Color(0xFF4FC3DA), Color(0xFF5F7BFF), Color(0xFF9C55F0), Color(0xFFE640C4)),
    )

val PulseLightColors =
    PulseColors(
        background = Color(0xFFF6F7FF),
        surface = Color(0xFFFFFFFF),
        surfaceMuted = Color(0xFFF1F2FB),
        surfaceStrong = Color(0xFFE6E5F5),
        textPrimary = Color(0xFF1A1740),
        textSecondary = Color(0xFF5E5A7D),
        textTertiary = Color(0xFF5E5A7D),
        primary = Color(0xFF415FE8),
        primaryPressed = Color(0xFF3248BB),
        onPrimary = Color(0xFFFFFFFF),
        primarySoft = Color(0xFFE4E9FF),
        onPrimarySoft = Color(0xFF342B76),
        action = Color(0xFF4440AD),
        border = Color(0x1F1A1740),
        outline = Color(0xFF8A86A8),
        divider = Color(0x1F1A1740),
        success = Color(0xFF087658),
        successSoft = Color(0xFFE1F5EC),
        warning = Color(0xFF925900),
        warningSoft = Color(0xFFFFF2CD),
        error = Color(0xFFBF2D24),
        errorSoft = Color(0xFFFCEAE7),
        info = Color(0xFF3856A5),
        infoSoft = Color(0xFFE8EEFF),
        coral = Color(0xFFD96A4F),
        sage = Color(0xFF7E9B65),
        sky = Color(0xFF79B9D1),
        lavender = Color(0xFFA48DE0),
        inverseSurface = Color(0xFF1A1740),
        inverseText = Color(0xFFF4F1FF),
        inverseAction = Color(0xFFC1B4FF),
        brand = PulseBrandColors.blue,
        ring = Color(0xFF4B4BC5),
        chip = Color(0x121A1740),
        horizonSurface = Color(0xFFFFE08A),
        onHorizonSurface = PulseBrandColors.onGold,
        gradientStart = Color(0xFF415FE8),
        gradientEnd = PulseBrandColors.violet,
    )

val PulseDarkColors =
    PulseColors(
        background = Color(0xFF0D0A1F),
        surface = Color(0xFF1A1636),
        surfaceMuted = Color(0xFF231E45),
        surfaceStrong = Color(0xFF342C5D),
        textPrimary = Color(0xFFF4F1FF),
        textSecondary = Color(0xFFA49EC4),
        textTertiary = Color(0xFFA49EC4),
        primary = Color(0xFFF4F1FF),
        primaryPressed = Color(0xFFDCD5FF),
        onPrimary = Color(0xFF0D0A1F),
        primarySoft = Color(0xFF222354),
        onPrimarySoft = Color(0xFFE5DCFF),
        action = Color(0xFFC1B4FF),
        border = Color(0x24F4F1FF),
        outline = Color(0xFF7D779F),
        divider = Color(0x24F4F1FF),
        success = Color(0xFF3DDCA6),
        successSoft = Color(0xFF193C39),
        warning = PulseBrandColors.gold,
        warningSoft = Color(0xFF3C3129),
        error = Color(0xFFFF7A6B),
        errorSoft = Color(0xFF492638),
        info = Color(0xFFADC3FF),
        infoSoft = Color(0xFF263054),
        coral = Color(0xFFE88B73),
        sage = Color(0xFF9BB681),
        sky = Color(0xFF95CDE1),
        lavender = Color(0xFFBD9DE5),
        inverseSurface = Color(0xFFF4F1FF),
        inverseText = Color(0xFF1A1740),
        inverseAction = Color(0xFF4440AD),
        brand = Color(0xFF8FA2FF),
        ring = Color(0xFFB6A6FF),
        chip = Color(0x1AF4F1FF),
        horizonSurface = PulseBrandColors.gold,
        onHorizonSurface = PulseBrandColors.onGold,
        gradientStart = Color(0xFFF4F1FF),
        gradientEnd = Color(0xFFF4F1FF),
    )
