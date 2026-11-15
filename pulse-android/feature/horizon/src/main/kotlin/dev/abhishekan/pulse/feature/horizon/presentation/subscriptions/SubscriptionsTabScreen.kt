package dev.abhishekan.pulse.feature.horizon.presentation.subscriptions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.draft
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection

@Composable
internal fun SubscriptionsTabScreen(
    section: HorizonSection<HorizonOverview>,
    canMutate: Boolean,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    currency: String,
    canViewPayments: Boolean,
    onEdit: (HorizonDraft) -> Unit,
    onConfirm: (HorizonChange, UiText) -> Unit,
    onViewPayments: (Long) -> Unit,
) {
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, onRetry) }
    section.value?.let { data ->
        SubscriptionList(
            data = data,
            canMutate = canMutate,
            canViewPayments = canViewPayments,
            add = onAdd,
            edit = { onEdit(it.draft(currency)) },
            delete = { item ->
                onConfirm(
                    HorizonChange.Delete(HorizonRecord.Subscription, item.id),
                    uiText(
                        R.string.pulse_horizon_delete_subscription_confirmation,
                        item.name,
                    ),
                )
            },
            payments = { onViewPayments(it.id) },
        )
    }
}
