package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
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
    flat: Boolean = false,
) {
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var timePicker by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val content: @Composable ColumnScope.() -> Unit = {
        MealEditorHeader(
            if (draft.timeForm) {
                stringResource(R.string.pulse_mealplan_new_meal_time)
            } else if (draft.id ==
                null
            ) {
                stringResource(R.string.pulse_mealplan_plan_a_meal)
            } else {
                stringResource(R.string.pulse_mealplan_edit_meal)
            },
            illustrated = !draft.timeForm,
        )
        Spacer(Modifier.height(8.dp))
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
            trailingIcon = { Icon(painterResource(R.drawable.meal_edit), null, Modifier.size(20.dp)) },
        )
        if (!draft.timeForm && draft.name.isEmpty()) {
            PulseText(
                stringResource(R.string.pulse_mealplan_name_example),
                style = PulseTextStyle.Body3,
                color = PulseTheme.colors.textSecondary,
            )
        }
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
            Spacer(Modifier.height(8.dp))
            PulseText(stringResource(R.string.pulse_mealplan_meal_date), style = PulseTextStyle.Label)
            val formattedDate = runCatching { LocalDate.parse(draft.date).format(mealDate) }.getOrDefault(draft.date)
            val dateLabel = stringResource(R.string.pulse_mealplan_meal_date_1_s, formattedDate)
            Surface(
                onClick = {
                    focus.clearFocus()
                    datePicker = true
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = PulseTokens.target)
                        .testTag("meal-date")
                        .semantics { contentDescription = dateLabel },
                enabled = !busy,
                shape = PulseTheme.shapes.large,
                color = PulseTheme.colors.surfaceMuted.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, PulseTheme.colors.border),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.meal_date), null, Modifier.size(24.dp))
                    PulseText(
                        formattedDate,
                        Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            PulseText(stringResource(R.string.pulse_mealplan_meal_time), style = PulseTextStyle.Label)
            MealTimeChips(times, draft.timeId, !busy) {
                focus.clearFocus()
                onUpdate(draft.copy(timeId = it.id))
            }
            val choices =
                buildList {
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
                    ?: stringResource(R.string.pulse_mealplan_select_meal_time),
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
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PulseButton(stringResource(R.string.pulse_mealplan_cancel), {
                focus.clearFocus()
                onCancel()
            }, Modifier.widthIn(min = 160.dp).weight(1f), enabled = !busy, variant = PulseButtonVariant.Secondary)
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
                Modifier.widthIn(min = 160.dp).weight(1f),
                enabled = !review,
                loading = busy,
            )
        }
    }
    val modifier = Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()
    if (flat) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.small), content = content)
    } else {
        PulseCard(modifier, content = content)
    }
    if (datePicker) {
        PulseDatePickerDialog(
            runCatching {
                LocalDate.parse(draft.date)
            }.getOrDefault(LocalDate.now(UserTimeZone.zone)),
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
