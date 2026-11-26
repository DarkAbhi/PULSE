package dev.abhishekan.pulse.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FitnessToday
import dev.abhishekan.pulse.core.domain.model.QuestSettingsView
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCardPadding
import dev.abhishekan.pulse.core.ui.components.PulseLinearProgressIndicator
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import java.text.NumberFormat
import dev.abhishekan.pulse.core.ui.R as UiR

/** Web "Today at a glance" (`fitness-at-a-glance.tsx`): steps, active energy, sleep and closed rings. */
@Composable
internal fun DashboardGlance(
    section: Section<FitnessToday>,
    columns: Int,
    onRetry: () -> Unit,
    onViewAll: () -> Unit,
    settings: QuestSettingsView? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        DashboardSectionHeader(
            stringResource(R.string.pulse_home_today_at_a_glance),
            stringResource(R.string.pulse_home_view_all),
            onViewAll,
        )
        section.refreshFailure?.let { PulseErrorState(it.message, onRetry) }
        when (val displayed = section.displayed) {
            Section.Loading ->
                PulseCenteredLoadingState(stringResource(R.string.pulse_home_loading_today), Modifier.fillMaxWidth())
            is Section.Error -> PulseErrorState(displayed.failure.message, onRetry)
            Section.Empty -> PulseText(stringResource(R.string.pulse_home_no_data))
            is Section.Content -> {
                val effectiveGoal = settings?.effective?.takeIf { it <= displayed.value.date }?.let { settings.settings.stepTarget }
                GlanceGrid(displayed.value, columns, effectiveGoal)
            }
        }
    }
}

@Composable
private fun GlanceGrid(
    today: FitnessToday,
    columns: Int,
    stepTarget: Int?,
) {
    val colors = PulseTheme.colors
    val numbers = NumberFormat.getIntegerInstance()
    val noData = stringResource(R.string.pulse_home_no_data)
    val rings =
        listOf(
            today.moveCalories to today.moveCaloriesGoal,
            today.exerciseMinutes?.toDouble() to today.exerciseMinutesGoal?.toDouble(),
            today.standHours?.toDouble() to today.standHoursGoal?.toDouble(),
        ).filter { (_, goal) -> goal != null && goal > 0 }
    val closed = rings.count { (value, goal) -> value != null && value >= goal!! }
    val stepProgress =
        stepTarget?.takeIf { it > 0 }?.let { target ->
            today.steps?.let { (it.toFloat() / target).coerceIn(0f, 1f) }
        }
    val tiles =
        listOf(
            GlanceTile(
                today.steps?.let(numbers::format) ?: noData,
                stringResource(R.string.pulse_home_steps),
                colors.lavender,
                progress = stepProgress,
            ),
            GlanceTile(
                today.moveCalories?.let { "${numbers.format(it)} ${stringResource(R.string.pulse_home_kcal)}" } ?: noData,
                stringResource(R.string.pulse_home_active),
                colors.coral,
            ),
            GlanceTile(
                today.asleepSeconds?.let { stringResource(R.string.pulse_home_sleep_value, (it / 3600).toInt(), (it % 3600 / 60).toInt()) }
                    ?: noData,
                stringResource(R.string.pulse_home_sleep),
                colors.info,
            ),
            GlanceTile(
                stringResource(R.string.pulse_home_activity),
                if (rings.isEmpty()) noData else stringResource(R.string.pulse_home_rings_closed, closed, rings.size),
                colors.success,
            ),
        )
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        tiles.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                row.forEach { tile ->
                    PulseCard(Modifier.weight(1f), contentPadding = PulseCardPadding.medium) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(48.dp).background(tile.accent.copy(alpha = 0.16f), PulseTheme.shapes.control),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(Modifier.size(14.dp).background(tile.accent, CircleShape))
                            }
                            Column {
                                PulseText(tile.value, style = PulseTextStyle.Title)
                                PulseText(tile.label, color = colors.textSecondary)
                                tile.progress?.let { progress ->
                                    PulseLinearProgressIndicator({ progress }, Modifier.fillMaxWidth(), color = tile.accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class GlanceTile(
    val value: String,
    val label: String,
    val accent: Color,
    val progress: Float? = null,
)

@Composable
internal fun DashboardSectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PulseText(title, Modifier.weight(1f).semantics { heading() }, style = PulseTextStyle.Heading3)
        PulseButton(
            action,
            onAction,
            style = PulseButtonStyle.Text,
            trailingIcon = { Icon(painterResource(UiR.drawable.pulse_back), null, Modifier.size(16.dp).rotate(180f)) },
        )
    }
}
