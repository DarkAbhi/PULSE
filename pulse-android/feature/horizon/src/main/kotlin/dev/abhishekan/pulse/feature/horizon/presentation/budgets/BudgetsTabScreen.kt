package dev.abhishekan.pulse.feature.horizon.presentation.budgets

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
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection

@Composable
internal fun BudgetsTabScreen(
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
        PulseText(stringResource(R.string.pulse_horizon_budgets_heading), style = PulseTextStyle.Heading2)
        PulseButton(stringResource(R.string.pulse_horizon_add_budget), onAdd, enabled = canMutate)
    }
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, onRetry) }
    section.value?.let { data ->
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
                    onEdit(
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
                    onConfirm(
                        HorizonChange.Delete(HorizonRecord.Budget, budget.id),
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
