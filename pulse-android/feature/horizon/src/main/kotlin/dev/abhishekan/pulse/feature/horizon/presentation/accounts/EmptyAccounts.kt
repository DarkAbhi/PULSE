package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.horizon.R

@Composable
internal fun EmptyAccounts(
    type: AccountType,
    enabled: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = type.accentColor
    val soft = type.softColor
    Column(
        modifier.fillMaxWidth().padding(vertical = PulseSpacing.cardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(PulseSpacing.spacious * 2, PulseSpacing.extraLarge * 2 + PulseSpacing.small),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .rotate(-12f)
                    .size(PulseSpacing.spacious)
                    .background(soft.copy(alpha = 0.6f), PulseTheme.shapes.extraLarge),
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .rotate(12f)
                    .size(PulseSpacing.spacious)
                    .background(soft.copy(alpha = 0.6f), PulseTheme.shapes.extraLarge),
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .size(PulseSpacing.extraLarge * 2)
                    .background(soft, PulseTheme.shapes.extraLarge),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val stroke = 1.dp.toPx()
                    val inset = stroke / 2
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(PulseSpacing.comfortable.toPx()),
                        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                    )
                }
                Icon(
                    painterResource(type.icon),
                    contentDescription = null,
                    modifier = Modifier.size(PulseSpacing.cardPadding),
                    tint = accent,
                )
            }
        }
        PulseText(
            stringResource(type.emptyLabel),
            Modifier.padding(top = PulseSpacing.extraLarge).semantics { heading() },
            style = PulseTextStyle.Heading3,
            textAlign = TextAlign.Center,
        )
        PulseText(
            stringResource(type.emptyMessage),
            Modifier.widthIn(max = PulseTokens.readingWidth / 2).padding(top = PulseSpacing.small, bottom = PulseSpacing.cardPadding),
            style = PulseTextStyle.Body2,
            color = PulseTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        PulseButton(
            text = stringResource(type.addTitle),
            onClick = onAdd,
            enabled = enabled,
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.pulse_purchase_add),
                    contentDescription = null,
                    modifier = Modifier.size(PulseSpacing.medium),
                )
            },
        )
    }
}
