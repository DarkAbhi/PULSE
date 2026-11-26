package dev.abhishekan.pulse.feature.fitness.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.WorkoutScheduleDay
import dev.abhishekan.pulse.core.domain.model.WorkoutSummary
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCardPadding
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.shared.label
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun SelectedDay(
    date: LocalDate,
    visits: List<WorkoutSummary>,
    plan: WorkoutScheduleDay?,
    onWorkout: (Long) -> Unit,
    createdId: Long?,
    busy: Boolean,
    modifier: Modifier = Modifier,
) {
    val muted = fitnessCalendarColors().muted
    val theme = PulseTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
    val dayHeading: @Composable () -> Unit = {
        PulseText(dateLabel, Modifier.semantics { heading() }, style = PulseTextStyle.Title3)
    }
    PulseCard(
        modifier.semantics { liveRegion = LiveRegionMode.Polite },
        contentPadding = PulseCardPadding.medium,
    ) {
        if (visits.isEmpty()) {
            Column {
                dayHeading()
                PulseText(stringResource(R.string.pulse_fitness_calendar_no_workout_logged), color = muted)
            }
        }
        visits.forEachIndexed { index, visit ->
            if (index > 0) HorizontalDivider(color = theme.divider)
            val label =
                visit.classification.type?.let { type ->
                    listOf(type.label(), visit.classification.focus).filter(String::isNotBlank).joinToString(" · ")
                } ?: stringResource(R.string.pulse_fitness_calendar_gym_visit)
            val details = stringResource(R.string.pulse_fitness_calendar_visit_time, label, visit.timeLabel)
            val actionLabel = stringResource(R.string.pulse_fitness_calendar_view_workout_for, details)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val detailsContent: @Composable (Modifier) -> Unit = { detailsModifier ->
                    Column(detailsModifier) {
                        if (index == 0) dayHeading()
                        PulseText(details, color = muted)
                    }
                }
                val button: @Composable () -> Unit = {
                    Surface(
                        onClick = { onWorkout(visit.id) },
                        enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = actionLabel },
                        shape = CircleShape,
                        color = theme.chip,
                        contentColor = theme.textPrimary,
                    ) {
                        Row(
                            Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PulseText(stringResource(R.string.pulse_fitness_view_workout), style = PulseTextStyle.Label)
                            FitnessGlyph(FitnessSymbol.Next, theme.textPrimary, 16.dp)
                        }
                    }
                }
                if (maxWidth >= 304.dp * LocalDensity.current.fontScale) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        detailsContent(Modifier.weight(1f))
                        button()
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        detailsContent(Modifier.fillMaxWidth())
                        button()
                    }
                }
            }
        }
        plan?.classification?.type?.let { type ->
            HorizontalDivider(color = theme.divider)
            val label = listOf(type.label(), plan.classification.focus).filter(String::isNotBlank).joinToString(" · ")
            PulseText(
                stringResource(R.string.pulse_fitness_calendar_scheduled, label),
                style = PulseTextStyle.Label,
                color = muted,
            )
        }
        // Keep the saved workout reachable if the refresh after recording failed.
        createdId?.takeIf { id -> visits.none { it.id == id } }?.let { id ->
            PulseButton(stringResource(R.string.pulse_fitness_open_recorded_workout), { onWorkout(id) }, enabled = !busy)
        }
    }
}
