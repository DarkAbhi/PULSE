package dev.abhishekan.pulse.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
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
            listOf(c.onPrimary to c.primary, c.onPrimaryContainer to c.primaryContainer,
                c.onSecondary to c.secondary, c.onSecondaryContainer to c.secondaryContainer,
                c.onTertiary to c.tertiary, c.onTertiaryContainer to c.tertiaryContainer,
                c.onBackground to c.background, c.onSurface to c.surface,
                c.onSurfaceVariant to c.surfaceVariant, c.onSurfaceVariant to c.surface, c.onSurfaceVariant to c.background,
                c.primary to c.surface, c.primary to c.background, c.error to c.surface,
                c.onError to c.error, c.onErrorContainer to c.errorContainer,
                c.inverseOnSurface to c.inverseSurface, c.inversePrimary to c.inverseSurface,
                c.onSurface to c.surfaceContainer, c.onSurface to c.surfaceContainerHighest,
            ).forEach { (foreground, background) ->
                assertTrue("Contrast below 4.5: $foreground on $background", contrast(foreground, background) >= 4.5f)
            }
            assertTrue("Calendar activity indicators must meet 3:1", contrast(c.tertiary, c.surface) >= 3f)
            assertTrue("Field outlines must meet 3:1", contrast(c.outline, c.surface) >= 3f)
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance(); val y = b.luminance()
        return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
    }
}
