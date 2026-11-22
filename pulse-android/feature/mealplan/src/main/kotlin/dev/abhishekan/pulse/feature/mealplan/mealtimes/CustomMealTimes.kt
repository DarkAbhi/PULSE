package dev.abhishekan.pulse.feature.mealplan.mealtimes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.feature.mealplan.R
import dev.abhishekan.pulse.feature.mealplan.shared.mealClock

@Composable
internal fun CustomMealTimes(
    times: List<MealTime>,
    failure: Failure?,
    onRetry: () -> Unit,
    onAdd: (() -> Unit)? = null,
    onDelete: ((MealTime) -> Unit)? = null,
    enabled: Boolean = true,
) {
    var deleting by remember { mutableStateOf<MealTime?>(null) }
    if (onAdd != null) {
        PulseButton(
            stringResource(R.string.pulse_mealplan_add_custom_meal_time),
            onAdd,
            Modifier.fillMaxWidth(),
            enabled = enabled,
            variant = PulseButtonVariant.Secondary,
        )
    }
    failure?.let {
        PulseErrorState(it.message, onRetry)
    }
    times.filterNot { it.isDefault }.forEach { time ->
        PulseCard(Modifier.fillMaxWidth()) {
            PulseText(time.name, style = PulseTextStyle.Title)
            PulseText(
                stringResource(
                    if (time.end < time.start) R.string.pulse_mealplan_overnight_time_range else R.string.pulse_mealplan_time_range,
                    time.start.format(mealClock),
                    time.end.format(mealClock),
                ),
            )
            if (onDelete != null) {
                PulseButton(
                    stringResource(R.string.pulse_mealplan_delete_time_1_s, time.name),
                    { deleting = time },
                    enabled = enabled,
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
                if (enabled) onDelete?.invoke(time)
            },
            { deleting = null },
        )
    }
}
