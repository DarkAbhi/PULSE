package dev.abhishekan.pulse.feature.horizon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseRefreshIndicator
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel

@Composable
fun HorizonRoute(
    onDraftChanged: (Boolean) -> Unit,
    viewModel: HorizonViewModel = koinViewModel(),
) {
    val accountCurrency by dev.abhishekan.pulse.core.domain.model.UserCurrency.changes
        .collectAsStateWithLifecycle()
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(timeZone) { viewModel.refresh() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseSnackbarEffect(state.message.takeUnless { state.reviewRequired }, viewModel::consumeMessage)
    LaunchedEffect(state.draft?.dirty) { onDraftChanged(state.draft?.dirty == true) }
    androidx.compose.runtime.key(timeZone, accountCurrency) {
        HorizonScreen(state, viewModel)
    }
}

@Composable
internal fun HorizonScreen(
    state: HorizonUiState,
    vm: HorizonViewModel,
) {
    val scrollStates = rememberSaveableStateHolder()
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    var ledgerCategory by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletion by remember { mutableStateOf<Pair<HorizonChange, UiText>?>(null) }
    val cancel = { if (state.draft?.dirty == true) discard = true else vm.discard() }
    BackHandler(state.draft != null || state.busy || state.subscriptionId != null) {
        when {
            state.busy -> Unit
            state.draft != null -> cancel()
            else -> vm.hidePayments()
        }
    }
    val currency = dev.abhishekan.pulse.core.domain.model.UserCurrency.code
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { width ->
        Column(
            Modifier
                .widthIn(
                    max = PulseTokens.contentWidth,
                ).fillMaxWidth()
                .padding(top = LocalPulseContentPadding.current.calculateTopPadding())
                .imePadding(),
        ) {
            if (state.draft == null ||
                records
            ) {
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
            val content: @Composable () -> Unit = {
                val scroll = rememberScrollState()
                LaunchedEffect(state.subscriptionId) {
                    if (state.subscriptionId !=
                        null
                    ) {
                        scroll.scrollTo(0)
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(bottom = LocalPulseContentPadding.current.calculateBottomPadding())
                        .padding(width.pagePadding),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                ) {
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
                    if (state.draft != null && !records) {
                        if (state.overview.error !=
                            null
                        ) {
                            PulseErrorState(state.overview.error.message, vm::refreshOverview)
                        }
                        if (state.draft.kind == HorizonForm.Subscription) {
                            SubscriptionEditor(
                                state.draft,
                                state.overview.value,
                                state.busy,
                                state.formError,
                                vm::update,
                            )
                        } else if (state.draft.kind == HorizonForm.Budget) {
                            BudgetEditor(
                                state.draft,
                                state.busy,
                                state.formError,
                                LocalConfiguration.current.smallestScreenWidthDp >= 600 && width != PulseWindowSize.Compact,
                                !state.reviewRequired && !state.busy && state.overview.value != null,
                                vm::update,
                                vm::save,
                            )
                        } else if (state.draft.kind == HorizonForm.Category) {
                            CategoryEditor(
                                state.draft,
                                state.busy,
                                state.formError,
                                !state.reviewRequired && !state.busy && state.overview.value != null,
                                vm::update,
                                vm::save,
                            )
                        } else {
                            HorizonEditor(
                                state.draft,
                                state.overview.value,
                                state.busy,
                                state.formError,
                                state.reviewRequired,
                                vm::update,
                                vm::save,
                                cancel,
                            )
                        }
                        val actionDraft =
                            state.draft.takeIf {
                                it.kind in
                                    listOf(HorizonForm.Subscription, HorizonForm.Budget, HorizonForm.Category)
                            }
                        if (actionDraft != null) {
                            HorizonEditorActions(
                                stringResource(
                                    if (actionDraft.kind == HorizonForm.Category) {
                                        R.string.pulse_horizon_save_category
                                    } else if (actionDraft.kind == HorizonForm.Budget) {
                                        R.string.pulse_horizon_save_budget
                                    } else if (actionDraft.id == null) {
                                        R.string.pulse_horizon_add_subscription
                                    } else {
                                        R.string.pulse_horizon_obligation_save_changes
                                    },
                                ),
                                !state.reviewRequired && !state.busy && state.overview.value != null,
                                state.busy,
                                vm::save,
                                cancel,
                            )
                        }
                    } else {
                        if (state.draft != null &&
                            !state.reviewRequired
                        ) {
                            PulseButton(
                                stringResource(
                                    R.string.pulse_horizon_resume_unsaved_edits,
                                ),
                                {
                                    records =
                                        false
                                },
                                style = PulseButtonStyle.Text,
                            )
                        }
                        val canMutate =
                            !state.busy &&
                                !state.reviewRequired &&
                                state.overview.value != null &&
                                state.draft == null
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
                                LedgerFilters(
                                    state, vm, ledgerCategory, { ledgerCategory = it },
                                    canMutate = canMutate,
                                    addCategory = { create(HorizonForm.Category) },
                                    logTransaction = { create(HorizonForm.Transaction) },
                                )
                                SectionState(state.transactions, {
                                    vm.refreshTransactions()
                                }) { page ->
                                    if (currency.isBlank()) {
                                        PulseText(stringResource(R.string.pulse_horizon_currency_unavailable), style = PulseTextStyle.Label)
                                    }
                                    HorizonLedger(
                                        page,
                                        currency,
                                        canMutate,
                                        { edit(it.draft(currency)) },
                                        { tx ->
                                            remove(
                                                HorizonRecord.Transaction,
                                                tx.id,
                                                uiText(
                                                    R.string.pulse_horizon_delete_1_s_this_permanently_removes_the_transaction_and,
                                                    tx.name,
                                                ),
                                            )
                                        },
                                        categoryId = ledgerCategory,
                                    )
                                    PageControls(page, state.transactions.loading || state.busy, {
                                        vm.refreshTransactions(it)
                                    })
                                }
                            }
                            HorizonTab.Budgets -> {
                                HorizonSectionAction(R.string.pulse_horizon_budgets_heading, R.string.pulse_horizon_add_budget, canMutate) { create(HorizonForm.Budget) }
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
                                HorizonSectionAction(R.string.pulse_horizon_obligations_heading, R.string.pulse_horizon_add_obligation, canMutate) { create(HorizonForm.Deduction) }
                                SectionState(state.overview, vm::refreshOverview) { data ->
                                    if (data.deductions.isEmpty()) {
                                        PulseEmptyState(
                                            stringResource(
                                                R.string.pulse_horizon_no_fixed_obligations,
                                            ),
                                            stringResource(
                                                R.string.pulse_horizon_add_recurring_bills_investments_or_other_commitments,
                                            ),
                                        )
                                    }
                                    ObligationGrid(data.deductions) { item ->
                                        ObligationCard(
                                            item = item,
                                            budgetName = item.budgetId?.let { id ->
                                                data.budgets.find { it.id == id }?.name
                                                    ?: stringResource(R.string.pulse_horizon_unavailable)
                                            },
                                            enabled = canMutate,
                                            edit = {
                                                edit(HorizonDraft(
                                                    HorizonForm.Deduction, item.id, item.name,
                                                    item.amount.toPlainString(), currency,
                                                    category = item.category,
                                                    dueDay = item.dueDay?.toString().orEmpty(),
                                                    active = item.active, budgetId = item.budgetId,
                                                ))
                                            },
                                            delete = {
                                                remove(
                                                    HorizonRecord.Deduction, item.id,
                                                    uiText(R.string.pulse_horizon_delete_1_s_this_removes_the_fixed_obligation_and, item.name),
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                            HorizonTab.Subscriptions -> {
                                if (state.subscriptionId !=
                                    null
                                ) {
                                    SubscriptionPayments(state, vm, canMutate, currency) { id ->
                                        deletion =
                                            HorizonChange.LinkPayment(state.subscriptionId, id) to
                                            uiText(
                                                R.string.pulse_horizon_link_this_debit_to_the_subscription_the_transaction_remains,
                                            )
                                    }
                                }
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
                    }
                    if (state.busy) {
                        PulseLoadingState(
                            stringResource(R.string.pulse_horizon_saving_your_change),
                        )
                    }
                }
            }
            if (state.draft != null && !records) {
                key(state.draft.kind, state.draft.id) { content() }
            } else {
                scrollStates.SaveableStateProvider(state.tab.name) { content() }
            }
        }
    }
    if (discard) {
        PulseDialog(
            stringResource(R.string.pulse_horizon_discard_unsaved_changes),
            stringResource(R.string.pulse_horizon_your_edits_have_not_been_saved_leaving_this_form),
            stringResource(R.string.pulse_horizon_discard_edits),
            {
                discard = false
                records = false
                vm.discard()
            },
            { discard = false },
        )
    }
    deletion?.let { (change, body) ->
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
internal fun <T> SectionState(
    section: HorizonSection<T>,
    retry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    if (section.loading) {
        if (section.value == null) {
            PulseLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon))
        } else {
            PulseRefreshIndicator()
        }
    }
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}

@Composable
internal fun HorizonSectionAction(title: Int, action: Int, enabled: Boolean, onClick: () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
    ) {
        PulseText(stringResource(title), style = PulseTextStyle.Heading2)
        PulseButton(stringResource(action), onClick, enabled = enabled)
    }
}
