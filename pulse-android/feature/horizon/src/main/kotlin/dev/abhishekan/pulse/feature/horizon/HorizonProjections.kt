package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonProjection
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText

@Composable
internal fun ProjectionCard(data: HorizonOverview) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PulseTheme.shapes.large,
        color = PulseTheme.colors.surface,
        contentColor = PulseTheme.colors.textPrimary,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProjectionHeadingIcon()
                PulseText(
                    stringResource(R.string.pulse_horizon_future_projections),
                    modifier = Modifier.weight(1f),
                    style = PulseTextStyle.Title3,
                )
            }
            if (data.projections.isEmpty()) {
                PulseEmptyState(
                    stringResource(R.string.pulse_horizon_no_projections),
                    stringResource(R.string.pulse_horizon_projections_appear_when_the_server_provides_them),
                )
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    // Keep every period visible on phones and allow extra room for enlarged text.
                    val tileWidth = 240.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                    val fitsRow = maxWidth >= tileWidth * data.projections.size + 12.dp * (data.projections.size - 1)
                    if (fitsRow) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            data.projections.forEachIndexed { index, projection ->
                                ProjectionTile(projection, index, Modifier.weight(1f))
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            data.projections.forEachIndexed { index, projection ->
                                ProjectionTile(projection, index, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectionTile(
    projection: HorizonProjection,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val paletteIndex =
        when (projection.months) {
            3 -> 0
            6 -> 1
            12 -> 2
            else -> index % 3
        }
    val colors = PulseTheme.colors
    val accent = listOf(colors.action, colors.info, colors.success)[paletteIndex]
    val background = listOf(colors.primarySoft, colors.infoSoft, colors.successSoft)[paletteIndex]
    Column(
        modifier
            .background(background, PulseTheme.shapes.large)
            .semantics(mergeDescendants = true) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PulseText(projection.label, Modifier.weight(1f), PulseTextStyle.Label, accent)
            ProjectionBars(accent)
        }
        PulseText(
            currencyText(projection.amount).asString(),
            color = PulseTheme.colors.textPrimary,
            style = PulseTextStyle.Heading3,
        )
        PulseText(
            pluralStringResource(R.plurals.pulse_horizon_projection_reserve, projection.months, projection.months),
            style = PulseTextStyle.Body3,
            color = PulseTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun ProjectionHeadingIcon() {
    val ink = PulseTheme.colors.onPrimarySoft
    Box(
        Modifier.size(40.dp).background(PulseTheme.colors.primarySoft, PulseTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val stroke = 2.dp.toPx()
            drawLine(
                ink,
                Offset(size.width * .25f, size.height * .75f),
                Offset(size.width * .75f, size.height * .25f),
                stroke,
                StrokeCap.Round,
            )
            drawLine(
                ink,
                Offset(size.width * .35f, size.height * .25f),
                Offset(size.width * .75f, size.height * .25f),
                stroke,
                StrokeCap.Round,
            )
            drawLine(
                ink,
                Offset(size.width * .75f, size.height * .25f),
                Offset(size.width * .75f, size.height * .65f),
                stroke,
                StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ProjectionBars(accent: Color) {
    Canvas(Modifier.size(32.dp)) {
        repeat(3) { index ->
            val height = size.height * (.35f + index * .22f)
            drawRoundRect(
                color = accent.copy(alpha = .35f + index * .15f),
                topLeft = Offset(size.width * index / 3, size.height - height),
                size = Size(size.width * .23f, height),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}
