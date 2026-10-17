package dev.abhishekan.pulse.feature.home

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.home.R

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.WorkoutToday
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun formatMoney(currency: String, amount: BigDecimal): UiText =
    uiText(R.string.pulse_home_money, currency, amount.setScale(maxOf(2, amount.scale())).toPlainString())

@Composable
internal fun HorizonCard(section: Section<Horizon>, onRetry: () -> Unit, onOpen: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth(), highlighted = section is Section.Content) {
        PulseText(uiText(R.string.pulse_home_financial_horizon), style = PulseTextStyle.Label)
        when (section) {
            Section.Loading -> PulseLoadingState(uiText(R.string.pulse_home_loading_your_horizon))
            Section.Empty -> PulseEmptyState(uiText(R.string.pulse_home_no_summary_yet), uiText(R.string.pulse_home_your_financial_summary_will_appear_here_when_available))
            is Section.Error -> PulseErrorState(section.failure.message, onRetry)
            is Section.Content -> {
                val data = section.value
                PulseText(uiText(R.string.pulse_home_your_room_to_move), style = PulseTextStyle.Title)
                PulseText(formatMoney(data.currency, data.remainingAmount), style = PulseTextStyle.Metric)
                PulseText(if (data.isOvercommitted) uiText(R.string.pulse_home_overcommitted_this_month) else uiText(R.string.pulse_home_uncommitted_this_month))
                Spacer(Modifier.height(PulseTokens.small))
                PulseText(uiText(R.string.pulse_home_starting_pool), style = PulseTextStyle.Label)
                PulseText(formatMoney(data.currency, data.baseAmount))
                PulseText(uiText(R.string.pulse_home_fixed_obligations), style = PulseTextStyle.Label)
                PulseText(formatMoney(data.currency, data.deductions))
            }
        }
        PulseTextButton(uiText(R.string.pulse_home_open_financial_horizon), onOpen)
    }
}

@Composable
internal fun WorkoutCard(section: Section<WorkoutToday>, onRetry: () -> Unit, onFitness: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(uiText(R.string.pulse_home_fitness_today), style = PulseTextStyle.Label, color = MaterialTheme.colorScheme.primary)
        when (section) {
            Section.Loading -> PulseLoadingState(uiText(R.string.pulse_home_loading_today_s_workout))
            Section.Empty -> PulseEmptyState(uiText(R.string.pulse_home_room_for_movement), uiText(R.string.pulse_home_no_workout_recorded_today))
            is Section.Error -> PulseErrorState(section.failure.message, onRetry)
            is Section.Content -> {
                PulseText(uiText(R.string.pulse_home_workout_recorded), style = PulseTextStyle.Title)
                PulseText(uiText(R.string.pulse_home_a_little_effort_a_little_momentum_you_showed_up))
            }
        }
        PulseTextButton(uiText(R.string.pulse_home_open_fitness), onFitness)
        PulseText(uiText(R.string.pulse_home_today_follows_india_time_asia_kolkata), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun NotificationsCard(section: Section<List<Notification>>, onRetry: () -> Unit, onOpen: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(uiText(R.string.pulse_home_recent_notifications), style = PulseTextStyle.Title)
        NotificationSection(section, onRetry, preview = true)
        PulseTextButton(uiText(R.string.pulse_home_view_recent_updates), onOpen)
    }
}

@Composable
fun NotificationSection(section: Section<List<Notification>>, onRetry: () -> Unit, preview: Boolean = false) {
    when (section) {
        Section.Loading -> PulseLoadingState(uiText(R.string.pulse_home_loading_notifications))
        Section.Empty -> PulseEmptyState(uiText(R.string.pulse_home_all_caught_up), uiText(R.string.pulse_home_your_latest_updates_will_appear_here))
        is Section.Error -> PulseErrorState(section.failure.message, onRetry)
        is Section.Content -> section.value.take(if (preview) 2 else 5).forEach { item ->
            Column(Modifier.fillMaxWidth().padding(vertical = PulseTokens.small), verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                PulseText(item.title, style = PulseTextStyle.Label)
                item.body?.let { PulseText(it) }
                item.createdAt?.let {
                    PulseText(DateTimeFormatter.ofPattern("d MMM · HH:mm").withZone(ZoneId.of("Asia/Kolkata")).format(it),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun SpacesCard(onFitness: () -> Unit, onHorizon: () -> Unit, onGarage: () -> Unit, onMealPlan: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(uiText(R.string.pulse_home_your_spaces), style = PulseTextStyle.Title)
        PulseTextButton(uiText(R.string.pulse_home_explore_fitness), onFitness)
        PulseTextButton(uiText(R.string.pulse_home_explore_financial_horizon), onHorizon)
        PulseTextButton(uiText(R.string.pulse_home_explore_garage), onGarage)
        PulseTextButton(uiText(R.string.pulse_home_explore_meal_plan), onMealPlan)
        listOf(uiText(R.string.pulse_home_fitness) to uiText(R.string.pulse_home_workouts_and_movement), uiText(R.string.pulse_home_financial_horizon) to uiText(R.string.pulse_home_your_monthly_room_to_move),
            uiText(R.string.pulse_home_garage) to uiText(R.string.pulse_home_vehicles_fuel_and_maintenance), uiText(R.string.pulse_home_meal_plan) to uiText(R.string.pulse_home_your_weekly_meals)).forEach { (title, description) ->
            Column(Modifier.padding(vertical = PulseTokens.small)) {
                PulseText(title, style = PulseTextStyle.Label)
                PulseText(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
