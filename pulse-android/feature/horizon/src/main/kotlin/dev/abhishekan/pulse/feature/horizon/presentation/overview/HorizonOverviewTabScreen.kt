package dev.abhishekan.pulse.feature.horizon.presentation.overview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection

internal data class HorizonOverviewTabState(
    val section: HorizonSection<HorizonOverview>,
    val canMutate: Boolean,
)

internal sealed interface HorizonOverviewTabAction {
    data object Retry : HorizonOverviewTabAction

    data object EditSettings : HorizonOverviewTabAction
}

@Composable
internal fun HorizonOverviewTabScreen(
    state: HorizonOverviewTabState,
    onAction: (HorizonOverviewTabAction) -> Unit,
) {
    val section = state.section
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, { onAction(HorizonOverviewTabAction.Retry) }) }
    section.value?.let { data ->
        HorizonMetrics(data, state.canMutate) {
            onAction(HorizonOverviewTabAction.EditSettings)
        }
        ProjectionCard(data)
    }
}
