package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.message

@Composable
internal fun HorizonOverviewTabScreen(
    section: HorizonSection<HorizonOverview>,
    canMutate: Boolean,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
) {
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, onRetry) }
    section.value?.let { data ->
        HorizonMetrics(data, canMutate) {
            onAdd()
        }
        ProjectionCard(data)
    }
}
