package dev.abhishekan.pulse.feature.mealplan
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun MealEditor(
    draft: MealDraft,
    times: List<MealTime>,
    busy: Boolean,
    review: Boolean,
    error: UiText?,
    onUpdate: (MealDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var timePicker by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        PulseText(
            if (draft.timeForm) {
                stringResource(R.string.pulse_mealplan_new_meal_time)
            } else if (draft.id ==
                null
            ) {
                stringResource(R.string.pulse_mealplan_plan_a_meal)
            } else {
                stringResource(R.string.pulse_mealplan_edit_meal)
            },
            style = PulseTextStyle.Title,
        )
        PulseTextField(
            draft.name,
            {
                onUpdate(draft.copy(name = it))
            },
            if (draft.timeForm) {
                stringResource(
                    R.string.pulse_mealplan_meal_time_name,
                )
            } else {
                stringResource(R.string.pulse_mealplan_meal_name)
            },
            Modifier.fillMaxWidth(),
            enabled = !busy,
            error = error,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(onDone = {
                    focus.clearFocus()
                    onSave()
                }),
        )
        if (draft.timeForm) {
            PulseTextButton(stringResource(R.string.pulse_mealplan_start_time_1_s, draft.start), {
                focus.clearFocus()
                timePicker =
                    "start"
            }, enabled = !busy)
            PulseTextButton(stringResource(R.string.pulse_mealplan_end_time_1_s, draft.end), {
                focus.clearFocus()
                timePicker =
                    "end"
            }, enabled = !busy)
            PulseText(
                stringResource(R.string.pulse_mealplan_use_24_hour_times_an_end_earlier_than_the),
            )
        } else {
            PulseTextButton(stringResource(R.string.pulse_mealplan_meal_date_1_s, draft.date), {
                focus.clearFocus()
                datePicker =
                    true
            }, enabled = !busy)
            val choices =
                buildList {
                    if (draft.originalTimeId ==
                        null
                    ) {
                        add("" to stringResource(R.string.pulse_mealplan_no_meal_time))
                    }
                    if (draft.timeId != null &&
                        times.none { it.id == draft.timeId }
                    ) {
                        add(
                            draft.timeId.toString() to
                                stringResource(R.string.pulse_mealplan_previously_assigned_meal_time),
                        )
                    }
                    addAll(
                        times.map {
                            it.id.toString() to
                                stringResource(
                                    R.string.pulse_mealplan_time_option,
                                    it.name,
                                    it.start.format(mealClock),
                                    it.end.format(mealClock),
                                )
                        },
                    )
                }
            PulseSelectionField(
                stringResource(R.string.pulse_mealplan_meal_time),
                choices.firstOrNull { it.first == draft.timeId?.toString().orEmpty() }?.second
                    ?: stringResource(R.string.pulse_mealplan_no_meal_time),
                choices.map {
                    it.second
                },
                {
                    onUpdate(draft.copy(timeId = choices[it].first.toLongOrNull()))
                },
                enabled = !busy,
            )
            if (draft.originalTimeId !=
                null
            ) {
                PulseText(
                    stringResource(
                        R.string.pulse_mealplan_you_can_change_the_assigned_time_removing_it_from,
                    ),
                )
            }
        }
        PulseButton(
            if (draft.timeForm) {
                stringResource(
                    R.string.pulse_mealplan_save_meal_time,
                )
            } else {
                stringResource(R.string.pulse_mealplan_save_meal)
            },
            {
                focus.clearFocus()
                onSave()
            },
            Modifier.fillMaxWidth(),
            enabled = !review,
            loading = busy,
        )
        PulseTextButton(stringResource(R.string.pulse_mealplan_cancel), {
            focus.clearFocus()
            onCancel()
        }, enabled = !busy)
    }
    if (datePicker) {
        PulseDatePickerDialog(
            runCatching {
                LocalDate.parse(draft.date)
            }.getOrDefault(LocalDate.now()),
            {
                onUpdate(draft.copy(date = it.toString()))
                datePicker = false
            },
            { datePicker = false },
        )
    }
    timePicker?.let { which ->
        PulseTimePickerDialog(LocalTime.parse(if (which == "start") draft.start else draft.end), {
            onUpdate(
                if (which ==
                    "start"
                ) {
                    draft.copy(start = it.format(mealClock))
                } else {
                    draft.copy(end = it.format(mealClock))
                },
            )
            timePicker =
                null
        }, { timePicker = null })
    }
}
