package dev.abhishekan.pulse.feature.home
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.WorkoutToday
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun formatMoney(
    currency: String,
    amount: BigDecimal,
): UiText =
    uiText(
        R.string.pulse_home_money,
        currency,
        amount.setScale(maxOf(2, amount.scale())).toPlainString(),
    )

@Composable
internal fun HorizonCard(
    section: Section<Horizon>,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth(), highlighted = section is Section.Content) {
        PulseText(
            stringResource(R.string.pulse_home_financial_horizon),
            style = PulseTextStyle.Label,
        )
        when (section) {
            Section.Loading ->
                PulseLoadingState(
                    stringResource(R.string.pulse_home_loading_your_horizon),
                )
            Section.Empty ->
                PulseEmptyState(
                    stringResource(R.string.pulse_home_no_summary_yet),
                    stringResource(
                        R.string.pulse_home_your_financial_summary_will_appear_here_when_available,
                    ),
                )
            is Section.Error -> PulseErrorState(section.failure.message, onRetry)
            is Section.Content -> {
                val data = section.value
                PulseText(
                    stringResource(R.string.pulse_home_your_room_to_move),
                    style = PulseTextStyle.Title,
                )
                PulseText(
                    formatMoney(data.currency, data.remainingAmount).asString(),
                    style = PulseTextStyle.Metric,
                )
                PulseText(
                    if (data.isOvercommitted) {
                        stringResource(
                            R.string.pulse_home_overcommitted_this_month,
                        )
                    } else {
                        stringResource(R.string.pulse_home_uncommitted_this_month)
                    },
                )
                Spacer(Modifier.height(PulseTokens.small))
                PulseText(
                    stringResource(R.string.pulse_home_starting_pool),
                    style = PulseTextStyle.Label,
                )
                PulseText(formatMoney(data.currency, data.baseAmount).asString())
                PulseText(
                    stringResource(R.string.pulse_home_fixed_obligations),
                    style = PulseTextStyle.Label,
                )
                PulseText(formatMoney(data.currency, data.deductions).asString())
            }
        }
        PulseTextButton(stringResource(R.string.pulse_home_open_financial_horizon), onOpen)
    }
}

@Composable
internal fun WorkoutCard(
    section: Section<WorkoutToday>,
    onRetry: () -> Unit,
    onFitness: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_home_fitness_today),
            style = PulseTextStyle.Label,
            color = MaterialTheme.colorScheme.primary,
        )
        when (section) {
            Section.Loading ->
                PulseLoadingState(
                    stringResource(R.string.pulse_home_loading_today_s_workout),
                )
            Section.Empty ->
                PulseEmptyState(
                    stringResource(R.string.pulse_home_room_for_movement),
                    stringResource(R.string.pulse_home_no_workout_recorded_today),
                )
            is Section.Error -> PulseErrorState(section.failure.message, onRetry)
            is Section.Content -> {
                PulseText(
                    stringResource(R.string.pulse_home_workout_recorded),
                    style = PulseTextStyle.Title,
                )
                PulseText(
                    stringResource(
                        R.string.pulse_home_a_little_effort_a_little_momentum_you_showed_up,
                    ),
                )
            }
        }
        PulseTextButton(stringResource(R.string.pulse_home_open_fitness), onFitness)
        PulseText(
            stringResource(R.string.pulse_home_today_follows_india_time_asia_kolkata),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun NotificationsCard(
    section: Section<List<Notification>>,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_home_recent_notifications),
            style = PulseTextStyle.Title,
        )
        NotificationSection(section, onRetry, preview = true)
        PulseTextButton(stringResource(R.string.pulse_home_view_recent_updates), onOpen)
    }
}

@Composable
fun NotificationSection(
    section: Section<List<Notification>>,
    onRetry: () -> Unit,
    preview: Boolean = false,
) {
    when (section) {
        Section.Loading ->
            PulseLoadingState(
                stringResource(R.string.pulse_home_loading_notifications),
            )
        Section.Empty ->
            PulseEmptyState(
                stringResource(R.string.pulse_home_all_caught_up),
                stringResource(R.string.pulse_home_your_latest_updates_will_appear_here),
            )
        is Section.Error -> PulseErrorState(section.failure.message, onRetry)
        is Section.Content ->
            section.value.take(if (preview) 2 else 5).forEach { item ->
                Column(
                    Modifier.fillMaxWidth().padding(vertical = PulseTokens.small),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
                ) {
                    PulseText(item.title, style = PulseTextStyle.Label)
                    item.body?.let { PulseText(it) }
                    item.createdAt?.let {
                        PulseText(
                            DateTimeFormatter
                                .ofPattern(
                                    "d MMM · HH:mm",
                                ).withZone(ZoneId.of("Asia/Kolkata"))
                                .format(it),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
    }
}

@Composable
internal fun SpacesCard(
    onFitness: () -> Unit,
    onHorizon: () -> Unit,
    onGarage: () -> Unit,
    onMealPlan: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(stringResource(R.string.pulse_home_your_spaces), style = PulseTextStyle.Title)
        PulseTextButton(stringResource(R.string.pulse_home_explore_fitness), onFitness)
        PulseTextButton(stringResource(R.string.pulse_home_explore_financial_horizon), onHorizon)
        PulseTextButton(stringResource(R.string.pulse_home_explore_garage), onGarage)
        PulseTextButton(stringResource(R.string.pulse_home_explore_meal_plan), onMealPlan)
        listOf(
            stringResource(R.string.pulse_home_fitness) to
                stringResource(R.string.pulse_home_workouts_and_movement),
            stringResource(R.string.pulse_home_financial_horizon) to
                stringResource(R.string.pulse_home_your_monthly_room_to_move),
            stringResource(R.string.pulse_home_garage) to
                stringResource(R.string.pulse_home_vehicles_fuel_and_maintenance),
            stringResource(R.string.pulse_home_meal_plan) to
                stringResource(R.string.pulse_home_your_weekly_meals),
        ).forEach { (title, description) ->
            Column(Modifier.padding(vertical = PulseTokens.small)) {
                PulseText(title, style = PulseTextStyle.Label)
                PulseText(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
