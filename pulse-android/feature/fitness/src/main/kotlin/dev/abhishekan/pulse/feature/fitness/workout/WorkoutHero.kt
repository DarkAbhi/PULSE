package dev.abhishekan.pulse.feature.fitness.workout

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abhishekan.pulse.core.domain.model.WorkoutInfo
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.shared.label
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun WorkoutHero(
    info: WorkoutInfo,
    timeZone: ZoneId,
    enabled: Boolean,
    onEdit: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val date = (info.start ?: info.createdAt).atZone(timeZone)
    val day = date.format(DateTimeFormatter.ofPattern("d", locale))
    val weekday = date.format(DateTimeFormatter.ofPattern("EEEE", locale))
    val monthYear = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    val shape = RoundedCornerShape(40.dp)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .shadow(PulseSpacing.small, shape, spotColor = Color(0xFF3A2CD9))
            .clip(shape)
            .background(
                Brush.linearGradient(0f to Color(0xFF3A2CD9), 0.55f to Color(0xFF7B2CE0), 1f to Color(0xFFD41FB0)),
            ),
    ) {
        val wide = maxWidth >= 700.dp
        Box(Modifier.matchParentSize()) {
            WorkoutGlyph(
                WorkoutSymbol.Weight,
                Color.White.copy(alpha = 0.10f),
                Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = (-10).dp).size(260.dp),
            )
        }
        Column(
            Modifier.padding(if (wide) 32.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(PulseSpacing.comfortable),
        ) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(PulseSpacing.small),
            ) {
                FlowRow(
                    Modifier.align(Alignment.CenterVertically).padding(end = PulseSpacing.small),
                    horizontalArrangement = Arrangement.spacedBy(PulseSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(PulseSpacing.small),
                ) {
                    WorkoutHeroTag(stringResource(R.string.pulse_fitness_workout_gym_visit), WorkoutSymbol.Weight)
                    if (info.imported) {
                        WorkoutHeroTag(stringResource(R.string.pulse_fitness_workout_watch_sync), WorkoutSymbol.Watch, inverted = true)
                    }
                    info.classification.type?.let { type ->
                        val label = type.label()
                        val focus = info.classification.focus.takeIf { it.isNotBlank() }
                        WorkoutHeroTag(listOfNotNull(label, focus).joinToString(" · "))
                    }
                }
                PulseButton(
                    stringResource(R.string.pulse_fitness_edit_workout),
                    onEdit,
                    Modifier.align(Alignment.CenterVertically),
                    enabled = enabled,
                    variant = PulseButtonVariant.Secondary,
                    leadingIcon = { WorkoutGlyph(WorkoutSymbol.Edit, LocalContentColor.current, Modifier.size(16.dp)) },
                )
            }
            Row(
                Modifier.semantics(mergeDescendants = true) { heading() },
                horizontalArrangement = Arrangement.spacedBy(PulseSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val daySize = if (wide) 104.sp else 64.sp
                PulseText(
                    day,
                    style =
                        PulseTheme.typography.displayXL.copy(
                            fontSize = daySize,
                            lineHeight = daySize,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    color = Color.White,
                )
                Column {
                    PulseText(weekday, style = PulseTextStyle.Heading2, color = Color.White)
                    PulseText(monthYear, style = PulseTextStyle.Title3, color = Color.White.copy(alpha = 0.90f))
                }
            }
            WorkoutHeroMetrics(info, timeZone, wide)
        }
    }
}

@Composable
private fun WorkoutHeroTag(
    label: String,
    symbol: WorkoutSymbol? = null,
    inverted: Boolean = false,
) {
    val shape = PulseTheme.shapes.pill
    val tint = if (inverted) Color(0xFF4A2BD0) else Color.White
    Row(
        Modifier
            .background(Color.White.copy(alpha = if (inverted) 0.95f else 0.20f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.40f), shape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(PulseSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        symbol?.let { WorkoutGlyph(it, tint, Modifier.size(16.dp)) }
        PulseText(label, style = PulseTextStyle.Label2, color = tint)
    }
}

@Composable
private fun WorkoutHeroMetrics(
    info: WorkoutInfo,
    timeZone: ZoneId,
    wide: Boolean,
) {
    if (info.start == null && info.end == null && info.durationSeconds == null && info.calories == null) return
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val time =
        remember(locale, is24Hour, timeZone) {
            val pattern = DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm")
            DateTimeFormatter.ofPattern(pattern, locale).withZone(timeZone)
        }
    val metrics =
        listOf(
            HeroMetric(R.string.pulse_fitness_workout_start, WorkoutSymbol.Clock, info.start?.let(time::format)),
            HeroMetric(R.string.pulse_fitness_workout_end, WorkoutSymbol.Flag, info.end?.let(time::format)),
            HeroMetric(
                R.string.pulse_fitness_workout_duration,
                WorkoutSymbol.Timer,
                info.durationSeconds?.let { formatWorkoutDuration(it, compact = true).asString() },
            ),
            HeroMetric(
                R.string.pulse_fitness_workout_calories,
                WorkoutSymbol.Energy,
                info.calories?.let { stringResource(R.string.pulse_fitness_workout_kcal, formatWorkoutNumber(it)) },
            ),
        )
    val shape = RoundedCornerShape(22.dp)
    val divider = Color.White.copy(alpha = 0.22f)
    Column(
        Modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, divider, shape),
    ) {
        metrics.chunked(if (wide) 4 else 2).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) HorizontalDivider(color = divider)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEachIndexed { index, metric ->
                    if (index > 0) VerticalDivider(color = divider)
                    Column(
                        Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(PulseSpacing.tiny),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PulseSpacing.tiny),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val labelColor = Color.White.copy(alpha = 0.88f)
                            WorkoutGlyph(metric.symbol, labelColor, Modifier.size(14.dp))
                            PulseText(stringResource(metric.label), style = PulseTextStyle.Caption, color = labelColor)
                        }
                        PulseText(
                            metric.value ?: stringResource(R.string.pulse_fitness_workout_missing_value),
                            style = PulseTextStyle.Title3,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

private data class HeroMetric(
    val label: Int,
    val symbol: WorkoutSymbol,
    val value: String?,
)
