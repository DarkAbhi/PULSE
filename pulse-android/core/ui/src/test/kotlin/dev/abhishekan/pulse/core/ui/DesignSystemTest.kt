package dev.abhishekan.pulse.core.ui
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTest {
    @Test fun layoutRespondsToWindowWidthBoundaries() {
        assertEquals(PulseWindowSize.Compact, pulseWindowSize(599.dp))
        assertEquals(PulseWindowSize.Medium, pulseWindowSize(600.dp))
        assertEquals(PulseWindowSize.Medium, pulseWindowSize(839.dp))
        assertEquals(PulseWindowSize.Expanded, pulseWindowSize(840.dp))
    }

    @Test fun textColorPairsMeetWcagAA() {
        listOf(PulseLightColors, PulseDarkColors).forEach { c ->
            listOf(
                c.onPrimary to c.primary,
                c.onPrimary to c.primaryPressed,
                c.onPrimarySoft to c.primarySoft,
                c.textPrimary to c.background,
                c.textPrimary to c.surface,
                c.textPrimary to c.surfaceMuted,
                c.textPrimary to c.surfaceStrong,
                c.textSecondary to c.surface,
                c.textSecondary to c.surfaceMuted,
                c.textSecondary to c.surfaceStrong,
                c.textTertiary to c.surface,
                c.textTertiary to c.surfaceStrong,
                c.action to c.background,
                c.action to c.surface,
                c.action to c.primarySoft,
                c.success to c.surface,
                c.success to c.successSoft,
                c.warning to c.surface,
                c.warning to c.warningSoft,
                c.error to c.surface,
                c.error to c.background,
                c.error to c.errorSoft,
                c.info to c.surface,
                c.info to c.infoSoft,
                c.inverseText to c.inverseSurface,
                c.inverseAction to c.inverseSurface,
            ).forEach { (foreground, background) ->
                assertTrue(
                    "Contrast below 4.5: $foreground on $background",
                    contrast(foreground, background) >= 4.5f,
                )
            }
            assertTrue(
                "Calendar activity indicators must meet 3:1",
                contrast(c.success, c.surface) >= 3f,
            )
            assertTrue("Field outlines must meet 3:1", contrast(c.outline, c.surface) >= 3f)
        }
    }

    @Test fun retainedMaterialColorPairsAlsoMeetWcagAA() {
        listOf(PulseLightColors to false, PulseDarkColors to true).forEach { (colors, dark) ->
            val c = colors.materialColors(dark)
            listOf(
                c.onPrimary to c.primary,
                c.onPrimaryFixed to c.primaryFixed,
                c.onPrimaryFixed to c.primaryFixedDim,
                c.onPrimaryFixedVariant to c.primaryFixed,
                c.onPrimaryFixedVariant to c.primaryFixedDim,
                c.onSecondary to c.secondary,
                c.onSecondaryFixed to c.secondaryFixed,
                c.onSecondaryFixedVariant to c.secondaryFixedDim,
                c.onTertiary to c.tertiary,
                c.onTertiaryFixed to c.tertiaryFixed,
                c.onTertiaryFixedVariant to c.tertiaryFixedDim,
                c.onError to c.error,
            ).forEach { (foreground, background) ->
                assertTrue("Compatibility contrast below 4.5: $foreground on $background", contrast(foreground, background) >= 4.5f)
            }
        }
    }

    @Test fun semanticTypeScaleAndTouchTargetsStayDistinct() {
        with(PulseTypography) {
            assertTrue(displayXL.fontSize > displayLarge.fontSize)
            assertTrue(displayLarge.fontSize > displayMedium.fontSize)
            assertTrue(heading1.fontSize > heading2.fontSize)
            assertTrue(heading2.fontSize > heading3.fontSize)
            assertTrue(body1.fontSize > body2.fontSize)
            assertTrue(body2.fontSize > body3.fontSize)
            listOf(
                displayXL,
                displayLarge,
                displayMedium,
                heading1,
                heading2,
                heading3,
                title1,
                title2,
                title3,
                body1,
                body2,
                body3,
                label1,
                label2,
                caption,
                overline,
                editorial,
            ).forEach {
                assertTrue(it.lineHeight >= it.fontSize)
                assertTrue(it.fontFamily != null)
                assertTrue(it.fontWeight != null)
                assertTrue(it.letterSpacing.isSp)
            }
        }
        assertTrue(PulseTokens.target >= 48.dp)
        assertEquals(PulseShapes.large, PulseTokens.cardShape)
        assertEquals(PulseElevation.none, PulseTokens.elevation)
    }

    private fun contrast(
        a: Color,
        b: Color,
    ): Float {
        val x = a.luminance()
        val y = b.luminance()
        return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
    }
}
