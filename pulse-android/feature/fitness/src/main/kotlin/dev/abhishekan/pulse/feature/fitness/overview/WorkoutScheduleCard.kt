package dev.abhishekan.pulse.feature.fitness.overview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.WorkoutClassification
import dev.abhishekan.pulse.core.domain.model.WorkoutType
import dev.abhishekan.pulse.core.ui.PulseShapes
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCardPadding
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.core.ui.pulseWindowSize
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.shared.FitnessLoadState
import dev.abhishekan.pulse.feature.fitness.shared.WorkoutClassificationDialog
import dev.abhishekan.pulse.feature.fitness.shared.fitnessFailureMessage
import dev.abhishekan.pulse.feature.fitness.shared.label
import java.time.DayOfWeek
import java.time.format.TextStyle

private enum class ScheduleKind { Workout, Rest, None }

private fun WorkoutClassification?.kind(): ScheduleKind =
    when (this?.type) {
        null -> ScheduleKind.None
        WorkoutType.RestDay -> ScheduleKind.Rest
        else -> ScheduleKind.Workout
    }

/** Web `features/fitness/styles/fitness-theme.css`; the gradient has no dark override there. */
@Immutable
private data class ScheduleColors(
    val chip: Color,
    val done: Color,
    val today: Color,
    val muted: Color,
    val gradient: Brush,
)

private val ScheduleGradient = Brush.linearGradient(listOf(Color(0xFF3A2CD9), Color(0xFFA420BD)))

@Composable
private fun scheduleColors(): ScheduleColors =
    if (PulseTheme.isDark) {
        ScheduleColors(Color(0xFF2B2749), Color(0xFF193953), Color(0xFFFF55D1), Color(0xFFB7B0D1), ScheduleGradient)
    } else {
        ScheduleColors(Color(0xFFEEEDF6), Color(0xFFE6EBFF), Color(0xFFDC229F), PulseTheme.colors.textSecondary, ScheduleGradient)
    }

@Composable
internal fun WorkoutScheduleCard(
    state: FitnessUiState,
    onAction: (FitnessAction) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }
    val locale = LocalLocale.current.platformLocale
    val byDay = state.schedule.associate { it.day to it.classification }
    val colors = scheduleColors()
    PulseCard(Modifier.fillMaxWidth(), contentPadding = PulseCardPadding.medium) {
        ScheduleHeader(state, colors)
        Spacer(Modifier.height(PulseSpacing.compact))
        when (val scheduleState = state.scheduleState) {
            is FitnessLoadState.Loading ->
                PulseText(stringResource(R.string.pulse_fitness_loading_schedule), color = colors.muted)
            is FitnessLoadState.Failed ->
                PulseErrorState(fitnessFailureMessage(scheduleState.failure), { onAction(FitnessAction.RefreshSchedule) })
            FitnessLoadState.Ready ->
                ScheduleGrid(
                    byDay = byDay,
                    today = state.overview?.today?.dayOfWeek,
                    enabled = !state.savingSchedule,
                    colors = colors,
                    dayName = { it.getDisplayName(TextStyle.FULL, locale) },
                    shortDayName = { it.getDisplayName(TextStyle.SHORT, locale) },
                    onEdit = { editing = it },
                )
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

@Composable
private fun ScheduleHeader(
    state: FitnessUiState,
    colors: ScheduleColors,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PulseSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(PulseSpacing.compact),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).widthIn(min = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(PulseSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(PulseShapes.control).background(colors.gradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.fitness_schedule_calendar), null, Modifier.size(22.dp), tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                PulseText(
                    stringResource(R.string.pulse_fitness_workout_schedule),
                    Modifier.semantics { heading() },
                    style = PulseTextStyle.Heading2,
                )
                PulseText(
                    stringResource(R.string.pulse_fitness_schedule_subtitle),
                    style = PulseTextStyle.Body2,
                    color = colors.muted,
                )
            }
        }
        if (state.scheduleState == FitnessLoadState.Ready && state.schedule.isNotEmpty()) {
            val workoutDays = state.schedule.count { it.classification.type != WorkoutType.RestDay }
            val restDays = state.schedule.size - workoutDays
            PulseText(
                pluralStringResource(R.plurals.pulse_fitness_schedule_summary, workoutDays, workoutDays, restDays),
                Modifier
                    .clip(PulseShapes.pill)
                    .background(colors.chip)
                    .padding(horizontal = PulseSpacing.compact, vertical = PulseSpacing.tiny),
                style = PulseTextStyle.Label2,
            )
        }
    }
}

@Composable
private fun ScheduleGrid(
    byDay: Map<DayOfWeek, WorkoutClassification>,
    today: DayOfWeek?,
    enabled: Boolean,
    colors: ScheduleColors,
    dayName: (DayOfWeek) -> String,
    shortDayName: (DayOfWeek) -> String,
    onEdit: (DayOfWeek) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns =
            when (pulseWindowSize(maxWidth)) {
                PulseWindowSize.Compact -> 2
                PulseWindowSize.Medium -> 4
                PulseWindowSize.Expanded -> 7
            }
        val gap = if (columns == 2) PulseSpacing.small else PulseSpacing.compact
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            DayOfWeek.entries.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { day ->
                        ScheduleDayTile(
                            dayName = dayName(day),
                            shortDayName = shortDayName(day),
                            classification = byDay[day],
                            isToday = day == today,
                            enabled = enabled,
                            colors = colors,
                            onClick = { onEdit(day) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ScheduleDayTile(
    dayName: String,
    shortDayName: String,
    classification: WorkoutClassification?,
    isToday: Boolean,
    enabled: Boolean,
    colors: ScheduleColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kind = classification.kind()
    val shape = PulseShapes.card
    val divider = PulseTheme.colors.divider
    val title =
        when (kind) {
            ScheduleKind.Workout -> classification?.type.label()
            ScheduleKind.Rest -> WorkoutType.RestDay.label()
            ScheduleKind.None -> stringResource(R.string.pulse_fitness_schedule_no_plan)
        }
    val focus = classification?.focus?.takeIf { kind == ScheduleKind.Workout && it.isNotBlank() }
    val summary = focus?.let { stringResource(R.string.pulse_fitness_schedule_day, title, it) } ?: title
    val description = stringResource(R.string.pulse_fitness_schedule_edit_day, dayName, summary)
    val todayLabel = stringResource(R.string.pulse_fitness_today)
    val editLabel = stringResource(R.string.pulse_fitness_schedule_edit)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .95f else 1f, tween(PulseTokens.MOTION_MILLIS), label = "schedulePress")

    Column(
        modifier
            .heightIn(min = 144.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.clip(shape)
            .then(
                when (kind) {
                    ScheduleKind.Workout -> Modifier.background(colors.done)
                    ScheduleKind.Rest -> Modifier.background(colors.chip)
                    ScheduleKind.None ->
                        Modifier.drawBehind {
                            val stroke = 2.dp.toPx()
                            val radius = 28.dp.toPx() - stroke / 2
                            drawRoundRect(
                                divider,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(radius),
                                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                            )
                        }
                },
            ).then(if (isToday) Modifier.border(2.5.dp, colors.today, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                onClickLabel = editLabel,
                role = Role.Button,
                onClick = onClick,
            ).clearAndSetSemantics {
                contentDescription = description
                if (isToday) stateDescription = todayLabel
            }.alpha(if (enabled) 1f else .6f)
            .padding(PulseSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(PulseSpacing.compact),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PulseText(
                shortDayName,
                Modifier.weight(1f),
                style = PulseTextStyle.Label,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isToday) {
                PulseText(
                    todayLabel,
                    Modifier
                        .clip(PulseShapes.pill)
                        .background(colors.gradient)
                        .padding(horizontal = PulseSpacing.small, vertical = 2.dp),
                    style = PulseTextStyle.Caption,
                    color = Color.White,
                )
            }
        }
        ScheduleKindBadge(kind, colors)
        Spacer(Modifier.weight(1f))
        Column {
            PulseText(
                title,
                style = PulseTextStyle.Title3,
                color = if (kind == ScheduleKind.None) colors.muted else Color.Unspecified,
            )
            focus?.let {
                PulseText(it, style = PulseTextStyle.Body2, color = colors.muted)
            }
        }
    }
}

@Composable
private fun ScheduleKindBadge(
    kind: ScheduleKind,
    colors: ScheduleColors,
) {
    val (icon, tint) =
        when (kind) {
            ScheduleKind.Workout -> R.drawable.fitness_schedule_workout to Color.White
            ScheduleKind.Rest -> R.drawable.fitness_schedule_rest to colors.muted
            ScheduleKind.None -> R.drawable.fitness_schedule_add to colors.muted
        }
    Box(
        Modifier
            .size(40.dp)
            .clip(PulseShapes.control)
            .then(
                when (kind) {
                    ScheduleKind.Workout -> Modifier.background(colors.gradient)
                    ScheduleKind.Rest -> Modifier.background(PulseTheme.colors.surface)
                    ScheduleKind.None -> Modifier.background(colors.chip)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = tint)
    }
}
