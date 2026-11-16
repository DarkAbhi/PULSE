package dev.abhishekan.pulse.feature.fitness.overview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.pulseDescription
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.core.ui.R as CoreUiR
import java.time.format.DateTimeFormatter

@Composable
internal fun SelectedDay(
    state: FitnessUiState,
    onRecord: () -> Unit,
    onWorkout: (Long) -> Unit,
) {
    val overview = checkNotNull(state.overview)
    val locale = LocalConfiguration.current.locales[0]
    val workouts = remember(overview.workouts, state.selected) { overview.workouts.filter { it.date == state.selected } }
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale) }
    val workoutFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMMM yyyy", locale) }
    val dateLabel = state.selected.format(dayFormatter)
    val countLabel = pluralStringResource(R.plurals.pulse_fitness_workout_count, workouts.size, workouts.size)
    val selectedSummary = stringResource(R.string.pulse_fitness_selected_day_summary, dateLabel, countLabel)
    val largeText = LocalDensity.current.fontScale >= 1.5f
    var confirmRecord by rememberSaveable(state.selected) { mutableStateOf(false) }
    if (confirmRecord && state.canRecord) {
        PulseDialog(
            title = stringResource(R.string.pulse_fitness_confirm_record_title),
            message =
                stringResource(
                    R.string.pulse_fitness_confirm_record_message,
                    dateLabel,
                ),
            confirmLabel = stringResource(R.string.pulse_fitness_record_workout),
            onConfirm = {
                confirmRecord = false
                onRecord()
            },
            onDismiss = { confirmRecord = false },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
            ) {
                PulseText(
                    stringResource(R.string.pulse_fitness_selected_day),
                    Modifier.align(Alignment.CenterVertically).padding(end = PulseTokens.small),
                    style = PulseTextStyle.Overline,
                    color = PulseTheme.colors.action,
                )
                Surface(
                    shape = CircleShape,
                    color = PulseTheme.colors.surface,
                    contentColor = PulseTheme.colors.action,
                ) {
                    PulseText(
                        countLabel,
                        Modifier.padding(horizontal = PulseTokens.small, vertical = PulseTokens.tiny).clearAndSetSemantics {},
                        style = PulseTextStyle.Caption,
                        color = PulseTheme.colors.action,
                    )
                }
            }
            PulseText(
                dateLabel,
                Modifier.clearAndSetSemantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = selectedSummary
                },
                style = PulseTextStyle.Title,
            )
            if (workouts.isEmpty()) {
                PulseText(
                    stringResource(R.string.pulse_fitness_no_workouts_for_day),
                    Modifier.padding(vertical = PulseTokens.medium),
                    color = PulseTheme.colors.textSecondary,
                )
            }
            workouts.forEachIndexed { index, workout ->
                Surface(
                    onClick = { onWorkout(workout.id) },
                    enabled = !state.busy,
                    modifier =
                        Modifier.fillMaxWidth().pulseDescription(
                            stringResource(
                                R.string.pulse_fitness_view_workout_1_s_2_s_3_s,
                                index + 1,
                                workout.date.format(workoutFormatter),
                                workout.timeLabel,
                            ),
                        ),
                    shape = PulseTokens.cardShape,
                    color = PulseTheme.colors.surface,
                    border =
                        BorderStroke(
                            1.dp,
                            PulseTheme.colors.divider,
                        ),
                ) {
                    Row(
                        Modifier.padding(PulseTokens.medium),
                        horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!largeText) WorkoutBadge()
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                            PulseText(
                                if (workouts.size == 1) {
                                    stringResource(R.string.pulse_fitness_workout_title)
                                } else {
                                    stringResource(R.string.pulse_fitness_workout_number, index + 1)
                                },
                                style = PulseTextStyle.Label,
                            )
                            PulseText(workout.timeLabel, color = PulseTheme.colors.textSecondary)
                        }
                        if (!largeText) FitnessGlyph(FitnessSymbol.Next, PulseTheme.colors.textSecondary)
                    }
                }
            }
            PulseButton(
                stringResource(R.string.pulse_fitness_add_workout),
                { confirmRecord = true },
                Modifier.fillMaxWidth().padding(top = PulseTokens.small),
                enabled = state.canRecord,
                loading = state.busy,
            )
        }
        state.createdId?.takeIf { id -> workouts.none { it.id == id } }?.let { id ->
            PulseButton(stringResource(R.string.pulse_fitness_open_recorded_workout), {
                onWorkout(id)
            }, Modifier.fillMaxWidth(), enabled = !state.busy)
        }
    }
}

@Composable
private fun WorkoutBadge() {
    Surface(
        shape = CircleShape,
        color = PulseTheme.colors.primarySoft,
    ) {
        Icon(
            painterResource(CoreUiR.drawable.pulse_fitness),
            contentDescription = null,
            modifier = Modifier.padding(PulseTokens.medium),
            tint = PulseTheme.colors.action,
        )
    }
}
