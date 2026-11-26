package dev.abhishekan.pulse.feature.fitness.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.WorkoutClassification
import dev.abhishekan.pulse.core.domain.model.WorkoutType
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable
internal fun WorkoutClassificationDialog(
    title: String,
    initial: WorkoutClassification,
    allowRest: Boolean,
    onSave: (WorkoutClassification) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(null) + WorkoutType.entries.filter { allowRest || it != WorkoutType.RestDay }
    var type by rememberSaveable { mutableStateOf(initial.type) }
    var focus by rememberSaveable { mutableStateOf(initial.focus) }
    val hasFocus = type != null && type != WorkoutType.RestDay
    val draft = WorkoutClassification(type, if (hasFocus) focus else "")
    PulseDialog(
        onDismissRequest = onDismiss,
        title = { PulseText(title, style = PulseTextStyle.Title) },
        confirmButton = {
            PulseButton(stringResource(CoreUiR.string.pulse_ui_save), { onSave(draft) }, enabled = draft.validFocus)
        },
        dismissButton = {
            PulseButton(stringResource(CoreUiR.string.pulse_ui_cancel), onDismiss, style = PulseButtonStyle.Text)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                PulseSelectionField(
                    label = stringResource(R.string.pulse_fitness_workout_type),
                    selected = type.label(),
                    options = options.map { it.label() },
                    onSelect = { type = options[it] },
                    labelAbove = true,
                )
                if (hasFocus) {
                    PulseTextField(
                        value = focus,
                        onValueChange = {
                            if (draft.copy(focus = it).validFocus) focus = it
                        },
                        label = stringResource(R.string.pulse_fitness_workout_focus),
                    )
                }
            }
        },
    )
}

@Composable
internal fun WorkoutType?.label(): String =
    stringResource(
        when (this) {
            null -> R.string.pulse_fitness_type_none
            WorkoutType.SingleMuscle -> R.string.pulse_fitness_type_single_muscle
            WorkoutType.DoubleMuscle -> R.string.pulse_fitness_type_double_muscle
            WorkoutType.Push -> R.string.pulse_fitness_type_push
            WorkoutType.Pull -> R.string.pulse_fitness_type_pull
            WorkoutType.Legs -> R.string.pulse_fitness_type_legs
            WorkoutType.UpperBody -> R.string.pulse_fitness_type_upper_body
            WorkoutType.LowerBody -> R.string.pulse_fitness_type_lower_body
            WorkoutType.FullBody -> R.string.pulse_fitness_type_full_body
            WorkoutType.Cardio -> R.string.pulse_fitness_type_cardio
            WorkoutType.Mobility -> R.string.pulse_fitness_type_mobility
            WorkoutType.Other -> R.string.pulse_fitness_type_other
            WorkoutType.RestDay -> R.string.pulse_fitness_type_rest_day
        },
    )
