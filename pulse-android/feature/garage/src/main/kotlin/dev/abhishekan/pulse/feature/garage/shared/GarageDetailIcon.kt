package dev.abhishekan.pulse.feature.garage.shared

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Decorative line icons inherit the same theme colors and stroke treatment as the shared UI.
internal enum class GarageIcon { Fuel, Delete }

@Composable
internal fun GarageDetailIcon(
    kind: GarageIcon,
    tint: Color = LocalContentColor.current,
) {
    Icon(garageIcons.getValue(kind), contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
}

private val garageIcons =
    GarageIcon.entries.associateWith { kind ->
        ImageVector
            .Builder(kind.name, 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(
                    stroke =
                        SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    fill = null,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) {
                    when (kind) {
                        GarageIcon.Fuel -> {
                            moveTo(4f, 21f)
                            lineTo(4f, 3f)
                            lineTo(14f, 3f)
                            lineTo(14f, 21f)
                            moveTo(2f, 21f)
                            lineTo(16f, 21f)
                            moveTo(4f, 10f)
                            lineTo(14f, 10f)
                            moveTo(14f, 7f)
                            lineTo(18f, 7f)
                            lineTo(21f, 10f)
                            lineTo(21f, 17f)
                            curveTo(21f, 20f, 17f, 20f, 17f, 17f)
                            lineTo(17f, 13f)
                            moveTo(18f, 4f)
                            lineTo(21f, 7f)
                            lineTo(21f, 10f)
                        }
                        GarageIcon.Delete -> {
                            moveTo(3f, 6f)
                            lineTo(21f, 6f)
                            moveTo(9f, 6f)
                            lineTo(9f, 3f)
                            lineTo(15f, 3f)
                            lineTo(15f, 6f)
                            moveTo(5f, 6f)
                            lineTo(6f, 21f)
                            lineTo(18f, 21f)
                            lineTo(19f, 6f)
                        }
                    }
                }
            }.build()
    }
