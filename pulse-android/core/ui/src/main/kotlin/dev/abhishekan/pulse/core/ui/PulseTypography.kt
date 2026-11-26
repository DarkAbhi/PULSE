package dev.abhishekan.pulse.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * Bricolage Grotesque everywhere, as on the web (DESIGN.md "Typography").
 * The bundled file is the variable font (opsz, wdth, wght). Browsers pick the optical
 * size from the font size automatically; Android does not, so text and display
 * families pin a representative optical size.
 */
private fun bricolage(
    weight: FontWeight,
    opticalSize: Float,
) = Font(
    R.font.bricolage_grotesque,
    weight,
    variationSettings =
        FontVariation.Settings(
            FontVariation.weight(weight.weight),
            FontVariation.Setting("opsz", opticalSize),
        ),
)

private val textWeights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

private val interfaceFont = FontFamily(textWeights.map { bricolage(it, opticalSize = 14f) })
private val displayFont = FontFamily(textWeights.map { bricolage(it, opticalSize = 48f) })

private val headingTracking = (-0.025).em
private val displayTracking = (-0.035).em

@Immutable
class PulseTypeScale {
    val displayXL =
        TextStyle(
            fontFamily = displayFont,
            fontSize = 64.sp,
            lineHeight = 68.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = displayTracking,
            fontFeatureSettings = "tnum",
        )
    val displayLarge =
        TextStyle(
            fontFamily = displayFont,
            fontSize = 48.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = displayTracking,
            fontFeatureSettings = "tnum",
        )
    val displayMedium =
        TextStyle(
            fontFamily = displayFont,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = displayTracking,
            fontFeatureSettings = "tnum",
        )

    // headline-lg / headline-md / headline-sm
    val heading1 =
        TextStyle(
            fontFamily = displayFont,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = headingTracking,
        )
    val heading2 =
        TextStyle(
            fontFamily = displayFont,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = headingTracking,
        )
    val heading3 =
        TextStyle(
            fontFamily = interfaceFont,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = headingTracking,
        )

    // title-lg is title2; title1 and title3 sit either side of it.
    val title1 =
        TextStyle(fontFamily = interfaceFont, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val title2 =
        TextStyle(fontFamily = interfaceFont, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val title3 =
        TextStyle(fontFamily = interfaceFont, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)

    // body-md / body-sm / caption-sized body
    val body1 =
        TextStyle(fontFamily = interfaceFont, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val body2 =
        TextStyle(fontFamily = interfaceFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val body3 =
        TextStyle(fontFamily = interfaceFont, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

    // label-md / label-sm
    val label1 =
        TextStyle(fontFamily = interfaceFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val label2 =
        TextStyle(fontFamily = interfaceFont, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
    val caption =
        TextStyle(fontFamily = interfaceFont, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

    // label-caps
    val overline =
        TextStyle(
            fontFamily = interfaceFont,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.04.em,
        )

    // The web has no serif; editorial headlines use the display face.
    val editorial =
        TextStyle(
            fontFamily = displayFont,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = headingTracking,
        )
}

val PulseTypography = PulseTypeScale()
