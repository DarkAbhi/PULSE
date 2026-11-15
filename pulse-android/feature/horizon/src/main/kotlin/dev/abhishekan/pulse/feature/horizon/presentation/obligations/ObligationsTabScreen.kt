package dev.abhishekan.pulse.feature.horizon.presentation.obligations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.draft
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection

internal data class ObligationsTabState(
    val section: HorizonSection<HorizonOverview>,
    val canMutate: Boolean,
    val currency: String,
)

internal sealed interface ObligationsTabAction {
    data object Retry : ObligationsTabAction

    data object Add : ObligationsTabAction

    data class Edit(
        val draft: HorizonDraft,
    ) : ObligationsTabAction

    data class RequestDelete(
        val id: Long,
        val name: String,
    ) : ObligationsTabAction
}

@Composable
internal fun ObligationsTabScreen(
    state: ObligationsTabState,
    onAction: (ObligationsTabAction) -> Unit,
) {
    val section = state.section
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
    ) {
        PulseText(stringResource(R.string.pulse_horizon_obligations_heading), style = PulseTextStyle.Heading2)
        PulseButton(
            stringResource(R.string.pulse_horizon_add_obligation),
            { onAction(ObligationsTabAction.Add) },
            enabled = state.canMutate,
        )
    }
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, { onAction(ObligationsTabAction.Retry) }) }
    section.value?.let { data ->
        val activeSubscriptions = remember(data.subscriptions) { data.subscriptions.filter { it.status == "active" } }
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
                enabled = state.canMutate,
                edit = {
                    onAction(
                        ObligationsTabAction.Edit(
                            HorizonDraft(
                                HorizonForm.Deduction,
                                item.id,
                                item.name,
                                item.amount.toPlainString(),
                                state.currency,
                                category = item.category,
                                dueDay = item.dueDay?.toString().orEmpty(),
                                active = item.active,
                                budgetId = item.budgetId,
                            ),
                        ),
                    )
                },
                delete = { onAction(ObligationsTabAction.RequestDelete(item.id, item.name)) },
            )
        }
        ObligationGrid(activeSubscriptions, { it.id }) { item ->
            SubscriptionObligationCard(
                item = item,
                budgetName = item.budgetId?.let { id -> data.budgets.find { it.id == id }?.name },
                categoryName = item.categoryId?.let { id -> data.categories.find { it.id == id }?.name },
                enabled = state.canMutate,
                manage = { onAction(ObligationsTabAction.Edit(item.draft(state.currency))) },
            )
        }
    }
}
