package dev.abhishekan.pulse.feature.horizon.presentation.subscriptions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.draft
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection

internal data class SubscriptionsTabState(
    val section: HorizonSection<HorizonOverview>,
    val canMutate: Boolean,
    val currency: String,
    val canViewPayments: Boolean,
)

internal sealed interface SubscriptionsTabAction {
    data object Retry : SubscriptionsTabAction

    data object Add : SubscriptionsTabAction

    data class Edit(
        val draft: HorizonDraft,
    ) : SubscriptionsTabAction

    data class RequestDelete(
        val id: Long,
        val name: String,
    ) : SubscriptionsTabAction

    data class ViewPayments(
        val id: Long,
    ) : SubscriptionsTabAction
}

@Composable
internal fun SubscriptionsTabScreen(
    state: SubscriptionsTabState,
    onAction: (SubscriptionsTabAction) -> Unit,
) {
    val section = state.section
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, { onAction(SubscriptionsTabAction.Retry) }) }
    section.value?.let { data ->
        SubscriptionList(
            data = data,
            canMutate = state.canMutate,
            canViewPayments = state.canViewPayments,
            add = { onAction(SubscriptionsTabAction.Add) },
            edit = { onAction(SubscriptionsTabAction.Edit(it.draft(state.currency))) },
            delete = { onAction(SubscriptionsTabAction.RequestDelete(it.id, it.name)) },
            payments = { onAction(SubscriptionsTabAction.ViewPayments(it.id)) },
        )
    }
}
