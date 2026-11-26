package dev.abhishekan.pulse.feature.fitness.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.domain.model.WorkoutClassification
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.shared.WorkoutClassificationDialog
import dev.abhishekan.pulse.feature.fitness.shared.fitnessFailureMessage
import dev.abhishekan.pulse.feature.fitness.shared.label
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
internal fun WorkoutScheduleCard(
    state: FitnessUiState,
    onAction: (FitnessAction) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }
    val locale = LocalLocale.current.platformLocale
    val byDay = state.schedule.associate { it.day to it.classification }
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_fitness_workout_schedule),
            Modifier.semantics { heading() },
            style = PulseTextStyle.Heading2,
        )
        when (val scheduleState = state.scheduleState) {
            is FitnessLoadState.Loading -> PulseText(stringResource(R.string.pulse_fitness_loading_schedule))
            is FitnessLoadState.Failed ->
                PulseErrorState(fitnessFailureMessage(scheduleState.failure), { onAction(FitnessAction.RefreshSchedule) })
            FitnessLoadState.Ready ->
                DayOfWeek.entries.forEach { day ->
                    val classification = byDay[day]
                    val name = day.getDisplayName(TextStyle.FULL, locale)
                    val type = classification?.type.label()
                    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                        PulseButton(
                            stringResource(R.string.pulse_fitness_schedule_day, name, type),
                            { editing = day },
                            enabled = !state.savingSchedule,
                            style = PulseButtonStyle.Text,
                        )
                        classification?.focus?.takeIf(String::isNotBlank)?.let {
                            PulseText(it, style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary)
                        }
                    }
                }
        }
    }
    editing?.let { day ->
        WorkoutClassificationDialog(
            title = day.getDisplayName(TextStyle.FULL, locale),
            initial = byDay[day] ?: WorkoutClassification(null),
            allowRest = true,
            onSave = {
                onAction(FitnessAction.SaveScheduleDay(day, it))
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}
