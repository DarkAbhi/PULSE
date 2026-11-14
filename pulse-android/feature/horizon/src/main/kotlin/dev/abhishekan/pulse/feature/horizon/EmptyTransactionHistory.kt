package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun EmptyTransactionHistory(
    state: HorizonUiState,
    tablet: Boolean,
    canLog: Boolean,
    clearSearch: () -> Unit,
    clearFilters: () -> Unit,
    log: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searching = state.historySearch.isNotBlank()
    val filtered = state.historyType != null || state.historyCategoryId != null
    val title =
        when {
            searching -> stringResource(R.string.pulse_horizon_no_search_results, state.historySearch.trim())
            filtered -> stringResource(R.string.pulse_horizon_no_filter_matches)
            else -> stringResource(R.string.pulse_horizon_no_transactions)
        }
    val category =
        state.overview.value
            ?.categories
            ?.find { it.id == state.historyCategoryId }
            ?.name
    val message =
        when {
            searching && filtered -> stringResource(R.string.pulse_horizon_no_search_results_filtered_help)
            searching -> stringResource(R.string.pulse_horizon_no_search_results_help)
            category != null && state.historyType == "credit" -> stringResource(R.string.pulse_horizon_no_income_in_category, category)
            category != null && state.historyType == "debit" -> stringResource(R.string.pulse_horizon_no_spending_in_category, category)
            filtered -> stringResource(R.string.pulse_horizon_no_filter_matches_help)
            else -> stringResource(R.string.pulse_horizon_log_first_transaction)
        }
    Column(
        modifier
            .widthIn(max = PulseTokens.readingWidth)
            .fillMaxWidth()
            .padding(vertical = PulseTokens.cardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        HistoryEmptyIllustration(filtered && !searching)
        PulseText(
            title,
            Modifier.padding(top = PulseTokens.cardPadding).semantics { heading() },
            style = PulseTextStyle.Heading3,
            textAlign = TextAlign.Center,
        )
        PulseText(
            message,
            Modifier.padding(top = PulseTokens.medium, bottom = PulseTokens.cardPadding),
            color = PulseTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
            verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.compact),
            maxItemsInEachRow = if (tablet) 2 else 1,
        ) {
            if (searching || filtered) {
                PulseButton(
                    stringResource(if (searching) R.string.pulse_horizon_clear_search else R.string.pulse_horizon_clear_filters),
                    if (searching) clearSearch else clearFilters,
                    Modifier.weight(1f).heightIn(min = 56.dp),
                    enabled = !state.busy,
                )
            }
            PulseButton(
                stringResource(R.string.pulse_horizon_log_transaction),
                log,
                Modifier.weight(1f).heightIn(min = 56.dp),
                enabled = canLog,
                variant = if (searching || filtered) PulseButtonVariant.Secondary else PulseButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun HistoryEmptyIllustration(filtered: Boolean) {
    val colors = PulseTheme.colors
    val spacing = PulseTheme.spacing
    Box(
        Modifier.size(spacing.target + spacing.spacious).clearAndSetSemantics { hideFromAccessibility() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = PulseTokens.focusWidth.toPx()
            val dash = spacing.tiny.toPx()
            drawCircle(
                colors.border,
                radius = size.minDimension / 2 - stroke / 2,
                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash / 2))),
            )
        }
        Icon(
            painterResource(if (filtered) R.drawable.pulse_ledger_no_filters else R.drawable.pulse_ledger_no_search),
            contentDescription = null,
            modifier = Modifier.size(spacing.extraLarge),
            tint = colors.textSecondary,
        )
        Box(
            Modifier.align(Alignment.TopEnd).size(spacing.large).background(colors.primary, PulseTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            PulseText("0", style = PulseTextStyle.Label, color = colors.onPrimary)
        }
    }
}
