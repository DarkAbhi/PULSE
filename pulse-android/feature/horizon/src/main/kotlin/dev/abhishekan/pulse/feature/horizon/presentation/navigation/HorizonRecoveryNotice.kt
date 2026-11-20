package dev.abhishekan.pulse.feature.horizon.presentation.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection

@Composable
internal fun HorizonRecoveryNotice(
    hasDraft: Boolean,
    showingRecords: Boolean,
    reviewReady: Boolean,
    onRefresh: () -> Unit,
    onToggleRecords: () -> Unit,
    onAcknowledge: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(stringResource(R.string.pulse_horizon_review_before_another_change), style = PulseTextStyle.Title)
        PulseText(stringResource(R.string.pulse_horizon_the_server_may_have_saved_the_last_change_check))
        PulseButton(stringResource(R.string.pulse_horizon_refresh_server_records), onRefresh, style = PulseButtonStyle.Text)
        if (hasDraft) {
            PulseButton(
                stringResource(
                    if (showingRecords) R.string.pulse_horizon_return_to_draft else R.string.pulse_horizon_view_server_records,
                ),
                onToggleRecords,
                style = PulseButtonStyle.Text,
            )
        }
        PulseButton(stringResource(R.string.pulse_horizon_i_reviewed_the_server_records), onAcknowledge, enabled = reviewReady)
    }
}

@Composable
internal fun HorizonCategoryRecords(
    section: HorizonSection<HorizonOverview>,
    onRetry: () -> Unit,
) {
    if (section.loading && section.value == null) {
        PulseCenteredLoadingState(stringResource(R.string.pulse_horizon_loading_your_horizon), Modifier.fillMaxWidth())
    }
    section.error?.let { PulseErrorState(it.message, onRetry) }
    section.value?.let { overview ->
        PulseText(stringResource(R.string.pulse_horizon_filter_categories), style = PulseTextStyle.Title)
        overview.categories.forEach { PulseText(it.displayName) }
    }
}
