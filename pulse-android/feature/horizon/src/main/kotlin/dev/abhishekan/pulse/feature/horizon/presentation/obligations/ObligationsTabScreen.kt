package dev.abhishekan.pulse.feature.horizon.presentation.obligations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.draft
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection

@Composable
internal fun ObligationsTabScreen(
    section: HorizonSection<HorizonOverview>,
    canMutate: Boolean,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    currency: String,
    onEdit: (HorizonDraft) -> Unit,
    onConfirm: (HorizonChange, UiText) -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
    ) {
        PulseText(stringResource(R.string.pulse_horizon_obligations_heading), style = PulseTextStyle.Heading2)
        PulseButton(stringResource(R.string.pulse_horizon_add_obligation), onAdd, enabled = canMutate)
    }
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, onRetry) }
    section.value?.let { data ->
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
                    onEdit(
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
                    onConfirm(
                        HorizonChange.Delete(HorizonRecord.Deduction, item.id),
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
                manage = { onEdit(item.draft(currency)) },
            )
        }
    }
}
