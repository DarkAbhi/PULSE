package dev.abhishekan.pulse.feature.horizon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseRefreshIndicator
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel

@Composable
fun HorizonRoute(
    onDraftChanged: (Boolean) -> Unit,
    viewModel: HorizonViewModel = koinViewModel(),
) {
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(timeZone) { viewModel.refresh() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseSnackbarEffect(state.message.takeUnless { state.reviewRequired }, viewModel::consumeMessage)
    LaunchedEffect(state.draft?.dirty) { onDraftChanged(state.draft?.dirty == true) }
    androidx.compose.runtime.key(timeZone) {
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
    var deletion by remember { mutableStateOf<Pair<HorizonChange, UiText>?>(null) }
    val cancel = { if (state.draft?.dirty == true) discard = true else vm.discard() }
    BackHandler(state.draft != null || state.busy || state.subscriptionId != null) {
        when {
            state.busy -> Unit
            state.draft != null -> cancel()
            else -> vm.hidePayments()
        }
    }
    val currency =
        state.overview.value
            ?.currency
            .orEmpty()
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
                            PulseTextButton(
                                stringResource(R.string.pulse_horizon_refresh_server_records),
                                vm::refresh,
                            )
                            if (state.draft !=
                                null
                            ) {
                                PulseTextButton(
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
                    } else {
                        if (state.draft != null &&
                            !state.reviewRequired
                        ) {
                            PulseTextButton(
                                stringResource(
                                    R.string.pulse_horizon_resume_unsaved_edits,
                                ),
                                {
                                    records =
                                        false
                                },
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
                                PulseText(
                                    stringResource(R.string.pulse_horizon_your_room_to_move),
                                    Modifier.semantics {
                                        heading()
                                    },
                                    PulseTextStyle.Display,
                                )
                                SectionState(state.overview, vm::refreshOverview) { data ->
                                    if (width != PulseWindowSize.Compact &&
                                        LocalDensity.current.fontScale <= 1.3f
                                    ) {
                                        Row(
                                            horizontalArrangement =
                                                Arrangement.spacedBy(
                                                    PulseTokens.cardPadding,
                                                ),
                                        ) {
                                            Column(Modifier.weight(1.2f)) {
                                                OverviewCard(data, canMutate) {
                                                    create(HorizonForm.Config)
                                                }
                                            }
                                            Column(
                                                Modifier.weight(1f),
                                                verticalArrangement =
                                                    Arrangement.spacedBy(
                                                        PulseTokens.cardPadding,
                                                    ),
                                            ) {
                                                CommitmentCard(data)
                                                ProjectionCard(data)
                                            }
                                        }
                                    } else {
                                        OverviewCard(data, canMutate) {
                                            create(HorizonForm.Config)
                                        }
                                        CommitmentCard(data)
                                        ProjectionCard(data)
                                    }
                                }
                            }
                            HorizonTab.Transactions -> {
                                PulseText(
                                    stringResource(R.string.pulse_horizon_the_everyday_ledger),
                                    style = PulseTextStyle.Display,
                                )
                                TransactionFilters(state, vm)
                                PulseButton(
                                    stringResource(
                                        R.string.pulse_horizon_add_transaction,
                                    ),
                                    {
                                        create(HorizonForm.Transaction)
                                    },
                                    enabled = canMutate,
                                )
                                PulseTextButton(
                                    stringResource(
                                        R.string.pulse_horizon_create_category,
                                    ),
                                    {
                                        create(HorizonForm.Category)
                                    },
                                    enabled = canMutate,
                                )
                                SectionState(state.transactions, {
                                    vm.refreshTransactions()
                                }) { page ->
                                    if (page.items.isEmpty()) {
                                        PulseEmptyState(
                                            stringResource(R.string.pulse_horizon_no_transactions),
                                            stringResource(
                                                R.string.pulse_horizon_try_another_filter_or_record_your_first_transaction,
                                            ),
                                        )
                                    }
                                    page.items.forEach { tx ->
                                        TransactionCard(
                                            tx,
                                            currency,
                                            canMutate,
                                            {
                                                edit(tx.draft(currency))
                                            },
                                            {
                                                remove(
                                                    HorizonRecord.Transaction,
                                                    tx.id,
                                                    uiText(
                                                        R.string.pulse_horizon_delete_1_s_this_permanently_removes_the_transaction_and,
                                                        tx.name,
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                    PageControls(page, state.transactions.loading || state.busy, {
                                        vm.refreshTransactions(it)
                                    })
                                }
                            }
                            HorizonTab.Budgets -> {
                                PulseText(
                                    stringResource(R.string.pulse_horizon_give_every_priority_room),
                                    style = PulseTextStyle.Display,
                                )
                                PulseButton(stringResource(R.string.pulse_horizon_add_budget), {
                                    create(HorizonForm.Budget)
                                }, enabled = canMutate)
                                SectionState(state.overview, vm::refreshOverview) { data ->
                                    if (data.budgets.isEmpty()) {
                                        PulseEmptyState(
                                            stringResource(R.string.pulse_horizon_no_budgets_yet),
                                            stringResource(
                                                R.string.pulse_horizon_create_a_budget_to_group_related_spending_and_obligations,
                                            ),
                                        )
                                    }
                                    data.budgets.forEach { budget ->
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
                                PulseText(
                                    stringResource(
                                        R.string.pulse_horizon_make_room_for_the_essentials,
                                    ),
                                    style = PulseTextStyle.Display,
                                )
                                PulseButton(stringResource(R.string.pulse_horizon_add_obligation), {
                                    create(HorizonForm.Deduction)
                                }, enabled = canMutate)
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
                                    data.deductions.forEach { item ->
                                        PulseCard(
                                            Modifier.fillMaxWidth().testTag(
                                                "horizon-record-${item.id}",
                                            ),
                                        ) {
                                            PulseText(item.name, style = PulseTextStyle.Title)
                                            PulseText(
                                                money(currency, item.amount).asString(),
                                                style = PulseTextStyle.Metric,
                                            )
                                            PulseText(
                                                stringResource(
                                                    if (item.dueDay ==
                                                        null
                                                    ) {
                                                        R.string.pulse_horizon_obligation_status
                                                    } else {
                                                        R.string.pulse_horizon_obligation_status_with_due_day
                                                    },
                                                    horizonOption(item.category),
                                                    if (item.active) {
                                                        stringResource(
                                                            R.string.pulse_horizon_active,
                                                        )
                                                    } else {
                                                        stringResource(
                                                            R.string.pulse_horizon_inactive,
                                                        )
                                                    },
                                                    item.dueDay ?: 0,
                                                ),
                                            )
                                            item.budgetId?.let { id ->
                                                PulseText(
                                                    stringResource(
                                                        R.string.pulse_horizon_budget_1_s,
                                                        data.budgets.find { it.id == id }?.name
                                                            ?: stringResource(
                                                                R.string.pulse_horizon_unavailable,
                                                            ),
                                                    ),
                                                )
                                            }
                                            Actions(
                                                item.name,
                                                canMutate,
                                                {
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
                                                {
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
                                    }
                                }
                            }
                            HorizonTab.Subscriptions -> {
                                PulseText(
                                    stringResource(R.string.pulse_horizon_know_what_keeps_renewing),
                                    style = PulseTextStyle.Display,
                                )
                                PulseButton(
                                    stringResource(
                                        R.string.pulse_horizon_add_subscription,
                                    ),
                                    {
                                        create(HorizonForm.Subscription)
                                    },
                                    enabled = canMutate,
                                )
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
                                    if (data.subscriptions.isEmpty()) {
                                        PulseEmptyState(
                                            stringResource(R.string.pulse_horizon_no_subscriptions),
                                            stringResource(
                                                R.string.pulse_horizon_add_a_monthly_or_yearly_subscription_to_track_its,
                                            ),
                                        )
                                    }
                                    data.subscriptions.forEach { item ->
                                        PulseCard(
                                            Modifier.fillMaxWidth().testTag(
                                                "horizon-record-${item.id}",
                                            ),
                                        ) {
                                            PulseText(item.name, style = PulseTextStyle.Title)
                                            PulseText(
                                                money(currency, item.amount).asString(),
                                                style = PulseTextStyle.Metric,
                                            )
                                            PulseText(
                                                stringResource(
                                                    R.string.pulse_horizon_subscription_status,
                                                    horizonOption(item.cycle),
                                                    horizonOption(item.status),
                                                ),
                                            )
                                            PulseText(
                                                stringResource(
                                                    R.string.pulse_horizon_monthly_equivalent_1_s,
                                                    money(currency, item.monthlyAmount).asString(),
                                                ),
                                            )
                                            item.nextRenewal?.let {
                                                PulseText(
                                                    stringResource(
                                                        R.string.pulse_horizon_next_renewal_1_s,
                                                        it,
                                                    ),
                                                )
                                            }
                                            item.billingDay?.let {
                                                PulseText(
                                                    stringResource(
                                                        R.string.pulse_horizon_billing_day_1_s,
                                                        it,
                                                    ),
                                                )
                                            }
                                            PulseText(
                                                pluralStringResource(
                                                    R.plurals.pulse_horizon_linked_payments,
                                                    item.linkedCount,
                                                    item.linkedCount,
                                                    money(currency, item.totalSpent).asString(),
                                                ),
                                            )
                                            item.notes?.let { PulseText(it) }
                                            Actions(
                                                item.name,
                                                canMutate,
                                                {
                                                    edit(item.draft(currency))
                                                },
                                                {
                                                    remove(
                                                        HorizonRecord.Subscription,
                                                        item.id,
                                                        uiText(
                                                            R.string.pulse_horizon_delete_subscription_confirmation,
                                                            item.name,
                                                        ),
                                                    )
                                                },
                                            )
                                            PulseTextButton(
                                                stringResource(
                                                    R.string.pulse_horizon_view_payments_for_1_s,
                                                    item.name,
                                                ),
                                                {
                                                    vm.showPayments(item.id)
                                                },
                                                enabled = !state.busy,
                                            )
                                        }
                                    }
                                }
                            }
                            HorizonTab.Purchases -> {
                                PulseText(
                                    stringResource(R.string.pulse_horizon_a_little_further_ahead),
                                    style = PulseTextStyle.Display,
                                )
                                PulseText(
                                    stringResource(
                                        R.string.pulse_horizon_next_month_s_planned_purchases,
                                    ),
                                )
                                PulseButton(
                                    stringResource(
                                        R.string.pulse_horizon_add_planned_purchase,
                                    ),
                                    {
                                        create(HorizonForm.Purchase)
                                    },
                                    enabled = canMutate,
                                )
                                SectionState(state.purchases, vm::refreshPurchases) { list ->
                                    if (currency.isBlank()) {
                                        PulseText(
                                            stringResource(
                                                R.string.pulse_horizon_currency_unavailable,
                                            ),
                                            style = PulseTextStyle.Label,
                                        )
                                    }
                                    PulseText(
                                        stringResource(
                                            R.string.pulse_horizon_planned_total_1_s,
                                            money(currency, list.total).asString(),
                                        ),
                                        style = PulseTextStyle.Title,
                                    )
                                    if (list.items.isEmpty()) {
                                        PulseEmptyState(
                                            stringResource(
                                                R.string.pulse_horizon_nothing_planned_yet,
                                            ),
                                            stringResource(
                                                R.string.pulse_horizon_add_something_you_re_setting_aside_for_next_month,
                                            ),
                                        )
                                    }
                                    list.items.forEach { item ->
                                        PulseCard(Modifier.fillMaxWidth()) {
                                            PulseText(item.name, style = PulseTextStyle.Title)
                                            PulseText(
                                                money(currency, item.price).asString(),
                                                style = PulseTextStyle.Metric,
                                            )
                                            if (currency.isBlank()) {
                                                PulseText(
                                                    stringResource(
                                                        R.string.pulse_horizon_currency_unavailable,
                                                    ),
                                                    style = PulseTextStyle.Label,
                                                )
                                            }
                                            item.url?.let { PulseText(it) }
                                            PulseTextButton(
                                                stringResource(
                                                    R.string.pulse_horizon_delete_1_s,
                                                    item.name,
                                                ),
                                                {
                                                    remove(
                                                        HorizonRecord.Purchase,
                                                        item.id,
                                                        uiText(
                                                            R.string.pulse_horizon_remove_1_s_from_next_month_s_planned_purchases,
                                                            item.name,
                                                        ),
                                                    )
                                                },
                                                enabled = canMutate,
                                            )
                                        }
                                    }
                                    if (list.items.isNotEmpty()) {
                                        PulseTextButton(
                                            stringResource(
                                                R.string.pulse_horizon_clear_planned_purchases,
                                            ),
                                            {
                                                deletion =
                                                    HorizonChange.ClearPurchases to
                                                    uiText(
                                                        R.string.pulse_horizon_permanently_remove_every_purchase_in_next_month_s_list,
                                                    )
                                            },
                                            enabled = canMutate,
                                        )
                                    }
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
                vm.mutate(change)
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
