package dev.abhishekan.pulse.feature.horizon.presentation.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonBudget
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseEmptyState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection

internal data class BudgetsTabState(
    val section: HorizonSection<List<HorizonBudget>>,
    val currency: String,
    val canMutate: Boolean,
)

internal sealed interface BudgetsTabAction {
    data object Retry : BudgetsTabAction

    data object Add : BudgetsTabAction

    data class Edit(
        val budget: HorizonBudget,
    ) : BudgetsTabAction

    data class Delete(
        val budget: HorizonBudget,
    ) : BudgetsTabAction
}

@Composable
internal fun BudgetsTabScreen(
    state: BudgetsTabState,
    onAction: (BudgetsTabAction) -> Unit,
) {
    val section = state.section
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
    ) {
        PulseText(stringResource(R.string.pulse_horizon_budgets_heading), style = PulseTextStyle.Heading2)
        PulseButton(
            stringResource(R.string.pulse_horizon_add_budget),
            { onAction(BudgetsTabAction.Add) },
            enabled = state.canMutate,
        )
    }
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, { onAction(BudgetsTabAction.Retry) }) }
    section.value?.let { budgets ->
        if (budgets.isEmpty()) {
            PulseEmptyState(
                stringResource(R.string.pulse_horizon_no_budgets_yet),
                stringResource(
                    R.string.pulse_horizon_create_a_budget_to_group_related_spending_and_obligations,
                ),
            )
        }
        BudgetGrid(budgets) { budget ->
            BudgetCard(
                budget,
                state.currency,
                state.canMutate,
                { onAction(BudgetsTabAction.Edit(budget)) },
                { onAction(BudgetsTabAction.Delete(budget)) },
            )
        }
    }
}
