package dev.abhishekan.pulse.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FitnessToday
import dev.abhishekan.pulse.core.domain.model.QuestSettingsView
import dev.abhishekan.pulse.core.ui.PulseBrandColors
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
    val numbers = NumberFormat.getIntegerInstance()
    val noData = stringResource(R.string.pulse_home_no_data)
    val rings =
        listOf(
            GlanceRing(today.moveCalories, today.moveCaloriesGoal, PulseBrandColors.pink),
            GlanceRing(today.exerciseMinutes?.toDouble(), today.exerciseMinutesGoal?.toDouble(), PulseBrandColors.gold),
            GlanceRing(today.standHours?.toDouble(), today.standHoursGoal?.toDouble(), PulseBrandColors.cyan),
        )
    val stepProgress =
        stepTarget?.takeIf { it > 0 }?.let { target ->
            today.steps?.let { (it.toFloat() / target).coerceIn(0f, 1f) }
        }
    val tiles =
        listOf<@Composable () -> Unit>(
            {
                GlanceMetric(
                    today.steps?.let(numbers::format) ?: noData,
                    stringResource(R.string.pulse_home_steps),
                    R.drawable.pulse_glance_steps,
                    PulseBrandColors.purpleBright,
                    progress = stepProgress,
                )
            },
            {
                GlanceMetric(
                    today.moveCalories?.let { "${numbers.format(it)} ${stringResource(R.string.pulse_home_kcal)}" } ?: noData,
                    stringResource(R.string.pulse_home_active),
                    R.drawable.pulse_glance_active,
                    PulseBrandColors.orange,
                )
            },
            {
                GlanceMetric(
                    today.asleepSeconds?.let {
                        stringResource(R.string.pulse_home_sleep_value, (it / 3600).toInt(), (it % 3600 / 60).toInt())
                    } ?: noData,
                    stringResource(R.string.pulse_home_sleep),
                    R.drawable.pulse_glance_sleep,
                    Color(0xFF8B6CF0),
                )
            },
            {
                val summary =
                    when {
                        rings.none { it.value != null } -> noData
                        rings.none { it.goal != null && it.goal > 0 } -> stringResource(R.string.pulse_home_no_ring_goals)
                        else -> stringResource(R.string.pulse_home_rings_closed, rings.count { it.progress == 1.0 }, rings.size)
                    }
                GlanceTile(stringResource(R.string.pulse_home_activity), summary, leading = { GlanceActivityRings(rings) })
            },
        )
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        tiles.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                row.forEach { tile ->
                    PulseCard(Modifier.weight(1f), contentPadding = PulseCardPadding.medium) { tile() }
                }
            }
        }
    }
}

@Composable
private fun GlanceMetric(
    value: String,
    label: String,
    @DrawableRes icon: Int,
    accent: Color,
    progress: Float? = null,
) {
    GlanceTile(value, label, progress = progress, progressColor = accent) {
        Box(
            Modifier.size(56.dp).background(accent.copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp), tint = accent)
        }
    }
}

@Composable
private fun GlanceTile(
    value: String,
    label: String,
    progress: Float? = null,
    progressColor: Color = Color.Unspecified,
    leading: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column {
            PulseText(value, style = PulseTextStyle.Title)
            PulseText(label, color = PulseTheme.colors.textSecondary)
            progress?.let {
                PulseLinearProgressIndicator({ it }, Modifier.fillMaxWidth(), color = progressColor)
            }
        }
    }
}

private data class GlanceRing(
    val value: Double?,
    val goal: Double?,
    val color: Color,
) {
    val progress: Double
        get() = if (value != null && goal != null && goal > 0) (value / goal).coerceIn(0.0, 1.0) else 0.0
}

@Composable
private fun GlanceActivityRings(rings: List<GlanceRing>) {
    Canvas(Modifier.size(56.dp)) {
        val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        rings.forEachIndexed { index, ring ->
            val radius = (22 - index * 6).dp.toPx()
            drawCircle(ring.color.copy(alpha = 0.22f), radius, style = stroke)
            val progress = ring.progress
            if (progress >= 1.0) {
                drawCircle(ring.color, radius, style = stroke)
            } else if (progress > 0.0) {
                drawArc(
                    color = ring.color,
                    startAngle = -90f,
                    sweepAngle = (360 * progress).toFloat(),
                    useCenter = false,
                    topLeft = center - Offset(radius, radius),
                    size = Size(radius * 2, radius * 2),
                    style = stroke,
                )
            }
        }
    }
}

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
