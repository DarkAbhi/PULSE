package dev.abhishekan.pulse.feature.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import dev.abhishekan.pulse.core.domain.model.QuestSettings
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseSwitch
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable
internal fun QuestGoalsDialog(
    settings: QuestSettings,
    state: QuestUiState,
    onSave: (QuestSettings) -> Unit,
    onClose: () -> Unit,
) {
    var gym by rememberSaveable { mutableStateOf(settings.gymWorkout) }
    var rings by rememberSaveable { mutableStateOf(settings.activityRings) }
    var meals by rememberSaveable { mutableStateOf(settings.dailyMeals) }
    var stepsOn by rememberSaveable { mutableStateOf(settings.stepTarget != null) }
    var sleepOn by rememberSaveable { mutableStateOf(settings.sleepMinutes != null) }
    var steps by rememberSaveable { mutableStateOf(settings.stepTarget?.toString().orEmpty()) }
    var sleep by rememberSaveable { mutableStateOf(settings.sleepMinutes?.let(::formatSleepGoal).orEmpty()) }
    val stepValue = steps.trim().toIntOrNull()?.takeIf { it in 1..100_000 }
    val sleepValue = parseSleepGoal(sleep)
    val valid = (!stepsOn || stepValue != null) && (!sleepOn || sleepValue != null)
    val enabled = !state.saving
    PulseDialog(
        onDismissRequest = { if (enabled) onClose() },
        title = { PulseText(stringResource(R.string.pulse_quests_goals), style = PulseTextStyle.Title) },
        confirmButton = {
            PulseButton(
                stringResource(CoreUiR.string.pulse_ui_save),
                {
                    onSave(
                        QuestSettings(gym, stepValue.takeIf { stepsOn }, rings, sleepValue.takeIf { sleepOn }, meals),
                    )
                },
                enabled = valid && enabled,
                loading = state.saving,
            )
        },
        dismissButton = {
            PulseButton(stringResource(CoreUiR.string.pulse_ui_cancel), onClose, enabled = enabled, style = PulseButtonStyle.Text)
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
            ) {
                state.saveFailure?.let { PulseText(it.message, color = PulseTheme.colors.error) }
                QuestGoalSwitch(R.string.pulse_quests_workout, gym, enabled) { gym = it }
                QuestGoalSwitch(R.string.pulse_quests_steps, stepsOn, enabled) { stepsOn = it }
                if (stepsOn) {
                    PulseTextField(
                        steps,
                        { steps = it.take(6) },
                        stringResource(R.string.pulse_quests_step_target),
                        Modifier.fillMaxWidth(),
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { PulseText(stringResource(R.string.pulse_quests_step_hint)) },
                    )
                }
                QuestGoalSwitch(R.string.pulse_quests_activity_rings, rings, enabled) { rings = it }
                QuestGoalSwitch(R.string.pulse_quests_sleep, sleepOn, enabled) { sleepOn = it }
                if (sleepOn) {
                    PulseTextField(
                        sleep,
                        { sleep = it.take(5) },
                        stringResource(R.string.pulse_quests_sleep),
                        Modifier.fillMaxWidth(),
                        enabled = enabled,
                        supportingText = {
                            val invalid = sleep.isNotBlank() && sleepValue == null
                            PulseText(
                                stringResource(if (invalid) R.string.pulse_quests_sleep_error else R.string.pulse_quests_sleep_hint),
                                color = if (invalid) PulseTheme.colors.error else PulseTheme.colors.textSecondary,
                            )
                        },
                    )
                }
                QuestGoalSwitch(R.string.pulse_quests_meals, meals, enabled) { meals = it }
            }
        },
    )
}

@Composable
private fun QuestGoalSwitch(
    label: Int,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    PulseSwitch(checked, onChange, stringResource(label), Modifier.fillMaxWidth(), enabled = enabled, switchAtEnd = true)
}

private val sleepGoalPattern = Regex("""([0-9]{1,2})(?::([0-5][0-9]))?""")

internal fun parseSleepGoal(input: String): Int? {
    val match = sleepGoalPattern.matchEntire(input.trim()) ?: return null
    val minutes = match.groupValues[1].toInt() * 60 + (match.groupValues[2].toIntOrNull() ?: 0)
    return minutes.takeIf { it in 1..1440 }
}

private fun formatSleepGoal(minutes: Int): String {
    val remainder = minutes % 60
    return if (remainder == 0) (minutes / 60).toString() else "${minutes / 60}:${remainder.toString().padStart(2, '0')}"
}
