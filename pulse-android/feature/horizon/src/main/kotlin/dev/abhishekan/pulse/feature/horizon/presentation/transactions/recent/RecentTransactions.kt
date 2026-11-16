package dev.abhishekan.pulse.feature.horizon.presentation.transactions.recent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseBottomSheet
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseExtendedFloatingActionButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.components.horizonGutter

@Composable
internal fun RecentTransactionsHeading(width: PulseWindowSize) {
    PulseText(
        stringResource(R.string.pulse_horizon_recent_transactions),
        Modifier.semantics { heading() },
        style =
            if (width == PulseWindowSize.Compact) {
                PulseTheme.typography.heading2.copy(fontSize = 29.sp)
            } else {
                PulseTheme.typography.heading1
            },
        color = PulseTheme.colors.textPrimary,
    )
}

@Composable
internal fun RecentTransactionAdd(
    canLog: Boolean,
    onLog: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showActions by rememberSaveable { mutableStateOf(false) }
    PulseExtendedFloatingActionButton(
        text = { PulseText(stringResource(R.string.pulse_horizon_add), style = PulseTextStyle.Label) },
        icon = { Icon(painterResource(R.drawable.pulse_purchase_add), contentDescription = null) },
        onClick = { showActions = true },
        modifier = modifier,
    )
    if (showActions) {
        PulseBottomSheet(
            title = stringResource(R.string.pulse_horizon_add_transactions),
            onDismissRequest = { showActions = false },
            expandOnOverflow = true,
        ) {
            Column(
                Modifier.padding(horizontal = PulseTokens.medium).padding(bottom = PulseTokens.medium),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
            ) {
                PulseButton(
                    text = stringResource(R.string.pulse_horizon_add_menu_log_transaction),
                    onClick = {
                        showActions = false
                        onLog()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canLog,
                )
                PulseButton(
                    text = stringResource(R.string.pulse_horizon_add_menu_import_statement),
                    onClick = {
                        showActions = false
                        onImport()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("horizon-statement-import"),
                    variant = PulseButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
internal fun EmptyRecentTransactions(
    width: PulseWindowSize,
    modifier: Modifier = Modifier,
) {
    val wide = width != PulseWindowSize.Compact
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = width.horizonGutter)
            .padding(
                top = if (wide) 28.dp else 24.dp,
                bottom = LocalPulseContentPadding.current.calculateBottomPadding(),
            ),
    ) {
        RecentTransactionsHeading(width)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (wide) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (width == PulseWindowSize.Expanded) 56.dp else 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TransactionGhostRows(Modifier.weight(1f).padding(bottom = 80.dp), wide = true)
                        FirstTransactionPrompt(Modifier.weight(1f), wide = true)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        TransactionGhostRows(Modifier.fillMaxWidth(), wide = false)
                        FirstTransactionPrompt(Modifier.fillMaxWidth(), wide = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstTransactionPrompt(
    modifier: Modifier,
    wide: Boolean,
) {
    val colors = PulseTheme.colors
    val typography = PulseTheme.typography
    Column(modifier) {
        PulseText(
            stringResource(R.string.pulse_horizon_nothing_logged_yet),
            Modifier.semantics { heading() },
            style =
                if (wide) {
                    typography.heading1.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold)
                } else {
                    typography.heading3.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold)
                },
            color = colors.textPrimary,
        )
        PulseText(
            stringResource(R.string.pulse_horizon_log_first_transaction),
            Modifier.padding(top = if (wide) 20.dp else 14.dp),
            style = typography.body1.copy(fontSize = if (wide) 18.sp else 16.5.sp, lineHeight = if (wide) 28.sp else 25.sp),
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun TransactionGhostRows(
    modifier: Modifier,
    wide: Boolean,
) {
    val colors = PulseTheme.colors
    val gap = if (wide) 12.dp else 10.dp
    val rowHeight = 74.dp
    // The drawing has no text, actions, or accessibility semantics.
    Canvas(modifier.height(rowHeight * 3 + gap * 2)) {
        val strokeWidth = 1.5.dp.toPx()
        val inset = strokeWidth / 2
        val dash = 3.dp.toPx()
        val stroke = Stroke(strokeWidth, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash / 2)))
        val outerRadius = CornerRadius(16.dp.toPx())
        val height = rowHeight.toPx()
        val iconLeft = minOf(17.dp.toPx(), size.width * 0.05f)
        val iconRadius = minOf(21.dp.toPx(), size.width * 0.07f)
        val amountWidth = minOf(56.dp.toPx(), size.width * 0.16f)
        val amountLeft = size.width - iconLeft - amountWidth
        val textLeft = iconLeft + iconRadius * 2 + 12.dp.toPx()
        val textWidth = (amountLeft - textLeft).coerceAtLeast(0f)
        val levels = if (wide) listOf(1f, 0.62f, 0.30f) else listOf(1f, 0.52f, 0.10f)
        levels.forEachIndexed { index, alpha ->
            val top = index * (height + gap.toPx())
            val center = top + height / 2
            val outline = colors.border.copy(alpha = alpha)
            val fill = colors.surfaceMuted.copy(alpha = alpha)
            drawRoundRect(
                outline,
                Offset(inset, top + inset),
                Size(size.width - strokeWidth, height - strokeWidth),
                outerRadius,
                style = stroke,
            )
            drawCircle(outline, iconRadius, Offset(iconLeft + iconRadius, center), style = stroke)
            val titleWidth =
                when (index) {
                    0 -> 0.60f
                    1 -> 0.46f
                    else -> 0.68f
                }
            drawRoundRect(
                fill,
                Offset(textLeft, center - 13.dp.toPx()),
                Size(textWidth * titleWidth, 10.dp.toPx()),
                CornerRadius(5.dp.toPx()),
            )
            drawRoundRect(
                fill.copy(alpha = alpha * 0.72f),
                Offset(textLeft, center + 5.dp.toPx()),
                Size(textWidth * 0.43f, 8.dp.toPx()),
                CornerRadius(4.dp.toPx()),
            )
            drawRoundRect(fill, Offset(amountLeft, center - 6.dp.toPx()), Size(amountWidth, 12.dp.toPx()), CornerRadius(6.dp.toPx()))
        }
    }
}
