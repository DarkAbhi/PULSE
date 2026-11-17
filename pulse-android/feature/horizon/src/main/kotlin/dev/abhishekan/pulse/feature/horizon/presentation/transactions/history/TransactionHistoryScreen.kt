package dev.abhishekan.pulse.feature.horizon.presentation.transactions.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTopAppBar
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.LedgerRow
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.ledgerMonths
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.util.Locale
import dev.abhishekan.pulse.core.ui.R as CoreUiR

internal data class TransactionHistoryUiState(
    val history: HorizonSection<HorizonTransactionPage>,
    val overview: HorizonSection<HorizonOverview>,
    val historySearch: String,
    val historyType: String?,
    val historyCategoryId: Long?,
    val busy: Boolean,
    val reviewingDraft: Boolean,
    val reviewRequired: Boolean,
)

internal sealed interface TransactionHistoryAction {
    data object Back : TransactionHistoryAction

    data class Filter(
        val search: String,
        val type: String?,
        val categoryId: Long?,
    ) : TransactionHistoryAction

    data object RetryCategories : TransactionHistoryAction

    data object Retry : TransactionHistoryAction

    data object LoadMore : TransactionHistoryAction

    data class Select(
        val id: Long,
    ) : TransactionHistoryAction

    data object Log : TransactionHistoryAction
}

@Composable
internal fun TransactionHistoryScreen(
    state: TransactionHistoryUiState,
    active: Boolean,
    padding: PaddingValues,
    tablet: Boolean,
    wide: Boolean,
    canLog: Boolean,
    onAction: (TransactionHistoryAction) -> Unit,
    review: @Composable () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf(state.historySearch) }
    val dispatch by rememberUpdatedState(onAction)
    LaunchedEffect(active, search, state.historySearch, state.historyType, state.historyCategoryId, state.busy) {
        if (active && search != state.historySearch && !state.busy) {
            delay(350)
            dispatch(TransactionHistoryAction.Filter(search, state.historyType, state.historyCategoryId))
        }
    }

    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var focusSearch by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val clearSearch: () -> Unit = {
        search = ""
        onAction(TransactionHistoryAction.Filter("", state.historyType, state.historyCategoryId))
        focusManager.clearFocus()
        keyboard?.hide()
    }
    val searchField: @Composable (Modifier) -> Unit = { modifier ->
        val label = stringResource(R.string.pulse_horizon_search_transactions)
        PulseTextField(
            search,
            { search = it },
            label,
            modifier.focusRequester(focus),
            enabled = !state.busy,
            leadingIcon = { Icon(painterResource(R.drawable.pulse_horizon_search), null, tint = PulseTheme.colors.textSecondary) },
            trailingIcon =
                if (search.isNotEmpty()) {
                    {
                        PulseButton(
                            stringResource(R.string.pulse_horizon_clear_search),
                            clearSearch,
                            style = PulseButtonStyle.Icon,
                            enabled = !state.busy,
                        ) { Icon(painterResource(R.drawable.pulse_purchase_clear), null) }
                    }
                } else {
                    null
                },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions =
                KeyboardActions(onSearch = {
                    onAction(TransactionHistoryAction.Filter(search, state.historyType, state.historyCategoryId))
                    focusManager.clearFocus()
                    keyboard?.hide()
                }),
        )
        LaunchedEffect(focusSearch, active) {
            if (focusSearch && active) {
                focus.requestFocus()
                keyboard?.show()
                focusSearch = false
            }
        }
    }

    TransactionHistoryScreenContent(
        state = state,
        active = active,
        padding = padding,
        tablet = tablet,
        wide = wide,
        canLog = canLog,
        search = search,
        searchOpen = searchOpen,
        searchField = searchField,
        clearSearch = clearSearch,
        onOpenSearch = {
            searchOpen = true
            focusSearch = true
        },
        onAction = {
            if (it == TransactionHistoryAction.Log) keyboard?.hide()
            onAction(it)
        },
        review = review,
    )
}

@Composable
private fun TransactionHistoryScreenContent(
    state: TransactionHistoryUiState,
    active: Boolean,
    padding: PaddingValues,
    tablet: Boolean,
    wide: Boolean,
    canLog: Boolean,
    search: String,
    searchOpen: Boolean,
    searchField: @Composable (Modifier) -> Unit,
    clearSearch: () -> Unit,
    onOpenSearch: () -> Unit,
    onAction: (TransactionHistoryAction) -> Unit,
    review: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (!state.reviewingDraft) {
            PulseTopAppBar(
                stringResource(R.string.pulse_horizon_all_transactions),
                navigationIcon = {
                    PulseButton(
                        stringResource(R.string.pulse_horizon_back_to_transactions),
                        { onAction(TransactionHistoryAction.Back) },
                        enabled = !state.busy,
                        style = PulseButtonStyle.Icon,
                    ) { Icon(painterResource(CoreUiR.drawable.pulse_back), null) }
                },
                actions = {
                    if (tablet) {
                        searchField(Modifier.fillMaxWidth(0.38f).padding(end = PulseTokens.medium))
                    } else {
                        PulseButton(
                            stringResource(R.string.pulse_horizon_search_transactions),
                            onOpenSearch,
                            enabled = !state.busy,
                            style = PulseButtonStyle.Icon,
                        ) { Icon(painterResource(R.drawable.pulse_horizon_search), null) }
                    }
                },
            )
        }
        val direction = LocalLayoutDirection.current
        Column(
            Modifier.padding(start = padding.calculateStartPadding(direction), end = padding.calculateEndPadding(direction)),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            if ((!tablet || state.reviewingDraft) && (searchOpen || search.isNotEmpty())) {
                searchField(Modifier.fillMaxWidth())
            }
            LedgerFilters(state, search, onAction)
        }

        val history = state.history
        val page = history.value
        val showingCurrentSearch = search == state.historySearch
        val months = remember(page) { ledgerMonths(page?.items.orEmpty()) }
        val locale = Locale.getDefault()
        val monthFormat = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val emptyHeight = maxHeight * 0.9f
            key(state.historySearch, state.historyType, state.historyCategoryId) {
                LazyColumn(
                    Modifier.fillMaxSize().testTag("transaction-history-list"),
                    state = rememberLazyListState(),
                    contentPadding = padding,
                ) {
                    if (state.reviewRequired || state.reviewingDraft) item("review") { review() }
                    if (history.loading && page == null) {
                        item("loading") {
                            PulseCenteredLoadingState(
                                stringResource(R.string.pulse_horizon_loading_transactions),
                                Modifier.fillMaxWidth().fillParentMaxHeight(),
                            )
                        }
                    }
                    if (page != null &&
                        page.items.isEmpty() &&
                        history.error == null &&
                        !history.loading &&
                        showingCurrentSearch
                    ) {
                        item("empty") {
                            EmptyTransactionHistory(
                                state = state,
                                tablet = tablet,
                                canLog = canLog,
                                clearSearch = clearSearch,
                                clearFilters = { onAction(TransactionHistoryAction.Filter(search, null, null)) },
                                log = { onAction(TransactionHistoryAction.Log) },
                                modifier =
                                    if (!state.reviewingDraft && !state.reviewRequired) Modifier.heightIn(min = emptyHeight) else Modifier,
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
                            LedgerRow(
                                tx,
                                wide,
                                open = if (showingCurrentSearch) ({ onAction(TransactionHistoryAction.Select(tx.id)) }) else null,
                            )
                            PulseHorizontalDivider()
                        }
                    }
                    if (history.error != null) {
                        item("error") {
                            PulseErrorState(history.error.message, { onAction(TransactionHistoryAction.Retry) })
                        }
                    } else if (page != null && (history.loading || page.page < page.totalPages)) {
                        item("more") {
                            TransactionHistoryAppendFooter(active, page.page, history.loading, onAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionHistoryAppendFooter(
    active: Boolean,
    page: Int,
    loading: Boolean,
    onAction: (TransactionHistoryAction) -> Unit,
) {
    val dispatch by rememberUpdatedState(onAction)
    LaunchedEffect(active, page, loading) {
        if (active && !loading) dispatch(TransactionHistoryAction.LoadMore)
    }
    PulseLoadingState(stringResource(R.string.pulse_horizon_loading_transactions))
}
