package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import java.time.LocalTime

@Composable
internal fun MealTimeEditor(
    draft: MealDraft,
    busy: Boolean,
    error: UiText?,
    onUpdate: (MealDraft) -> Unit,
    onSave: () -> Unit,
) {
    var picker by rememberSaveable { mutableStateOf<Int?>(null) }
    val focus = LocalFocusManager.current
    val colors = PulseTheme.colors
    PulseCard(
        Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth(),
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
            PulseTextField(
                value = draft.name,
                onValueChange = { onUpdate(draft.copy(name = it)) },
                label = stringResource(R.string.pulse_mealplan_meal_time_name),
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                labelAbove = true,
                error = error,
                placeholder = {
                    PulseText(stringResource(R.string.pulse_mealplan_meal_time_name_hint), color = colors.textSecondary)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(onDone = {
                        focus.clearFocus()
                        onSave()
                    }),
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth / LocalDensity.current.fontScale >= 272.dp) 2 else 1
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
                    maxItemsInEachRow = columns,
                ) {
                    MealTimePickerField(
                        stringResource(R.string.pulse_mealplan_start_time),
                        draft.start,
                        stringResource(R.string.pulse_mealplan_start_time_1_s, draft.start),
                        !busy,
                        Modifier.weight(1f),
                    ) {
                        focus.clearFocus()
                        picker = R.string.pulse_mealplan_start_time
                    }
                    MealTimePickerField(
                        stringResource(R.string.pulse_mealplan_end_time),
                        draft.end,
                        stringResource(R.string.pulse_mealplan_end_time_1_s, draft.end),
                        !busy,
                        Modifier.weight(1f),
                    ) {
                        focus.clearFocus()
                        picker = R.string.pulse_mealplan_end_time
                    }
                }
            }
        }
    }
    picker?.let { field ->
        val editingStart = field == R.string.pulse_mealplan_start_time
        val time = if (editingStart) draft.start else draft.end
        PulseTimePickerDialog(
            runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.MIDNIGHT),
            { selected ->
                val value = selected.format(mealClock)
                onUpdate(if (editingStart) draft.copy(start = value) else draft.copy(end = value))
                picker = null
            },
            { picker = null },
        )
    }
}

@Composable
private fun MealTimePickerField(
    label: String,
    value: String,
    description: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(label, style = PulseTextStyle.Label)
        PulseButton(
            value,
            onClick,
            Modifier.fillMaxWidth().semantics { contentDescription = description },
            enabled = enabled,
            variant = PulseButtonVariant.Secondary,
            leadingIcon = { Icon(painterResource(R.drawable.meal_clock), null, Modifier.size(18.dp)) },
        )
    }
}
