package dev.abhishekan.pulse.feature.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.DailyQuest
import dev.abhishekan.pulse.core.domain.model.QuestOverview
import dev.abhishekan.pulse.core.domain.model.QuestProgress
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseLinearProgressIndicator
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable
internal fun QuestStreak(
    streak: Int,
    modifier: Modifier = Modifier,
) {
    val ink = Color(0xFF3A2A00)
    PulseCard(modifier, containerColor = Color(0xFFFFE08A)) {
        PulseText(stringResource(R.string.pulse_quests_streak), Modifier.semantics { heading() }, color = ink)
        PulseText(
            pluralStringResource(R.plurals.pulse_quests_days, streak, streak),
            style = PulseTextStyle.Heading1,
            color = ink,
        )
        PulseText(stringResource(R.string.pulse_quests_streak_description), color = ink)
    }
}

@Composable
internal fun QuestWeek(
    overview: QuestOverview,
    loading: Boolean,
    onWeek: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    PulseCard(modifier) {
        val current = overview.week.any { it.date == overview.today }
        QuestHeading(if (current) R.string.pulse_quests_week else R.string.pulse_quests_selected_week)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PulseButton(
                stringResource(R.string.pulse_quests_previous_week),
                { overview.previousWeek?.let(onWeek) },
                enabled = overview.previousWeek != null && !loading,
                style = PulseButtonStyle.Icon,
                icon = { Icon(painterResource(CoreUiR.drawable.pulse_back), null) },
            )
            PulseButton(
                stringResource(R.string.pulse_quests_next_week),
                { overview.nextWeek?.let(onWeek) },
                enabled = overview.nextWeek != null && !loading,
                style = PulseButtonStyle.Icon,
                icon = { Icon(painterResource(CoreUiR.drawable.pulse_back), null, Modifier.rotate(180f)) },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            overview.week.forEach { day ->
                val colors = PulseTheme.colors
                val isToday = day.date == overview.today
                val complete = day.status == "complete"
                val background =
                    when {
                        isToday -> colors.primary
                        complete -> colors.successSoft
                        else -> colors.surfaceMuted
                    }
                val text = if (isToday) colors.onPrimary else colors.textPrimary
                val status =
                    when (day.status) {
                        "complete" -> R.string.pulse_quests_complete
                        "pending" -> R.string.pulse_quests_pending
                        "missed" -> R.string.pulse_quests_missed
                        "rest" -> R.string.pulse_quests_rest
                        "future" -> R.string.pulse_quests_future
                        else -> R.string.pulse_quests_unavailable
                    }
                val detail =
                    stringResource(
                        R.string.pulse_quests_day_status,
                        day.date.format(dateFormat),
                        stringResource(status),
                        day.completed,
                        day.total,
                    )
                Column(
                    Modifier
                        .weight(1f)
                        .background(background, RoundedCornerShape(10.dp))
                        .padding(vertical = 6.dp)
                        .clearAndSetSemantics { contentDescription = detail },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    PulseText(
                        day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        color = text,
                        style = PulseTextStyle.Caption,
                        maxLines = 1,
                    )
                    PulseText(day.date.dayOfMonth.toString(), color = text, style = PulseTextStyle.Body3, maxLines = 1)
                    PulseText(
                        if (complete) {
                            "✓"
                        } else if (day.completed > 0) {
                            "•"
                        } else {
                            " "
                        },
                        color = text,
                        style = PulseTextStyle.Caption,
                    )
                }
            }
        }
    }
}

@Composable
internal fun QuestTodayXp(
    progress: QuestProgress,
    modifier: Modifier = Modifier,
) {
    PulseCard(modifier) {
        QuestHeading(R.string.pulse_quests_today_xp)
        PulseText(stringResource(R.string.pulse_quests_level, progress.level), style = PulseTextStyle.Heading1)
        PulseText(stringResource(R.string.pulse_quests_xp_progress, progress.todayXp, progress.todayXpTarget))
        if (progress.todayXpTarget > 0) {
            PulseLinearProgressIndicator(
                { (progress.todayXp.toFloat() / progress.todayXpTarget).coerceIn(0f, 1f) },
                Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun QuestDaily(
    overview: QuestOverview,
    onGoals: () -> Unit,
    onWorkoutDays: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleQuests = overview.todayQuests.filter { it.labelId != null }
    PulseCard(modifier) {
        QuestHeading(R.string.pulse_quests_daily)
        if (overview.settings.configured) {
            PulseText(stringResource(R.string.pulse_quests_completion, overview.progress.todayComplete, overview.todayQuests.size))
            if (visibleQuests.isEmpty()) {
                PulseText(stringResource(R.string.pulse_quests_no_daily), color = PulseTheme.colors.textSecondary)
            }
            if (overview.settings.settings.gymWorkout && !overview.settings.hasWorkoutDays) {
                PulseButton(
                    stringResource(R.string.pulse_quests_add_workout_days),
                    onWorkoutDays,
                    variant = PulseButtonVariant.Secondary,
                )
            }
            visibleQuests.forEach { QuestRow(it) }
        } else {
            PulseText(stringResource(R.string.pulse_quests_setup_message))
            PulseButton(stringResource(R.string.pulse_quests_set_goals), onGoals)
        }
    }
}

private val DailyQuest.labelId: Int?
    get() =
        when (id) {
            "gym_workout" -> R.string.pulse_quests_workout
            "daily_steps" -> R.string.pulse_quests_steps
            "activity_rings" -> R.string.pulse_quests_activity_rings
            "sleep_goal" -> R.string.pulse_quests_sleep
            "daily_meals" -> R.string.pulse_quests_meals
            else -> null
        }

@Composable
private fun QuestRow(quest: DailyQuest) {
    val label = quest.labelId ?: return
    val colors = PulseTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (quest.completed) colors.successSoft else colors.surfaceMuted, PulseTheme.shapes.medium)
            .padding(PulseTokens.medium),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
    ) {
        PulseText(stringResource(label), style = PulseTextStyle.Label)
        val progress =
            if (quest.id == "sleep_goal") {
                stringResource(R.string.pulse_quests_sleep_progress, questDuration(quest.progress), questDuration(quest.target))
            } else {
                stringResource(R.string.pulse_quests_progress_value, quest.progress, quest.target)
            }
        PulseText(progress, color = colors.textSecondary)
        PulseText(
            listOf(
                stringResource(R.string.pulse_quests_xp, quest.xp),
                stringResource(R.string.pulse_quests_hearts, quest.hearts),
                if (quest.completed) stringResource(R.string.pulse_quests_complete) else stringResource(R.string.pulse_quests_pending),
            ).joinToString(" · "),
            style = PulseTextStyle.Body3,
            color = if (quest.completed) colors.success else colors.action,
        )
    }
}

@Composable
private fun questDuration(seconds: Int): String = stringResource(R.string.pulse_quests_duration, seconds / 3600, seconds / 60 % 60)

@Composable
internal fun QuestMedals(bestStreak: Int) {
    val milestones = listOf(3, 7, 14, 30)
    val next = milestones.firstOrNull { it > bestStreak }
    PulseCard(Modifier.fillMaxWidth()) {
        QuestHeading(R.string.pulse_quests_medals)
        PulseText(stringResource(R.string.pulse_quests_best_streak, bestStreak))
        milestones.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                pair.forEach { milestone ->
                    val earned = milestone <= bestStreak
                    val label =
                        when {
                            earned -> R.string.pulse_quests_medal_earned
                            milestone == next -> R.string.pulse_quests_medal_next
                            else -> R.string.pulse_quests_medal_upcoming
                        }
                    PulseText(
                        stringResource(label, milestone),
                        Modifier
                            .weight(1f)
                            .background(
                                if (earned) Color(0xFFFFE08A) else PulseTheme.colors.surfaceMuted,
                                PulseTheme.shapes.medium,
                            ).padding(PulseTokens.small),
                        textAlign = TextAlign.Center,
                        color = if (earned) Color(0xFF3A2A00) else PulseTheme.colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
internal fun QuestWeekProgress(progress: QuestProgress) {
    PulseCard(Modifier.fillMaxWidth()) {
        QuestHeading(R.string.pulse_quests_week_progress)
        PulseText(stringResource(R.string.pulse_quests_xp, progress.weekXp), style = PulseTextStyle.Title)
        PulseText(stringResource(R.string.pulse_quests_hearts, progress.weekHearts))
        PulseText(stringResource(R.string.pulse_quests_week_active, progress.weekActive))
        PulseText(stringResource(R.string.pulse_quests_level, progress.level))
    }
}

@Composable
internal fun QuestHeading(resource: Int) {
    PulseText(stringResource(resource), Modifier.semantics { heading() }, style = PulseTextStyle.Heading2)
}
