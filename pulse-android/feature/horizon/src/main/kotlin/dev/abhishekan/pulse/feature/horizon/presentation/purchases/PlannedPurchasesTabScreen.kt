package dev.abhishekan.pulse.feature.horizon.presentation.purchases

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.PlannedPurchase
import dev.abhishekan.pulse.core.domain.model.PlannedPurchases
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection

internal data class PlannedPurchasesTabState(
    val section: HorizonSection<PlannedPurchases>,
    val currency: String,
    val canMutate: Boolean,
)

internal sealed interface PlannedPurchasesTabAction {
    data object Retry : PlannedPurchasesTabAction

    data object Add : PlannedPurchasesTabAction

    data class Delete(
        val purchase: PlannedPurchase,
    ) : PlannedPurchasesTabAction

    data object Clear : PlannedPurchasesTabAction
}

@Composable
internal fun PlannedPurchasesTabScreen(
    state: PlannedPurchasesTabState,
    onAction: (PlannedPurchasesTabAction) -> Unit,
) {
    val section = state.section
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, { onAction(PlannedPurchasesTabAction.Retry) }) }
    section.value?.let { list ->
        PlannedPurchaseList(
            purchases = list,
            currency = state.currency,
            enabled = state.canMutate,
            add = { onAction(PlannedPurchasesTabAction.Add) },
            delete = { onAction(PlannedPurchasesTabAction.Delete(it)) },
            clear = { onAction(PlannedPurchasesTabAction.Clear) },
        )
    }
}
