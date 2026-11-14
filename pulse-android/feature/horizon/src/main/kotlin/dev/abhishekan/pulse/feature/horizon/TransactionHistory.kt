package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun TransactionHistory(
    state: HorizonUiState,
    vm: HorizonViewModel,
    active: Boolean,
    searchOpen: Boolean,
    focusSearch: Boolean,
    onSearchFocused: () -> Unit,
    padding: PaddingValues,
    wide: Boolean,
    select: (HorizonTransaction) -> Unit,
    review: @Composable () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf(state.historySearch) }
    LaunchedEffect(active, search, state.historySearch, state.historyType, state.historyCategoryId, state.busy) {
        if (active && search != state.historySearch && !state.busy) {
            delay(350)
            vm.filterHistory(search, state.historyType, state.historyCategoryId)
        }
    }

    Column(Modifier.fillMaxSize()) {
        val direction = LocalLayoutDirection.current
        Column(
            Modifier.padding(start = padding.calculateStartPadding(direction), end = padding.calculateEndPadding(direction)),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            if (searchOpen || search.isNotEmpty()) {
                val focus = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current
                PulseTextField(
                    search,
                    { search = it },
                    stringResource(R.string.pulse_horizon_search_transactions),
                    Modifier.fillMaxWidth().focusRequester(focus),
                    enabled = !state.busy,
                )
                LaunchedEffect(focusSearch) {
                    if (focusSearch) {
                        focus.requestFocus()
                        keyboard?.show()
                        onSearchFocused()
                    }
                }
            }
            LedgerFilters(state, vm, search)
        }

        val history = state.history
        val page = history.value
        val months = remember(page) { ledgerMonths(page?.items.orEmpty()) }
        val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        key(state.historySearch, state.historyType, state.historyCategoryId) {
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f).testTag("transaction-history-list"),
                state = rememberLazyListState(),
                contentPadding = padding,
            ) {
                if (state.reviewRequired || state.draft != null) item("review") { review() }
                if (history.loading && page == null) {
                    item("loading") {
                        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_transactions), Modifier.fillMaxWidth())
                    }
                }
                if (page != null && page.items.isEmpty() && history.error == null && !history.loading) {
                    item("empty") {
                        PulseEmptyState(
                            stringResource(R.string.pulse_horizon_no_transactions),
                            stringResource(R.string.pulse_horizon_try_another_filter_or_record_your_first_transaction),
                        )
                    }
                }
                months.forEach { (month, transactions) ->
                    item(month.toString()) {
                        PulseText(
                            month.format(monthFormat),
                            Modifier.padding(top = PulseTokens.medium).semantics { heading() },
                            style = PulseTextStyle.Title,
                        )
                    }
                    items(transactions, key = { it.id }) { tx ->
                        LedgerRow(tx, wide, open = if (search == state.historySearch) ({ select(tx) }) else null)
                        PulseHorizontalDivider()
                    }
                }
                if (history.error != null) {
                    item("error") {
                        PulseErrorState(history.error.message, vm::retryHistory)
                    }
                } else if (page != null && (history.loading || page.page < page.totalPages)) {
                    item("more") {
                        LaunchedEffect(active, page.page, history.loading) {
                            if (active && !history.loading) vm.loadMoreHistory()
                        }
                        PulseLoadingState(stringResource(R.string.pulse_horizon_loading_transactions))
                    }
                }
            }
        }
    }
}
