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
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseEmptyState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.components.HorizonTabScroll
import dev.abhishekan.pulse.feature.horizon.presentation.components.horizonGutter
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.LedgerRow

internal data class RecentTransactionsUiState(
    val section: HorizonSection<HorizonTransactionPage>,
    val canOpenHistory: Boolean,
    val showAdd: Boolean,
    val reserveAddSpace: Boolean,
    val showFullEmptyState: Boolean,
    val canLog: Boolean,
)

internal sealed interface RecentTransactionsAction {
    data object Retry : RecentTransactionsAction

    data object OpenHistory : RecentTransactionsAction

    data class Select(
        val id: Long,
    ) : RecentTransactionsAction

    data object Log : RecentTransactionsAction

    data object Import : RecentTransactionsAction
}

@Composable
internal fun RecentTransactionsTabScreen(
    state: RecentTransactionsUiState,
    currency: String,
    width: PulseWindowSize,
    scrollKey: String,
    scrollStates: SaveableStateHolder,
    onAction: (RecentTransactionsAction) -> Unit,
    recovery: @Composable () -> Unit,
    saving: @Composable () -> Unit,
) {
    val section = state.section
    val addSpacing = if (state.reserveAddSpace) 56.dp + width.pagePadding else 0.dp
    Box(Modifier.fillMaxSize()) {
        if (state.showFullEmptyState && section.error == null && section.value?.items?.isEmpty() == true) {
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
                    section.error?.let { PulseErrorState(it.message, { onAction(RecentTransactionsAction.Retry) }) }
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
                                        LedgerRow(tx, wide = false) { onAction(RecentTransactionsAction.Select(tx.id)) }
                                        if (index < recent.lastIndex) PulseHorizontalDivider()
                                    }
                                }
                            }
                            PulseButton(
                                stringResource(R.string.pulse_horizon_view_all_transactions),
                                { onAction(RecentTransactionsAction.OpenHistory) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.canOpenHistory,
                                variant = PulseButtonVariant.Secondary,
                            )
                        }
                    }

                    saving()
                }
            }
        }
        if (state.showAdd) {
            RecentTransactionAdd(
                canLog = state.canLog,
                onLog = { onAction(RecentTransactionsAction.Log) },
                onImport = { onAction(RecentTransactionsAction.Import) },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = LocalPulseContentPadding.current.calculateBottomPadding())
                        .padding(end = width.horizonGutter, bottom = width.pagePadding),
            )
        }
    }
}
