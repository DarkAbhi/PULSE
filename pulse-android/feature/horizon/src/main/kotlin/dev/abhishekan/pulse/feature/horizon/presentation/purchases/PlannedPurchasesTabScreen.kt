package dev.abhishekan.pulse.feature.horizon.presentation.purchases

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.domain.model.PlannedPurchases
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection

@Composable
internal fun PlannedPurchasesTabScreen(
    section: HorizonSection<PlannedPurchases>,
    currency: String,
    canMutate: Boolean,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    onConfirm: (HorizonChange, UiText) -> Unit,
) {
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, onRetry) }
    section.value?.let { list ->
        PlannedPurchaseList(
            purchases = list,
            currency = currency,
            enabled = canMutate,
            add = onAdd,
            delete = { item ->
                onConfirm(
                    HorizonChange.Delete(HorizonRecord.Purchase, item.id),
                    uiText(R.string.pulse_horizon_remove_1_s_from_next_month_s_planned_purchases, item.name),
                )
            },
            clear = {
                onConfirm(
                    HorizonChange.ClearPurchases,
                    uiText(R.string.pulse_horizon_permanently_remove_every_purchase_in_next_month_s_list),
                )
            },
        )
    }
}
