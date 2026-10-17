package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun MealEditor(draft: MealDraft, times: List<MealTime>, busy: Boolean, review: Boolean, error: String?,
    onUpdate: (MealDraft) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    var datePicker by rememberSaveable { mutableStateOf(false) }
    var timePicker by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        PulseText(if (draft.timeForm) "New meal time" else if (draft.id == null) "Plan a meal" else "Edit meal", style = PulseTextStyle.Title)
        PulseTextField(draft.name, { onUpdate(draft.copy(name = it)) }, if (draft.timeForm) "Meal time name" else "Meal name", Modifier.fillMaxWidth(),
            enabled = !busy, error = error, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); onSave() }))
        if (draft.timeForm) {
            PulseTextButton("Start time: ${draft.start}", { focus.clearFocus(); timePicker = "start" }, enabled = !busy)
            PulseTextButton("End time: ${draft.end}", { focus.clearFocus(); timePicker = "end" }, enabled = !busy)
            PulseText("Use 24-hour times. An end earlier than the start spans midnight; equal endpoints are accepted by Pulse.")
        } else {
            PulseTextButton("Meal date: ${draft.date}", { focus.clearFocus(); datePicker = true }, enabled = !busy)
            val choices = buildList {
                if (draft.originalTimeId == null) add("" to "No meal time")
                if (draft.timeId != null && times.none { it.id == draft.timeId }) add(draft.timeId.toString() to "Previously assigned meal time")
                addAll(times.map { it.id.toString() to "${it.name} · ${it.start.format(mealClock)}–${it.end.format(mealClock)}" })
            }
            PulseSelectionField("Meal time", choices.firstOrNull { it.first == draft.timeId?.toString().orEmpty() }?.second ?: "No meal time", choices.map { it.second },
                { onUpdate(draft.copy(timeId = choices[it].first.toLongOrNull())) }, enabled = !busy)
            if (draft.originalTimeId != null) PulseText("You can change the assigned time. Removing it from an existing meal is not available.")
        }
        PulseButton(if (draft.timeForm) "Save meal time" else "Save meal", { focus.clearFocus(); onSave() }, Modifier.fillMaxWidth(), enabled = !review, loading = busy)
        PulseTextButton("Cancel", { focus.clearFocus(); onCancel() }, enabled = !busy)
    }
    if (datePicker) PulseDatePickerDialog(runCatching { LocalDate.parse(draft.date) }.getOrDefault(LocalDate.now()), {
        onUpdate(draft.copy(date = it.toString())); datePicker = false
    }, { datePicker = false })
    timePicker?.let { which ->
        PulseTimePickerDialog(LocalTime.parse(if (which == "start") draft.start else draft.end), {
            onUpdate(if (which == "start") draft.copy(start = it.format(mealClock)) else draft.copy(end = it.format(mealClock))); timePicker = null
        }, { timePicker = null })
    }
}
