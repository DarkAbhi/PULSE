package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.MealChange
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message

@Composable
internal fun CustomMealTimes(
    state: MealPlanUiState,
    vm: MealPlanViewModel,
    enabled: Boolean,
) {
    var deleting by remember { mutableStateOf<MealTime?>(null) }
    if (state.reviewRequired && state.draft == null) {
        PulseCard(Modifier.fillMaxWidth()) {
            state.message?.let { PulseText(it) }
            PulseText(stringResource(R.string.pulse_mealplan_review_before_another_change), style = PulseTextStyle.Title)
            PulseButton(
                stringResource(R.string.pulse_mealplan_refresh_server_records),
                vm::refresh,
                enabled = !state.busy,
                style = PulseButtonStyle.Text,
            )
            PulseButton(
                stringResource(R.string.pulse_mealplan_i_reviewed_the_server_records),
                vm::reviewed,
                enabled = state.reviewReady && !state.busy,
            )
        }
    }
    if (state.draft == null) {
        PulseButton(
            stringResource(R.string.pulse_mealplan_add_custom_meal_time),
            { vm.edit(MealDraft(timeForm = true)) },
            Modifier.fillMaxWidth(),
            enabled = enabled && state.canWrite,
            variant = PulseButtonVariant.Secondary,
        )
    }
    state.times.error?.takeUnless { state.times.loading }?.let {
        PulseErrorState(it.message, vm::refreshTimes)
    }
    state.times.value.orEmpty().filterNot { it.isDefault }.forEach { time ->
        PulseCard(Modifier.fillMaxWidth()) {
            PulseText(time.name, style = PulseTextStyle.Title)
            PulseText(
                stringResource(
                    if (time.end < time.start) R.string.pulse_mealplan_overnight_time_range else R.string.pulse_mealplan_time_range,
                    time.start.format(mealClock),
                    time.end.format(mealClock),
                ),
            )
            if (state.draft == null) {
                PulseButton(
                    stringResource(R.string.pulse_mealplan_delete_time_1_s, time.name),
                    { deleting = time },
                    enabled = enabled && state.canWrite,
                    style = PulseButtonStyle.Text,
                )
            }
        }
    }
    deleting?.let { time ->
        PulseDialog(
            stringResource(R.string.pulse_mealplan_delete_time_1_s, time.name),
            stringResource(R.string.pulse_mealplan_delete_1_s_meals_remain_under_other_meals_with, time.name),
            stringResource(R.string.pulse_mealplan_delete),
            {
                deleting = null
                vm.mutate(MealChange.DeleteTime(time))
            },
            { deleting = null },
        )
    }
}
