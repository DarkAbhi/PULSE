package dev.abhishekan.pulse.feature.mealplan
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.domain.model.PlannedMeal
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseExtendedFloatingActionButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val mealDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")
internal val mealClock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
internal fun MealAgenda(
    date: LocalDate,
    today: LocalDate,
    meals: List<PlannedMeal>,
    enabled: Boolean,
    onEdit: (PlannedMeal) -> Unit,
    onToggle: (PlannedMeal) -> Unit,
    onDelete: (PlannedMeal) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val columns = if (maxWidth / LocalDensity.current.fontScale >= 680.dp) 2 else 1
        val compact = maxWidth < 600.dp
        val compactCard = (maxWidth - 24.dp * (columns - 1)) / columns < 400.dp || LocalDensity.current.fontScale > 1.3f
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (meals.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = if (compact) 8.dp else 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Image(
                        painterResource(R.drawable.meal_empty),
                        null,
                        Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = if (compact) 180.dp else 320.dp),
                        contentScale = ContentScale.Fit,
                    )
                    PulseText(
                        stringResource(if (date == today) R.string.pulse_mealplan_empty_today else R.string.pulse_mealplan_empty_day),
                        Modifier.fillMaxWidth().semantics { heading() },
                        style = if (compact) PulseTextStyle.Heading2 else PulseTextStyle.Heading1,
                        textAlign = TextAlign.Center,
                    )
                    PulseText(
                        stringResource(R.string.pulse_mealplan_empty_help),
                        Modifier.fillMaxWidth(),
                        style = PulseTextStyle.Body,
                        color = PulseTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val sorted = meals.sortedWith(compareBy<PlannedMeal> { it.start == null }.thenBy { it.start }.thenBy { it.id })
                // Deleted meal times can leave different saved ranges under Other meals.
                val groups = sorted.groupBy { Triple(it.timeId, it.start, it.end) }.values.toList()
                groups.chunked(columns).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().then(if (columns == 2) Modifier.height(IntrinsicSize.Min) else Modifier),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        row.forEach { group ->
                            MealIllustratedCard(
                                group,
                                compactCard,
                                enabled,
                                onEdit,
                                onToggle,
                                onDelete,
                                Modifier.weight(1f).then(if (columns == 2) Modifier.fillMaxHeight() else Modifier),
                            )
                        }
                        if (row.size < columns) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MealIllustratedCard(
    meals: List<PlannedMeal>,
    compact: Boolean,
    enabled: Boolean,
    edit: (PlannedMeal) -> Unit,
    toggle: (PlannedMeal) -> Unit,
    delete: (PlannedMeal) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PulseTheme.colors
    val meal = meals.first()
    val hour = meal.start?.hour
    val artwork =
        when {
            hour == null -> R.drawable.meal_lunch
            hour < 11 -> R.drawable.meal_breakfast
            hour < 15 -> R.drawable.meal_lunch
            hour < 19 -> R.drawable.meal_snack
            else -> R.drawable.meal_dinner
        }
    val tint =
        when (artwork) {
            R.drawable.meal_breakfast -> colors.primary
            R.drawable.meal_snack -> colors.sage
            R.drawable.meal_dinner -> colors.lavender
            else -> colors.coral
        }
    Surface(
        modifier.testTag("meal-card-${meal.id}"),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(tint.copy(alpha = 0.14f), Color.Transparent)),
                ).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (LocalDensity.current.fontScale <= 1.5f) {
                Image(painterResource(artwork), null, Modifier.size(if (compact) 80.dp else 132.dp), contentScale = ContentScale.Fit)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PulseText(meal.timeName ?: stringResource(R.string.pulse_mealplan_other_meals), style = PulseTextStyle.Title)
                val start = meal.start
                val end = meal.end
                if (start != null && end != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.meal_clock), null, Modifier.size(16.dp), tint = colors.textSecondary)
                        PulseText(
                            stringResource(
                                if (end <
                                    start
                                ) {
                                    R.string.pulse_mealplan_overnight_time_range
                                } else {
                                    R.string.pulse_mealplan_time_range
                                },
                                start.format(DateTimeFormatter.ofPattern("h:mm a")),
                                end.format(DateTimeFormatter.ofPattern("h:mm a")),
                            ),
                            style = PulseTextStyle.Body3,
                            color = colors.textSecondary,
                        )
                    }
                }
                meals.forEach { item ->
                    val consumptionLabel =
                        stringResource(
                            R.string.pulse_mealplan_meal_consumption_label,
                            stringResource(if (item.consumed) R.string.pulse_mealplan_consumed else R.string.pulse_mealplan_planned),
                            item.name,
                        )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulseCheckbox(
                            item.consumed,
                            { toggle(item) },
                            item.name,
                            Modifier.weight(1f).semantics { contentDescription = consumptionLabel },
                            enabled = enabled,
                        )
                        PulseButton(
                            stringResource(R.string.pulse_mealplan_edit_1_s, item.name),
                            { edit(item) },
                            Modifier
                                .background(colors.surfaceMuted.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            enabled = enabled,
                            style = PulseButtonStyle.Icon,
                        ) {
                            Icon(painterResource(R.drawable.meal_edit), null)
                        }
                        PulseButton(
                            stringResource(R.string.pulse_mealplan_delete_1_s, item.name),
                            { delete(item) },
                            enabled = enabled,
                            style = PulseButtonStyle.Icon,
                        ) {
                            Icon(painterResource(R.drawable.meal_delete), null, tint = colors.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MealTimes(
    times: List<MealTime>,
    enabled: Boolean,
    onDelete: (MealTime) -> Unit,
) {
    PulseText(
        stringResource(R.string.pulse_mealplan_your_daily_rhythm),
        Modifier.semantics {
            heading()
        },
        PulseTextStyle.Display,
    )
    PulseText(
        stringResource(R.string.pulse_mealplan_default_and_custom_meal_times_meals_can_also_be),
    )
    if (times.isEmpty()) {
        PulseEmptyState(
            stringResource(R.string.pulse_mealplan_no_meal_times_available),
            stringResource(R.string.pulse_mealplan_create_a_custom_time_or_add_meals_without_one),
        )
    }
    times.forEach { time ->
        PulseCard(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    painterResource(
                        when (time.start.hour) {
                            in 5..10 -> R.drawable.meal_sun
                            in 11..15 -> R.drawable.meal_sun
                            in 16..18 -> R.drawable.meal_sunset
                            else -> R.drawable.meal_moon
                        },
                    ),
                    null,
                    Modifier.background(PulseTheme.colors.primarySoft, RoundedCornerShape(12.dp)).padding(12.dp).size(24.dp),
                    tint = PulseTheme.colors.primary,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PulseText(time.name, style = PulseTextStyle.Title)
                    PulseText(
                        stringResource(
                            if (time.end <
                                time.start
                            ) {
                                R.string.pulse_mealplan_overnight_time_range
                            } else {
                                R.string.pulse_mealplan_time_range
                            },
                            time.start.format(mealClock),
                            time.end.format(mealClock),
                        ),
                    )
                    if (time.isDefault) {
                        PulseText(
                            stringResource(R.string.pulse_mealplan_default_protected),
                            style = PulseTextStyle.Label,
                        )
                    } else {
                        PulseButton(
                            stringResource(
                                R.string.pulse_mealplan_delete_time_1_s,
                                time.name,
                            ),
                            {
                                onDelete(time)
                            },
                            enabled = enabled,
                            style = PulseButtonStyle.Text,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MealTimesHeader(
    onBack: () -> Unit,
    enabled: Boolean,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PulseButton(stringResource(R.string.pulse_mealplan_back_to_planner), onBack, enabled = enabled, style = PulseButtonStyle.Icon) {
            Icon(painterResource(dev.abhishekan.pulse.core.ui.R.drawable.pulse_back), null)
        }
        PulseText(
            stringResource(R.string.pulse_mealplan_meal_times),
            Modifier.weight(1f).semantics { heading() },
            style = PulseTextStyle.Title,
        )
    }
}

@Composable
internal fun AddMealTimeButton(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PulseExtendedFloatingActionButton(
        onClick = onAdd,
        modifier = modifier.widthIn(max = 280.dp),
        icon = { Icon(painterResource(R.drawable.meal_add), null, Modifier.size(24.dp)) },
        text = {
            PulseText(
                stringResource(R.string.pulse_mealplan_add_custom_meal_time),
                style = PulseTextStyle.Label,
            )
        },
    )
}
