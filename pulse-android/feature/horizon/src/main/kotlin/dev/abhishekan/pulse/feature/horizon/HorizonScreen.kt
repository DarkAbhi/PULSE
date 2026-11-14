package dev.abhishekan.pulse.feature.horizon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.components.PulseFormLayout
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable
fun HorizonRoute(viewModel: HorizonViewModel = koinViewModel()) {
    val accountCurrency by UserCurrency.changes
        .collectAsStateWithLifecycle()
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    LaunchedEffect(timeZone) { viewModel.refresh() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searchOpen by rememberSaveable(state.historyOpen) { mutableStateOf(false) }
    var focusSearch by remember { mutableStateOf(false) }
    PulseHideNavigation(state.draft != null || state.subscriptionId != null || state.historyOpen)
    PulseSnackbarEffect(state.message.takeUnless { state.reviewRequired }, viewModel::consumeMessage)
    val scrollStates = rememberSaveableStateHolder()
    PulseScreenTransition(
        state,
        screenDepth = {
            when {
                it.transactionDraft != null -> 2
                it.draft != null && it.historyOpen -> 2
                it.draft != null || it.historyOpen -> 1
                else -> 0
            }
        },
    ) { screen, active ->
        PulseScreenScaffold(
            title =
                when {
                    screen.draft != null -> stringResource(screen.draft.topBarTitle())
                    screen.historyOpen -> stringResource(R.string.pulse_horizon_all_transactions)
                    else -> ""
                },
            showTopBar = screen.draft != null || screen.historyOpen,
            backLabel =
                stringResource(
                    when {
                        screen.transactionDraft != null -> R.string.pulse_horizon_back_to_transaction
                        screen.draft != null && screen.historyOpen -> R.string.pulse_horizon_back_to_history
                        screen.historyOpen -> R.string.pulse_horizon_back_to_transactions
                        else -> R.string.pulse_horizon_back_to_horizon
                    },
                ),
            backEnabled = !screen.busy,
            actions = {
                if (screen.draft == null && screen.historyOpen) {
                    PulseButton(
                        stringResource(R.string.pulse_horizon_search_transactions),
                        {
                            searchOpen = true
                            focusSearch = true
                        },
                        enabled = !screen.busy,
                        style = PulseButtonStyle.Icon,
                    ) { Icon(painterResource(R.drawable.pulse_horizon_search), null) }
                }
                HorizonTopBarActions(screen, viewModel::deleteObligation, viewModel::addTransactionCategory)
            },
        ) {
            key(timeZone, accountCurrency) {
                HorizonScreen(screen, viewModel, scrollStates, active, searchOpen, focusSearch) { focusSearch = false }
            }
        }
    }
}

@Composable
internal fun HorizonScreen(
    state: HorizonUiState,
    vm: HorizonViewModel,
    scrollStates: SaveableStateHolder,
    active: Boolean,
    searchOpen: Boolean,
    focusSearch: Boolean,
    onSearchFocused: () -> Unit,
) {
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    var deletion by remember { mutableStateOf<Pair<HorizonChange, UiText>?>(null) }
    var selectedTransaction by rememberSaveable { mutableStateOf<Long?>(null) }
    val closeForm = {
        records = false
        vm.discard()
    }
    val cancel = { if (state.draft?.dirty == true) discard = true else closeForm() }
    BackHandler(active && (state.draft != null || state.busy || state.subscriptionId != null || state.historyOpen)) {
        when {
            state.busy -> Unit
            state.draft != null -> cancel()
            state.historyOpen -> vm.closeHistory()
            else -> vm.hidePayments()
        }
    }
    val currency = UserCurrency.code
    val canMutate = !state.busy && !state.reviewRequired && state.overview.value != null && state.draft == null
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { width ->
        Column(
            Modifier
                .widthIn(
                    max = PulseTokens.contentWidth,
                ).fillMaxWidth()
                .padding(top = LocalPulseContentPadding.current.calculateTopPadding())
                .imePadding(),
        ) {
            val showHistory = state.historyOpen && (state.draft == null || records)
            if (!showHistory && (state.draft == null || records)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState(),
                        ).padding(start = width.pagePadding, top = width.pagePadding, end = width.pagePadding),
                    horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                ) {
                    HorizonTab.entries.forEach { tab ->
                        PulseFilterChip(
                            stringResource(tab.label),
                            tab == state.tab,
                            { vm.tab(tab) },
                            enabled = !state.busy,
                            showSelectedIcon = false,
                        )
                    }
                }
            }
            val review: @Composable () -> Unit = {
                if (state.reviewRequired) {
                    PulseCard(Modifier.fillMaxWidth()) {
                        PulseText(
                            stringResource(R.string.pulse_horizon_review_before_another_change),
                            style = PulseTextStyle.Title,
                        )
                        PulseText(
                            stringResource(
                                R.string.pulse_horizon_the_server_may_have_saved_the_last_change_check,
                            ),
                        )
                        PulseButton(
                            stringResource(R.string.pulse_horizon_refresh_server_records),
                            vm::refresh,
                            style = PulseButtonStyle.Text,
                        )
                        if (state.draft !=
                            null
                        ) {
                            PulseButton(
                                if (records) {
                                    stringResource(
                                        R.string.pulse_horizon_return_to_draft,
                                    )
                                } else {
                                    stringResource(
                                        R.string.pulse_horizon_view_server_records,
                                    )
                                },
                                {
                                    records =
                                        !records
                                },
                                style = PulseButtonStyle.Text,
                            )
                        }
                        PulseButton(
                            stringResource(
                                R.string.pulse_horizon_i_reviewed_the_server_records,
                            ),
                            vm::acknowledgeReview,
                            enabled =
                                state.reviewReady && !state.busy,
                        )
                    }
                }
            }
            val resumeDraft: @Composable () -> Unit = {
                if (state.draft != null && !state.reviewRequired) {
                    PulseButton(
                        stringResource(R.string.pulse_horizon_resume_unsaved_edits),
                        { records = false },
                        style = PulseButtonStyle.Text,
                    )
                }
            }
            val content: @Composable () -> Unit = {
                val scroll = rememberScrollState()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(bottom = LocalPulseContentPadding.current.calculateBottomPadding())
                        .padding(width.pagePadding),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                ) {
                    review()
                    resumeDraft()
                    if (records && state.transactionDraft != null && state.reviewRequired) {
                        SectionState(state.overview, vm::refreshOverview) { overview ->
                            PulseText(stringResource(R.string.pulse_horizon_filter_categories), style = PulseTextStyle.Title)
                            overview.categories.forEach { PulseText(it.name) }
                        }
                    }
                    val edit: (HorizonDraft) -> Unit = {
                        records = false
                        vm.edit(it)
                    }
                    val create: (HorizonForm) -> Unit = {
                        records = false
                        vm.newForm(it)
                    }
                    val remove: (
                        HorizonRecord,
                        Long,
                        UiText,
                    ) -> Unit = { kind, id, body ->
                        deletion =
                            HorizonChange.Delete(kind, id) to body
                    }
                    when (state.tab) {
                        HorizonTab.Overview -> {
                            SectionState(state.overview, vm::refreshOverview) { data ->
                                HorizonMetrics(data, canMutate) {
                                    create(HorizonForm.Config)
                                }
                                ProjectionCard(data)
                            }
                        }
                        HorizonTab.Transactions -> {
                            PulseText(
                                stringResource(R.string.pulse_horizon_recent_transactions),
                                style = PulseTextStyle.Heading2,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                                verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
                            ) {
                                PulseButton(
                                    stringResource(R.string.pulse_horizon_log_transaction),
                                    { create(HorizonForm.Transaction) },
                                    enabled = canMutate,
                                )
                                PulseButton(
                                    stringResource(R.string.pulse_horizon_view_all_transactions),
                                    vm::openHistory,
                                    enabled = !state.busy && state.draft == null,
                                    variant = PulseButtonVariant.Secondary,
                                )
                            }
                            SectionState(state.recent, vm::refreshRecent) { page ->
                                if (currency.isBlank()) {
                                    PulseText(stringResource(R.string.pulse_horizon_currency_unavailable), style = PulseTextStyle.Label)
                                }
                                val recent = page.items.take(5)
                                if (recent.isEmpty()) {
                                    PulseEmptyState(
                                        stringResource(R.string.pulse_horizon_no_transactions),
                                        stringResource(R.string.pulse_horizon_log_first_transaction),
                                    )
                                } else {
                                    Column {
                                        recent.forEachIndexed { index, tx ->
                                            key(tx.id) {
                                                LedgerRow(tx, wide = false) { selectedTransaction = tx.id }
                                                if (index < recent.lastIndex) PulseHorizontalDivider()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HorizonTab.Budgets -> {
                            HorizonSectionAction(
                                R.string.pulse_horizon_budgets_heading,
                                R.string.pulse_horizon_add_budget,
                                canMutate,
                            ) { create(HorizonForm.Budget) }
                            SectionState(state.overview, vm::refreshOverview) { data ->
                                if (data.budgets.isEmpty()) {
                                    PulseEmptyState(
                                        stringResource(R.string.pulse_horizon_no_budgets_yet),
                                        stringResource(
                                            R.string.pulse_horizon_create_a_budget_to_group_related_spending_and_obligations,
                                        ),
                                    )
                                }
                                BudgetGrid(data.budgets) { budget ->
                                    BudgetCard(
                                        budget,
                                        currency,
                                        canMutate,
                                        {
                                            edit(
                                                HorizonDraft(
                                                    HorizonForm.Budget,
                                                    budget.id,
                                                    budget.name,
                                                    budget.allocated.toPlainString(),
                                                    currency,
                                                ),
                                            )
                                        },
                                        {
                                            remove(
                                                HorizonRecord.Budget,
                                                budget.id,
                                                uiText(
                                                    R.string.pulse_horizon_delete_budget_confirmation,
                                                    budget.name,
                                                ),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        HorizonTab.Obligations -> {
                            HorizonSectionAction(
                                R.string.pulse_horizon_obligations_heading,
                                R.string.pulse_horizon_add_obligation,
                                canMutate,
                            ) {
                                create(HorizonForm.Deduction)
                            }
                            SectionState(state.overview, vm::refreshOverview) { data ->
                                val activeSubscriptions = data.subscriptions.filter { it.status == "active" }
                                if (data.deductions.isEmpty() && activeSubscriptions.isEmpty()) {
                                    PulseEmptyState(
                                        stringResource(
                                            R.string.pulse_horizon_no_fixed_obligations,
                                        ),
                                        stringResource(
                                            R.string.pulse_horizon_add_recurring_bills_investments_or_other_commitments,
                                        ),
                                    )
                                }
                                ObligationGrid(data.deductions, { it.id }) { item ->
                                    ObligationCard(
                                        item = item,
                                        budgetName =
                                            item.budgetId?.let { id ->
                                                data.budgets.find { it.id == id }?.name
                                                    ?: stringResource(R.string.pulse_horizon_unavailable)
                                            },
                                        enabled = canMutate,
                                        edit = {
                                            edit(
                                                HorizonDraft(
                                                    HorizonForm.Deduction,
                                                    item.id,
                                                    item.name,
                                                    item.amount.toPlainString(),
                                                    currency,
                                                    category = item.category,
                                                    dueDay = item.dueDay?.toString().orEmpty(),
                                                    active = item.active,
                                                    budgetId = item.budgetId,
                                                ),
                                            )
                                        },
                                        delete = {
                                            remove(
                                                HorizonRecord.Deduction,
                                                item.id,
                                                uiText(
                                                    R.string.pulse_horizon_delete_1_s_this_removes_the_fixed_obligation_and,
                                                    item.name,
                                                ),
                                            )
                                        },
                                    )
                                }
                                ObligationGrid(activeSubscriptions, { it.id }) { item ->
                                    SubscriptionObligationCard(
                                        item = item,
                                        budgetName = item.budgetId?.let { id -> data.budgets.find { it.id == id }?.name },
                                        categoryName = item.categoryId?.let { id -> data.categories.find { it.id == id }?.name },
                                        enabled = canMutate,
                                        manage = { edit(item.draft(currency)) },
                                    )
                                }
                            }
                        }
                        HorizonTab.Subscriptions -> {
                            SectionState(state.overview, vm::refreshOverview) { data ->
                                SubscriptionList(
                                    data = data,
                                    canMutate = canMutate,
                                    canViewPayments = !state.busy,
                                    add = { create(HorizonForm.Subscription) },
                                    edit = { edit(it.draft(currency)) },
                                    delete = { item ->
                                        remove(
                                            HorizonRecord.Subscription,
                                            item.id,
                                            uiText(
                                                R.string.pulse_horizon_delete_subscription_confirmation,
                                                item.name,
                                            ),
                                        )
                                    },
                                    payments = { vm.showPayments(it.id) },
                                )
                            }
                        }
                        HorizonTab.Purchases -> {
                            SectionState(state.purchases, vm::refreshPurchases) { list ->
                                PlannedPurchaseList(
                                    purchases = list,
                                    currency = currency,
                                    enabled = canMutate,
                                    add = { create(HorizonForm.Purchase) },
                                    delete = { item ->
                                        remove(
                                            HorizonRecord.Purchase,
                                            item.id,
                                            uiText(R.string.pulse_horizon_remove_1_s_from_next_month_s_planned_purchases, item.name),
                                        )
                                    },
                                    clear = {
                                        deletion = HorizonChange.ClearPurchases to
                                            uiText(R.string.pulse_horizon_permanently_remove_every_purchase_in_next_month_s_list)
                                    },
                                )
                            }
                        }
                    }
                    if (state.busy && (state.draft == null || records)) {
                        PulseLoadingState(
                            stringResource(R.string.pulse_horizon_saving_your_change),
                        )
                    }
                }
            }
            val sectionLoading =
                when (state.tab) {
                    HorizonTab.Transactions -> false
                    HorizonTab.Purchases -> state.purchases.loading && state.purchases.value == null
                    else -> state.overview.loading && state.overview.value == null
                }
            if (showHistory) {
                val bottom = LocalPulseContentPadding.current.calculateBottomPadding()
                val wide = width == PulseWindowSize.Expanded && LocalDensity.current.fontScale <= 1.15f
                scrollStates.SaveableStateProvider(if (state.draft == null) "transaction-history" else "review-history") {
                    TransactionHistory(
                        state,
                        vm,
                        active && state.draft == null,
                        searchOpen,
                        active && focusSearch,
                        onSearchFocused,
                        PaddingValues(start = width.pagePadding, end = width.pagePadding, top = PulseTokens.small, bottom = bottom),
                        wide,
                        { selectedTransaction = it.id },
                    ) {
                        review()
                        resumeDraft()
                    }
                }
            } else if (sectionLoading &&
                state.draft == null &&
                !state.reviewRequired
            ) {
                PulseCenteredLoadingState(
                    stringResource(R.string.pulse_horizon_loading_your_horizon),
                    Modifier.fillMaxWidth().weight(1f),
                )
            } else if (state.draft != null && !records) {
                key(state.draft.kind, state.draft.id) {
                    HorizonFormScreen(state, vm, width, cancel, review)
                }
            } else {
                scrollStates.SaveableStateProvider((if (state.draft == null) "root-" else "review-") + state.tab.name) { content() }
            }
        }
    }
    if (active && state.tab == HorizonTab.Subscriptions && state.subscriptionId != null && state.draft == null) {
        SubscriptionPaymentsSheet(state, vm, !state.busy && !state.reviewRequired && state.overview.value != null) { id ->
            deletion = HorizonChange.LinkPayment(state.subscriptionId, id) to
                uiText(R.string.pulse_horizon_link_this_debit_to_the_subscription_the_transaction_remains)
        }
    }
    val displayedTransactions = if (state.historyOpen) state.history.value else state.recent.value
    if (active && state.draft == null && state.tab == HorizonTab.Transactions) {
        displayedTransactions?.items?.find { it.id == selectedTransaction }?.let { tx ->
            TransactionDetails(
                tx,
                currency,
                canMutate,
                { selectedTransaction = null },
                { vm.edit(it.draft(currency)) },
                { transaction ->
                    deletion = HorizonChange.Delete(HorizonRecord.Transaction, transaction.id) to
                        uiText(R.string.pulse_horizon_delete_1_s_this_permanently_removes_the_transaction_and, transaction.name)
                },
            )
        }
    }
    if (active && discard) {
        PulseDialog(
            stringResource(R.string.pulse_horizon_discard_unsaved_changes),
            stringResource(R.string.pulse_horizon_your_edits_have_not_been_saved_leaving_this_form),
            stringResource(R.string.pulse_horizon_discard_edits),
            {
                discard = false
                closeForm()
            },
            { discard = false },
        )
    }
    deletion?.takeIf { active }?.let { (change, body) ->
        PulseDialog(
            if (change is HorizonChange.LinkPayment) {
                stringResource(
                    R.string.pulse_horizon_link_payment,
                )
            } else {
                stringResource(R.string.pulse_horizon_confirm_deletion)
            },
            body.asString(),
            if (change is HorizonChange.LinkPayment) {
                stringResource(
                    R.string.pulse_horizon_link_payment_action,
                )
            } else {
                stringResource(R.string.pulse_horizon_delete)
            },
            {
                deletion =
                    null
                val deletingDraft =
                    change is HorizonChange.Delete &&
                        change.kind == HorizonRecord.Deduction &&
                        state.draft?.kind == HorizonForm.Deduction &&
                        state.draft.id == change.id
                vm.mutate(change, closeDraft = deletingDraft)
            },
            { deletion = null },
        )
    }
}

@Composable
private fun HorizonFormScreen(
    state: HorizonUiState,
    vm: HorizonViewModel,
    width: PulseWindowSize,
    cancel: () -> Unit,
    review: @Composable () -> Unit,
) {
    val draft = checkNotNull(state.draft)
    val data = state.overview.value
    val canSave = !state.reviewRequired && !state.busy && data != null
    val pagePadding = width.pagePadding
    val bottomPadding = LocalPulseContentPadding.current.calculateBottomPadding()
    val prefix: @Composable () -> Unit = {
        review()
        state.overview.error?.let { PulseErrorState(it.message, vm::refreshOverview) }
    }
    if (draft.kind == HorizonForm.Purchase) {
        Box(Modifier.fillMaxSize().padding(bottom = bottomPadding)) {
            PlannedPurchaseEditor(
                draft,
                data,
                state.busy,
                state.formError,
                state.reviewRequired,
                vm::update,
                vm::save,
                cancel,
                pagePadding,
                prefix,
            )
        }
        return
    }

    val footerWidth =
        when (draft.kind) {
            HorizonForm.Config -> PulseTokens.readingWidth
            HorizonForm.Deduction -> 1000.dp
            else -> PulseTokens.contentWidth
        }
    val keyboard = LocalSoftwareKeyboardController.current
    PulseFormLayout(
        modifier = Modifier.padding(bottom = bottomPadding),
        scrollContentModifier = Modifier.padding(pagePadding),
        footer = {
            PulseFormActions(
                primary =
                    PulseFormAction(
                        stringResource(CoreUiR.string.pulse_ui_save),
                        {
                            keyboard?.hide()
                            vm.save()
                        },
                        enabled = canSave,
                        loading = state.busy,
                    ),
                secondary = PulseFormAction(stringResource(CoreUiR.string.pulse_ui_cancel), cancel, enabled = !state.busy),
                modifier =
                    Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = pagePadding)
                        .widthIn(max = footerWidth)
                        .testTag("horizon-editor-actions"),
            )
        },
    ) {
        prefix()
        when (draft.kind) {
            HorizonForm.Subscription -> SubscriptionEditor(draft, data, state.busy, state.formError, vm::update)
            HorizonForm.Transaction -> TransactionEditor(draft, data, state.busy, state.formError, vm::update)
            HorizonForm.Budget ->
                BudgetEditor(
                    draft,
                    state.busy,
                    state.formError,
                    LocalConfiguration.current.smallestScreenWidthDp >= 600 && width != PulseWindowSize.Compact,
                    canSave,
                    vm::update,
                    vm::save,
                )
            HorizonForm.Category -> CategoryEditor(draft, state.busy, state.formError, canSave, vm::update, vm::save)
            HorizonForm.Deduction -> FixedObligationEditor(draft, data, state.busy, state.formError, vm::update)
            HorizonForm.Config -> HorizonEditor(draft, data, state.busy, state.formError, vm::update, vm::save)
            HorizonForm.Purchase -> Unit
        }
    }
}

@Composable
internal fun <T> SectionState(
    section: HorizonSection<T>,
    retry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}

@Composable
internal fun HorizonSectionAction(
    title: Int,
    action: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
    ) {
        PulseText(stringResource(title), style = PulseTextStyle.Heading2)
        PulseButton(stringResource(action), onClick, enabled = enabled)
    }
}
