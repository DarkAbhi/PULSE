package dev.abhishekan.pulse.feature.mealplan.planner

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.mealplan.R

@Composable
internal fun MealChangeReview(
    state: MealPlanUiState,
    onAction: (MealPlanAction) -> Unit,
    showExplanation: Boolean,
    draftAction: @Composable () -> Unit = {},
) {
    PulseCard(Modifier.fillMaxWidth()) {
        (state.write as? MealWriteState.NeedsReview)?.failure?.let {
            PulseText(uiText(R.string.pulse_mealplan_uncertain_change, it.message))
        }
        PulseText(stringResource(R.string.pulse_mealplan_review_before_another_change), style = PulseTextStyle.Title)
        if (showExplanation) {
            PulseText(stringResource(R.string.pulse_mealplan_a_previous_update_may_have_reached_the_server_check))
        }
        PulseButton(
            stringResource(R.string.pulse_mealplan_refresh_server_records),
            { onAction(MealPlanAction.Refresh) },
            enabled = !state.busy,
            style = PulseButtonStyle.Text,
        )
        draftAction()
        PulseButton(
            stringResource(R.string.pulse_mealplan_i_reviewed_the_server_records),
            { onAction(MealPlanAction.Reviewed) },
            enabled = state.reviewReady,
        )
    }
}
