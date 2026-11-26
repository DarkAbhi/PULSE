package dev.abhishekan.pulse.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.QuestActivity
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCardPadding
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import dev.abhishekan.pulse.core.ui.R as UiR

private val CELL = 12.dp
private val GAP = 4.dp
private val DAY_LABEL_WIDTH = 24.dp

/** Web `quest-heatmap.tsx`: one column per week, one row per weekday, coloured by quests completed. */
@Composable
internal fun DashboardQuestHeatmap(
    section: Section<QuestActivity>,
    onRetry: () -> Unit,
    onViewQuests: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth(), contentPadding = PulseCardPadding.medium) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PulseText(
                stringResource(R.string.pulse_home_quest_activity),
                Modifier.weight(1f).semantics { heading() },
                style = PulseTextStyle.Title,
            )
            PulseButton(
                stringResource(R.string.pulse_home_view_quests),
                onViewQuests,
                style = PulseButtonStyle.Text,
                trailingIcon = { Icon(painterResource(UiR.drawable.pulse_back), null, Modifier.size(16.dp).rotate(180f)) },
            )
        }
        section.refreshFailure?.let { PulseErrorState(it.message, onRetry) }
        when (val displayed = section.displayed) {
            Section.Loading -> PulseCenteredLoadingState(stringResource(R.string.pulse_home_loading_quests), Modifier.fillMaxWidth())
            is Section.Error -> PulseErrorState(displayed.failure.message, onRetry)
            Section.Empty -> Unit
            is Section.Content -> Heatmap(displayed.value)
        }
    }
}

@Composable
private fun Heatmap(activity: QuestActivity) {
    val counts = activity.days.associate { it.date to it.completedIds.distinct().size }
    var selected by rememberSaveable(activity.today) { mutableStateOf(activity.today.toEpochDay()) }
    val levels = PulseTheme.featureTones.questLevels
    val empty = PulseTheme.colors.chip
    val locale = Locale.getDefault()
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val weeks = ((maxWidth - DAY_LABEL_WIDTH) / (CELL + GAP)).toInt().coerceIn(1, 53)
        // Columns start on Sunday, as on the web, and end with the week containing today.
        val lastSunday = activity.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val firstSunday = lastSunday.minusWeeks((weeks - 1).toLong())
        Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
            Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(GAP)) {
                Spacer(Modifier.width(DAY_LABEL_WIDTH))
                (0 until weeks).forEach { week ->
                    val start = firstSunday.plusWeeks(week.toLong())
                    val label = if (week == 0 || start.dayOfMonth <= 7) start.month.getDisplayName(TextStyle.SHORT, locale) else ""
                    Box(Modifier.width(CELL)) {
                        // Month labels overflow their column, like the web axis.
                        if (label.isNotEmpty()) {
                            PulseText(
                                label,
                                style = PulseTextStyle.Caption,
                                maxLines = 1,
                                modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true).width(40.dp),
                            )
                        }
                    }
                }
            }
            (0 until 7).forEach { weekday ->
                Row(horizontalArrangement = Arrangement.spacedBy(GAP), verticalAlignment = Alignment.CenterVertically) {
                    PulseText(
                        lastSunday.plusDays(weekday.toLong()).dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        Modifier.width(DAY_LABEL_WIDTH).clearAndSetSemantics {},
                        style = PulseTextStyle.Caption,
                        maxLines = 1,
                    )
                    (0 until weeks).forEach { week ->
                        val day = firstSunday.plusWeeks(week.toLong()).plusDays(weekday.toLong())
                        if (day.isAfter(activity.today)) {
                            Box(Modifier.size(CELL))
                        } else {
                            val count = counts[day] ?: 0
                            val isSelected = day.toEpochDay() == selected
                            val description =
                                stringResource(
                                    R.string.pulse_home_heatmap_day,
                                    day.format(dateFormat),
                                    pluralStringResource(R.plurals.pulse_home_quests_completed, count, count),
                                )
                            Box(
                                Modifier
                                    .size(CELL)
                                    .background(if (count == 0) empty else levels[minOf(count, levels.size) - 1], CircleShape)
                                    .then(if (isSelected) Modifier.border(2.dp, PulseTheme.colors.textPrimary, CircleShape) else Modifier)
                                    .semantics {
                                        contentDescription = description
                                        this.selected = isSelected
                                    }.clickable(role = Role.Button) { selected = day.toEpochDay() },
                            )
                        }
                    }
                }
            }
        }
    }
    val day = LocalDate.ofEpochDay(selected)
    val count = counts[day] ?: 0
    PulseText(
        stringResource(
            R.string.pulse_home_heatmap_selected,
            day.format(dateFormat),
            pluralStringResource(R.plurals.pulse_home_quests_completed, count, count),
        ),
        style = PulseTextStyle.Label,
    )
    Row(
        Modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseText(stringResource(R.string.pulse_home_less), style = PulseTextStyle.Caption, color = PulseTheme.colors.textSecondary)
        (listOf(empty) + levels).forEach { Box(Modifier.size(CELL).background(it, CircleShape)) }
        PulseText(stringResource(R.string.pulse_home_more), style = PulseTextStyle.Caption, color = PulseTheme.colors.textSecondary)
    }
    Box(Modifier.height(PulseTokens.tiny))
}
