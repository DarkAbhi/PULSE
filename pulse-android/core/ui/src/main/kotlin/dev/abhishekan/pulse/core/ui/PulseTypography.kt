package dev.abhishekan.pulse.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val interfaceFont =
    FontFamily(
        Font(R.font.dm_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.dm_sans, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        Font(R.font.dm_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(R.font.dm_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    )
private val displayFont = FontFamily(Font(R.font.barlow_condensed_bold, FontWeight.Bold))
private val editorialFont = FontFamily(Font(R.font.instrument_serif_regular))

@Immutable
class PulseTypeScale {
    val displayXL =
        TextStyle(
            fontFamily = displayFont,
            fontSize = 64.sp,
            lineHeight = 68.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            fontFeatureSettings = "tnum",
        )
    val displayLarge =
        TextStyle(
            fontFamily = displayFont,
            fontSize = 48.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            fontFeatureSettings = "tnum",
        )
    val displayMedium =
        TextStyle(
            fontFamily = displayFont,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            fontFeatureSettings = "tnum",
        )
    val heading1 =
        TextStyle(fontFamily = interfaceFont, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val heading2 =
        TextStyle(fontFamily = interfaceFont, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val heading3 =
        TextStyle(fontFamily = interfaceFont, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val title1 =
        TextStyle(fontFamily = interfaceFont, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val title2 =
        TextStyle(fontFamily = interfaceFont, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
    val title3 =
        TextStyle(fontFamily = interfaceFont, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
    val body1 =
        TextStyle(fontFamily = interfaceFont, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val body2 =
        TextStyle(fontFamily = interfaceFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val body3 =
        TextStyle(fontFamily = interfaceFont, fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val label1 =
        TextStyle(fontFamily = interfaceFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val label2 =
        TextStyle(fontFamily = interfaceFont, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val caption =
        TextStyle(fontFamily = interfaceFont, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val overline =
        TextStyle(fontFamily = interfaceFont, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    val editorial =
        TextStyle(fontFamily = editorialFont, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
}

val PulseTypography = PulseTypeScale()
