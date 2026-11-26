package dev.abhishekan.pulse.feature.horizon.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import dev.abhishekan.pulse.core.ui.PulseTheme

/** Broad bezier lobes echo the reference's layered clouds at any aspect ratio. */
@Composable
internal fun Modifier.purchaseClouds(): Modifier {
    val colors = PulseTheme.colors
    return drawBehind {
        val w = size.width
        val h = size.height
        val lowerCloud =
            Path().apply {
                moveTo(w * 0.56f, h)
                cubicTo(w * 0.61f, h * 0.70f, w * 0.69f, h * 0.58f, w * 0.77f, h * 0.59f)
                cubicTo(w * 0.85f, h * 0.60f, w * 0.87f, h * 0.34f, w, h * 0.28f)
                lineTo(w, h)
                close()
            }
        val upperCloud =
            Path().apply {
                moveTo(w * 0.65f, h)
                cubicTo(w * 0.72f, h, w * 0.72f, h * 0.50f, w * 0.77f, h * 0.27f)
                cubicTo(w * 0.82f, h * 0.02f, w * 0.87f, 0f, w * 0.90f, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                close()
            }
        drawPath(lowerCloud, colors.primary.copy(alpha = 0.07f))
        drawPath(upperCloud, Brush.horizontalGradient(listOf(colors.primary.copy(alpha = 0.04f), colors.primary.copy(alpha = 0.13f))))
    }
}
