package dev.abhishekan.pulse.feature.fitness.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant

@Composable
internal fun WorkoutActionButton(
    text: String,
    symbol: WorkoutSymbol,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    destructive: Boolean = false,
    soft: Boolean = false,
) {
    PulseButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant =
            when {
                destructive -> PulseButtonVariant.DestructiveSoft
                primary -> PulseButtonVariant.Primary
                soft -> PulseButtonVariant.Soft
                else -> PulseButtonVariant.Secondary
            },
        leadingIcon = {
            WorkoutGlyph(symbol, LocalContentColor.current)
        },
    )
}

internal enum class WorkoutSymbol { Add, Play, Stop, Clock, Energy, Info, Weight, Muscle, List, Edit, Delete, Refresh, Document }

@Composable
internal fun WorkoutGlyph(
    symbol: WorkoutSymbol,
    color: Color,
    modifier: Modifier = Modifier.size(24.dp),
) {
    Canvas(modifier) {
        val u = size.width / 24f
        val stroke = 1.8f * u

        fun line(
            x: Float,
            y: Float,
            xx: Float,
            yy: Float,
        ) = drawLine(color, Offset(x * u, y * u), Offset(xx * u, yy * u), stroke)

        fun rect(
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            fill: Boolean = false,
        ) = drawRoundRect(
            color,
            Offset(x * u, y * u),
            Size(
                w * u,
                h * u,
            ),
            CornerRadius(1.5f * u),
            style = if (fill) Fill else Stroke(stroke),
        )
        when (symbol) {
            WorkoutSymbol.Add -> {
                line(12f, 3f, 12f, 21f)
                line(3f, 12f, 21f, 12f)
            }
            WorkoutSymbol.Stop -> rect(5f, 5f, 14f, 14f, true)
            WorkoutSymbol.Play ->
                drawPath(
                    Path().apply {
                        moveTo(7 * u, 4 * u)
                        lineTo(20 * u, 12 * u)
                        lineTo(7 * u, 20 * u)
                        close()
                    },
                    color,
                )
            WorkoutSymbol.Clock, WorkoutSymbol.Info -> {
                drawCircle(color, 9 * u, style = Stroke(stroke))
                if (symbol ==
                    WorkoutSymbol.Clock
                ) {
                    line(12f, 6f, 12f, 12f)
                    line(12f, 12f, 17f, 15f)
                } else {
                    line(12f, 10f, 12f, 17f)
                    drawCircle(
                        color,
                        u,
                        Offset(
                            12 * u,
                            7 * u,
                        ),
                    )
                }
            }
            WorkoutSymbol.Weight -> {
                line(2f, 12f, 22f, 12f)
                rect(4f, 6f, 4f, 12f, true)
                rect(16f, 6f, 4f, 12f, true)
            }
            WorkoutSymbol.List -> for (y in listOf(6f, 12f, 18f)) {
                drawCircle(color, 1.5f * u, Offset(4 * u, y * u))
                line(9f, y, 21f, y)
            }
            WorkoutSymbol.Muscle -> {
                line(8f, 3f, 6f, 12f)
                line(6f, 12f, 8f, 21f)
                line(16f, 3f, 18f, 12f)
                line(18f, 12f, 16f, 21f)
                line(12f, 4f, 12f, 19f)
            }
            WorkoutSymbol.Delete -> {
                rect(6f, 7f, 12f, 14f)
                line(3f, 5f, 21f, 5f)
                line(9f, 2f, 15f, 2f)
                line(10f, 10f, 10f, 18f)
                line(14f, 10f, 14f, 18f)
            }
            WorkoutSymbol.Document -> {
                rect(5f, 2f, 14f, 20f)
                for (y in listOf(8f, 12f, 16f)) line(8f, y, 16f, y)
            }
            WorkoutSymbol.Refresh -> {
                drawArc(color, 35f, 285f, false, Offset(3 * u, 3 * u), Size(18 * u, 18 * u), style = Stroke(stroke))
                line(20f, 3f, 20f, 9f)
                line(14f, 9f, 20f, 9f)
            }
            WorkoutSymbol.Edit -> {
                line(4f, 17f, 17f, 4f)
                line(7f, 20f, 20f, 7f)
                line(17f, 4f, 20f, 7f)
                line(4f, 17f, 3f, 21f)
                line(3f, 21f, 7f, 20f)
            }
            WorkoutSymbol.Energy ->
                drawPath(
                    Path().apply {
                        moveTo(14 * u, 2 * u)
                        lineTo(5 * u, 14 * u)
                        lineTo(11 * u, 14 * u)
                        lineTo(10 * u, 22 * u)
                        lineTo(
                            20 * u,
                            9 * u,
                        )
                        lineTo(14 * u, 9 * u)
                        close()
                    },
                    color,
                )
        }
    }
}
