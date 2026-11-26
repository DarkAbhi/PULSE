package dev.abhishekan.pulse.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.WorkoutToday
import dev.abhishekan.pulse.core.ui.PulseBrandColors
import dev.abhishekan.pulse.core.ui.PulseShapes
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseTone
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTileCard
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import dev.abhishekan.pulse.core.ui.R as UiR

/** The four web quick-action tiles (`features/dashboard/components/quick-actions`). */
@Composable
internal fun DashboardQuickActions(
    state: HomeUiState,
    columns: Int,
    currency: String,
    timeZone: ZoneId,
    onRetry: (HomeSection) -> Unit,
    onHorizon: () -> Unit,
    onFitness: () -> Unit,
    onGarage: () -> Unit,
    onMealPlan: () -> Unit,
    gap: Dp = PulseTokens.cardPadding,
) {
    val tones = PulseTheme.featureTones
    val tiles: List<@Composable (Modifier) -> Unit> =
        listOf(
            { m -> FinanceTile(state.horizon, tones.finance, currency, { onRetry(HomeSection.Horizon) }, onHorizon, m) },
            { m -> FitnessTile(state.workout, tones.fitness, { onRetry(HomeSection.Workout) }, onFitness, m) },
            { m ->
                QuickActionTile(stringResource(R.string.pulse_home_garage), UiR.drawable.pulse_garage, tones.garage, onGarage, m) {
                    PulseText(stringResource(R.string.pulse_home_garage_summary), color = tones.garage.foreground)
                }
            },
            { m ->
                QuickActionTile(stringResource(R.string.pulse_home_meal_plan), UiR.drawable.pulse_meals, tones.meal, onMealPlan, m) {
                    PulseText(stringResource(R.string.pulse_home_meal_plan_summary), color = tones.meal.foreground)
                    Spacer(Modifier.size(PulseTokens.small))
                    WeekStrip(LocalDate.now(timeZone), tones.meal.foreground)
                }
            },
        )
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        tiles.chunked(columns).forEach { row ->
            val equalHeight = row.size > 1
            Row(
                Modifier.fillMaxWidth().then(if (equalHeight) Modifier.height(IntrinsicSize.Min) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEach { tile ->
                    tile(Modifier.weight(1f).then(if (equalHeight) Modifier.fillMaxHeight() else Modifier))
                }
            }
        }
    }
}

@Composable
private fun FinanceTile(
    section: Section<Horizon>,
    tone: PulseTone,
    currency: String,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    QuickActionTile(
        stringResource(R.string.pulse_home_your_room_to_move),
        UiR.drawable.pulse_horizon,
        tone,
        onOpen,
        modifier,
        description = stringResource(R.string.pulse_home_financial_horizon),
        actionColor = PulseBrandColors.gold,
        backdrop = { FinanceWave(Modifier.matchParentSize()) },
    ) {
        PulseText(stringResource(R.string.pulse_home_whats_left), color = tone.foreground.copy(alpha = 0.85f))
        section.refreshFailure?.let { PulseErrorState(it.message, onRetry) }
        when (val displayed = section.displayed) {
            is Section.Content ->
                PulseText(
                    UserCurrency.format(displayed.value.remainingAmount, currency),
                    style = PulseTextStyle.Metric,
                    color = tone.foreground,
                )
            is Section.Error -> PulseErrorState(displayed.failure.message, onRetry)
            Section.Empty -> PulseText(stringResource(R.string.pulse_home_no_summary_yet), color = tone.foreground)
            Section.Loading -> PulseText(stringResource(R.string.pulse_home_loading_your_horizon), color = tone.foreground)
        }
    }
}

@Composable
private fun FinanceWave(modifier: Modifier) {
    // Match the web finance card's 400 × 80 path and nine-second drift.
    val wave = remember { PathParser().parsePathString("M-30 49Q39 -2 105 36T240 36T375 36T510 36V80H-30Z").toPath() }
    val waveColor = Color.White.copy(alpha = if (PulseTheme.isDark) 0.35f else 0.5f)
    val motionScale = rememberCoroutineScope().coroutineContext[MotionDurationScale]
    val transition = rememberInfiniteTransition(label = "Finance wave")
    val drift =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0.06f,
            animationSpec = infiniteRepeatable(tween(9_000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)), RepeatMode.Reverse),
            label = "Wave drift",
        )
    Canvas(modifier) {
        val width = size.width
        val waveHeight = size.height / 3f
        val shift = if (motionScale?.scaleFactor == 0f) 0f else drift.value
        withTransform({
            translate(left = -width * shift, top = size.height - waveHeight)
            scale(scaleX = width / 400f, scaleY = waveHeight / 80f, pivot = Offset.Zero)
        }) {
            drawPath(wave, waveColor)
        }
    }
}

@Composable
private fun FitnessTile(
    section: Section<WorkoutToday>,
    tone: PulseTone,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    QuickActionTile(stringResource(R.string.pulse_home_fitness), UiR.drawable.pulse_fitness, tone, onOpen, modifier) {
        section.refreshFailure?.let { PulseErrorState(it.message, onRetry) }
        when (val displayed = section.displayed) {
            is Section.Content -> PulseText(stringResource(R.string.pulse_home_fitness_recorded), color = tone.foreground)
            Section.Empty -> {
                PulseText(stringResource(R.string.pulse_home_no_workout_recorded_today), color = tone.foreground)
                PulseText(stringResource(R.string.pulse_home_fitness_empty), color = tone.foreground)
            }
            is Section.Error -> PulseErrorState(displayed.failure.message, onRetry)
            Section.Loading -> PulseText(stringResource(R.string.pulse_home_loading_today_s_workout), color = tone.foreground)
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    @DrawableRes icon: Int,
    tone: PulseTone,
    onOpen: () -> Unit,
    modifier: Modifier,
    description: String = title,
    actionColor: Color = tone.start,
    backdrop: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val openLabel = description
    PulseTileCard(
        tone,
        modifier
            .clip(PulseShapes.hero)
            .semantics { contentDescription = openLabel }
            .clickable(role = Role.Button, onClick = onOpen),
        backdrop = backdrop,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), null, Modifier.size(20.dp), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            // Web: a white action disc whose arrow takes the tile colour.
            Box(Modifier.size(36.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(UiR.drawable.pulse_back), null, Modifier.size(16.dp).rotate(180f), tint = actionColor)
            }
        }
        Spacer(Modifier.size(PulseTokens.small))
        PulseText(title, style = PulseTextStyle.Title, color = tone.foreground)
        content()
    }
}

@Composable
private fun WeekStrip(
    today: LocalDate,
    foreground: Color,
) {
    val monday = today.with(DayOfWeek.MONDAY)
    Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0L until 7L).map { monday.plusDays(it) }.forEach { day ->
            val isToday = day == today
            Box(
                Modifier
                    .size(24.dp)
                    .then(if (isToday) Modifier.background(foreground, CircleShape) else Modifier.border(1.dp, foreground, CircleShape)),
                contentAlignment = Alignment.Center,
            ) {
                PulseText(
                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = PulseTextStyle.Caption,
                    color = if (isToday) PulseTheme.featureTones.meal.end else foreground,
                    modifier = Modifier.padding(0.dp),
                )
            }
        }
    }
}
