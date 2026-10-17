package dev.abhishekan.pulse.feature.home

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

internal fun formatMoney(currency: String, amount: BigDecimal): String =
    "$currency ${amount.setScale(maxOf(2, amount.scale())).toPlainString()}"

@Composable
internal fun HorizonCard(section: Section<Horizon>, onRetry: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth(), highlighted = section is Section.Content) {
        PulseText("FINANCIAL HORIZON", style = PulseTextStyle.Label)
        when (section) {
            Section.Loading -> PulseLoadingState("Loading your horizon")
            Section.Empty -> PulseEmptyState("No summary yet", "Your financial summary will appear here when available.")
            is Section.Error -> PulseErrorState(section.failure.message, onRetry)
            is Section.Content -> {
                val data = section.value
                PulseText("Your room to move", style = PulseTextStyle.Title)
                PulseText(formatMoney(data.currency, data.remainingAmount), style = PulseTextStyle.Metric)
                PulseText(if (data.isOvercommitted) "Overcommitted this month" else "Uncommitted this month")
                Spacer(Modifier.height(PulseTokens.small))
                PulseText("Starting pool", style = PulseTextStyle.Label)
                PulseText(formatMoney(data.currency, data.baseAmount))
                PulseText("Fixed obligations", style = PulseTextStyle.Label)
                PulseText(formatMoney(data.currency, data.deductions))
            }
        }
    }
}

@Composable
internal fun WorkoutCard(section: Section<WorkoutToday>, onRetry: () -> Unit, onFitness: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("FITNESS · TODAY", style = PulseTextStyle.Label, color = MaterialTheme.colorScheme.primary)
        when (section) {
            Section.Loading -> PulseLoadingState("Loading today’s workout")
            Section.Empty -> PulseEmptyState("Room for movement", "No workout recorded today.")
            is Section.Error -> PulseErrorState(section.failure.message, onRetry)
            is Section.Content -> {
                PulseText("Workout recorded", style = PulseTextStyle.Title)
                PulseText("A little effort, a little momentum. You showed up today.")
            }
        }
        PulseTextButton("Open Fitness", onFitness)
        PulseText("Today follows India time (Asia/Kolkata).", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun NotificationsCard(section: Section<List<Notification>>, onRetry: () -> Unit, onOpen: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("Recent notifications", style = PulseTextStyle.Title)
        NotificationSection(section, onRetry, preview = true)
        PulseTextButton("View recent updates", onOpen)
    }
}

@Composable
fun NotificationSection(section: Section<List<Notification>>, onRetry: () -> Unit, preview: Boolean = false) {
    when (section) {
        Section.Loading -> PulseLoadingState("Loading notifications")
        Section.Empty -> PulseEmptyState("All caught up", "Your latest updates will appear here.")
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
internal fun SpacesCard(onFitness: () -> Unit) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("Your spaces", style = PulseTextStyle.Title)
        PulseTextButton("Explore Fitness", onFitness)
        listOf("Fitness" to "Workouts and movement", "Financial Horizon" to "Your monthly room to move",
            "Garage" to "Vehicles, fuel, and maintenance", "Meal Plan" to "Your weekly meals").forEach { (title, description) ->
            Column(Modifier.padding(vertical = PulseTokens.small)) {
                PulseText(title, style = PulseTextStyle.Label)
                PulseText(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
