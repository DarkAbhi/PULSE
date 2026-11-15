package dev.abhishekan.pulse.feature.horizon.presentation.transactions.recent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.components.HorizonTabScroll
import dev.abhishekan.pulse.feature.horizon.presentation.components.horizonGutter
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.LedgerRow

@Composable
internal fun RecentTransactionsTabScreen(
    section: HorizonSection<HorizonTransactionPage>,
    currency: String,
    width: PulseWindowSize,
    canOpenHistory: Boolean,
    onRetry: () -> Unit,
    onOpenHistory: () -> Unit,
    onSelect: (Long) -> Unit,
    showAdd: Boolean,
    reserveAddSpace: Boolean,
    showFullEmptyState: Boolean,
    canLog: Boolean,
    onLog: () -> Unit,
    onImport: () -> Unit,
    scrollKey: String,
    scrollStates: SaveableStateHolder,
    recovery: @Composable () -> Unit,
    saving: @Composable () -> Unit,
) {
    val addSpacing = if (reserveAddSpace) 56.dp + width.pagePadding else 0.dp
    Box(Modifier.fillMaxSize()) {
        if (showFullEmptyState && section.error == null && section.value?.items?.isEmpty() == true) {
            scrollStates.SaveableStateProvider("empty-transactions") {
                EmptyRecentTransactions(width, Modifier.fillMaxSize().padding(bottom = addSpacing))
            }
        } else {
            scrollStates.SaveableStateProvider(scrollKey) {
                HorizonTabScroll(width.horizonGutter, addSpacing) {
                    recovery()
                    RecentTransactionsHeading(width)
                    if (section.loading && section.value == null) {
                        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
                    }
                    section.error?.let { PulseErrorState(it.message, onRetry) }
                    section.value?.let { page ->
                        if (currency.isBlank()) {
                            PulseText(stringResource(R.string.pulse_horizon_currency_unavailable), style = PulseTextStyle.Label)
                        }
                        val recent = page.items.take(5)
                        if (recent.isEmpty()) {
                            PulseEmptyState(
                                stringResource(R.string.pulse_horizon_nothing_logged_yet),
                                stringResource(R.string.pulse_horizon_log_first_transaction),
                            )
                        } else {
                            Column {
                                recent.forEachIndexed { index, tx ->
                                    key(tx.id) {
                                        LedgerRow(tx, wide = false) { onSelect(tx.id) }
                                        if (index < recent.lastIndex) PulseHorizontalDivider()
                                    }
                                }
                            }
                            PulseButton(
                                stringResource(R.string.pulse_horizon_view_all_transactions),
                                onOpenHistory,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = canOpenHistory,
                                variant = PulseButtonVariant.Secondary,
                            )
                        }
                    }

                    saving()
                }
            }
        }
        if (showAdd) {
            RecentTransactionAdd(
                canLog = canLog,
                onLog = onLog,
                onImport = onImport,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = LocalPulseContentPadding.current.calculateBottomPadding())
                        .padding(end = width.horizonGutter, bottom = width.pagePadding),
            )
        }
    }
}
