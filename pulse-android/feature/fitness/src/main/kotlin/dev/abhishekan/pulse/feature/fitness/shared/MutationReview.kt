package dev.abhishekan.pulse.feature.fitness.shared

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R

@Composable
internal fun MutationReview(
    ready: Boolean,
    busy: Boolean,
    onRefresh: () -> Unit,
    onReviewed: () -> Unit,
    message: UiText? = null,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        message?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        PulseText(
            stringResource(R.string.pulse_fitness_check_before_trying_again),
            style = PulseTextStyle.Title,
        )
        PulseText(
            if (ready) {
                stringResource(
                    R.string.pulse_fitness_the_latest_server_records_are_shown_below_review_them,
                )
            } else {
                stringResource(
                    R.string.pulse_fitness_the_change_may_already_have_reached_pulse_refresh_server,
                )
            },
        )
        if (ready) {
            PulseButton(
                stringResource(R.string.pulse_fitness_i_ve_reviewed_the_latest_records),
                onReviewed,
                enabled = !busy,
            )
        } else {
            PulseButton(
                stringResource(R.string.pulse_fitness_refresh_server_records),
                onRefresh,
                enabled = !busy,
            )
        }
    }
}
