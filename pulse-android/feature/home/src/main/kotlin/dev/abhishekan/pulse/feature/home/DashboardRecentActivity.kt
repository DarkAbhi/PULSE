package dev.abhishekan.pulse.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.RecentActivityEntry
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.core.ui.patterns.PulseLoadingState
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import dev.abhishekan.pulse.core.ui.R as UiR

@Composable
internal fun DashboardRecentActivity(
    entries: List<RecentActivityEntry>?,
    loading: Boolean,
    onRetry: () -> Unit,
) {
    val zone by UserTimeZone.changes.collectAsStateWithLifecycle()
    val currency by UserCurrency.changes.collectAsStateWithLifecycle()
    val today = LocalDate.now(zone)
    val locale = LocalLocale.current.platformLocale
    val visible = entries.orEmpty().mapNotNull { event -> activityPresentation[event.kind]?.let { event to it } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseText(
            stringResource(R.string.pulse_home_recent_activity),
            Modifier.semantics { heading() },
            PulseTextStyle.Heading2,
        )
        PulseCard(Modifier.fillMaxWidth()) {
            when {
                entries == null && loading ->
                    PulseLoadingState(stringResource(R.string.pulse_home_loading_recent_activity))
                entries == null ->
                    PulseErrorState(stringResource(R.string.pulse_home_recent_activity_error), onRetry)
                visible.isEmpty() ->
                    PulseText(stringResource(R.string.pulse_home_recent_activity_empty), color = PulseTheme.colors.textSecondary)
                else ->
                    visible.forEachIndexed { index, (entry, presentation) ->
                        if (index > 0) {
                            PulseHorizontalDivider(Modifier.padding(start = PulseTokens.target + PulseTokens.medium))
                        }
                        val recorded = entry.recordedAt.atZone(zone).toLocalDate()
                        val date =
                            when (recorded) {
                                today -> stringResource(R.string.pulse_home_activity_today)
                                today.minusDays(1) -> stringResource(R.string.pulse_home_activity_yesterday)
                                else -> calendarLabel(recorded, today, locale, weekday = true)
                            }
                        DashboardActivityRow(
                            stringResource(presentation.title),
                            entry.description(recorded, zone, currency, locale),
                            date,
                            presentation.icon,
                        )
                    }
            }
        }
    }
}

@Composable
private fun DashboardActivityRow(
    title: String,
    description: String,
    date: String,
    icon: Int,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val dateBesideText = maxWidth / LocalDensity.current.fontScale >= 500.dp
        val colors = PulseTheme.colors
        Row(
            Modifier.fillMaxWidth().padding(vertical = PulseTokens.tiny),
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(PulseTokens.target).background(colors.primarySoft, PulseTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), null, Modifier.size(24.dp), tint = colors.action)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                PulseText(title, style = PulseTextStyle.Label)
                if (description.isNotBlank()) {
                    PulseText(description, style = PulseTextStyle.Body3, color = colors.textSecondary)
                }
                if (!dateBesideText) PulseText(date, style = PulseTextStyle.Caption, color = colors.textSecondary)
            }
            if (dateBesideText) PulseText(date, style = PulseTextStyle.Caption, color = colors.textSecondary)
        }
    }
}

private fun calendarLabel(
    date: LocalDate,
    reference: LocalDate,
    locale: Locale,
    weekday: Boolean = false,
): String {
    val pattern = (if (weekday) "EEE, " else "") + "d MMM" + if (date.year != reference.year) " yyyy" else ""
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}

private fun RecentActivityEntry.description(
    recorded: LocalDate,
    zone: ZoneId,
    currency: String,
    locale: Locale,
): String {
    val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 3 }

    fun detail(key: String) = details[key]?.takeIf(String::isNotBlank)

    fun numeric(key: String) = detail(key)?.toBigDecimalOrNull()?.let(number::format)

    fun unit(
        key: String,
        suffix: String,
    ) = numeric(key)?.let { "$it $suffix" }

    fun count(
        key: String,
        label: String,
    ) = detail(key)?.toLongOrNull()?.let { "${number.format(it)} $label${if (it == 1L) "" else "s"}" }

    fun parts(vararg values: String?) = values.filterNotNull().filter(String::isNotBlank).joinToString(" · ")

    val name = detail("name")
    val amount = detail("amount")?.toBigDecimalOrNull()?.let { UserCurrency.format(it, currency) }
    val date = detail("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val occurred =
        detail("occurred_at")?.let { runCatching { Instant.parse(it).atZone(zone).toLocalDate() }.getOrNull() } ?: date
    val sourceDate = occurred?.takeUnless { it == recorded }?.let { calendarLabel(it, recorded, locale) }
    val duration =
        detail("duration_seconds")
            ?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0 }
            ?.toLong()
            ?.let(::activityDuration)
    return when (kind) {
        "fuel_fillup" -> parts(name, unit("quantity", "L"), amount, sourceDate)
        "air_fill" -> parts(name, unit("odometer_km", "km"), sourceDate)
        "maintenance" -> parts(name, detail("description"), amount, sourceDate)
        "maintenance_attachment" -> parts(name, detail("description"))
        "reminder_configured" -> {
            val schedule =
                when (detail("schedule_kind")) {
                    "engine_oil" -> "Engine oil"
                    "general_service" -> "General service"
                    "tire_pressure" -> "Tire pressure"
                    else -> null
                }
            val interval = detail("interval_unit")?.let { count("interval_duration", if (it == "days") "day" else "month") }
            val frequency = listOfNotNull(unit("interval_km", "km"), interval).joinToString(" or ")
            parts(name, schedule, frequency.takeIf(String::isNotBlank)?.let { "Every $it" })
        }
        "meal_added" -> parts(name, detail("meal_time"), sourceDate?.let { "Planned for $it" })
        "meal_eaten" -> parts(name, detail("meal_time"))
        "meal_time_added" -> parts(name, detail("start_time")?.let { start -> detail("end_time")?.let { "$start–$it" } })
        "workout_logged" -> parts(count("exercise_count", "exercise"), count("set_count", "set"), sourceDate)
        "workout_synced" -> {
            val distance = detail("distance_meters")?.toDoubleOrNull()?.takeIf(Double::isFinite)
            parts(name, duration, distance?.let { "${number.format(it / 1000)} km" }, sourceDate)
        }
        "exercise_added" -> parts(name, sourceDate)
        "sleep_synced" -> parts(duration?.let { "$it asleep" }, sourceDate)
        "daily_activity_synced" ->
            parts(
                count("steps", "step"),
                unit("move_calories", "kcal moved"),
                unit("exercise_minutes", "min exercise"),
                sourceDate,
            )
        "transaction_logged" -> parts(name, listOfNotNull(amount, detail("transaction_type")).joinToString(" "), sourceDate)
        "subscription_added" -> parts(name, amount?.let { "$it/${if (detail("billing_cycle") == "yearly") "year" else "month"}" })
        "budget_added" -> parts(name, amount?.let { "$it/month" })
        "obligation_added" -> parts(name, amount, detail("due_day")?.let { "Due on day $it" })
        "planned_purchase_added" -> parts(name, amount, date?.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)))
        "account_added" -> parts(name, if (detail("account_type") == "credit_card") "Credit card" else "Bank account")
        "category_added" -> parts(detail("parent_name"), name)
        "subscription_payment_linked" -> parts(name, amount)
        else -> name.orEmpty()
    }
}

private fun activityDuration(seconds: Long): String =
    buildList {
        val hours = seconds / 3600
        val minutes = seconds / 60 % 60
        if (hours > 0) add("${hours}h")
        if (minutes > 0) add("${minutes}m")
        if (hours == 0L && minutes == 0L) add("${seconds}s")
    }.joinToString(" ")

private data class ActivityPresentation(
    val title: Int,
    val icon: Int,
)

private val activityPresentation =
    mapOf(
        "fuel_fillup" to ActivityPresentation(R.string.pulse_home_activity_fuel_fillup, UiR.drawable.pulse_garage),
        "air_fill" to ActivityPresentation(R.string.pulse_home_activity_air_fill, UiR.drawable.pulse_garage),
        "maintenance" to ActivityPresentation(R.string.pulse_home_activity_maintenance, UiR.drawable.pulse_garage),
        "vehicle_added" to ActivityPresentation(R.string.pulse_home_activity_vehicle_added, UiR.drawable.pulse_garage),
        "reminder_configured" to ActivityPresentation(R.string.pulse_home_activity_reminder_configured, UiR.drawable.pulse_bell),
        "maintenance_attachment" to ActivityPresentation(R.string.pulse_home_activity_maintenance_attachment, UiR.drawable.pulse_garage),
        "meal_added" to ActivityPresentation(R.string.pulse_home_activity_meal_added, UiR.drawable.pulse_meals),
        "meal_eaten" to ActivityPresentation(R.string.pulse_home_activity_meal_eaten, UiR.drawable.pulse_meals),
        "meal_time_added" to ActivityPresentation(R.string.pulse_home_activity_meal_time_added, UiR.drawable.pulse_meals),
        "workout_logged" to ActivityPresentation(R.string.pulse_home_activity_workout_logged, UiR.drawable.pulse_fitness),
        "workout_synced" to ActivityPresentation(R.string.pulse_home_activity_workout_synced, UiR.drawable.pulse_fitness),
        "exercise_added" to ActivityPresentation(R.string.pulse_home_activity_exercise_added, UiR.drawable.pulse_fitness),
        "sleep_synced" to ActivityPresentation(R.string.pulse_home_activity_sleep_synced, UiR.drawable.pulse_fitness),
        "daily_activity_synced" to ActivityPresentation(R.string.pulse_home_activity_daily_activity_synced, UiR.drawable.pulse_fitness),
        "transaction_logged" to ActivityPresentation(R.string.pulse_home_activity_transaction_logged, UiR.drawable.pulse_horizon),
        "statement_imported" to ActivityPresentation(R.string.pulse_home_activity_statement_imported, UiR.drawable.pulse_horizon),
        "subscription_added" to ActivityPresentation(R.string.pulse_home_activity_subscription_added, UiR.drawable.pulse_horizon),
        "subscription_payment_linked" to
            ActivityPresentation(R.string.pulse_home_activity_subscription_payment_linked, UiR.drawable.pulse_horizon),
        "budget_added" to ActivityPresentation(R.string.pulse_home_activity_budget_added, UiR.drawable.pulse_horizon),
        "obligation_added" to ActivityPresentation(R.string.pulse_home_activity_obligation_added, UiR.drawable.pulse_horizon),
        "planned_purchase_added" to ActivityPresentation(R.string.pulse_home_activity_planned_purchase_added, UiR.drawable.pulse_horizon),
        "account_added" to ActivityPresentation(R.string.pulse_home_activity_account_added, UiR.drawable.pulse_horizon),
        "account_archived" to ActivityPresentation(R.string.pulse_home_activity_account_archived, UiR.drawable.pulse_horizon),
        "account_restored" to ActivityPresentation(R.string.pulse_home_activity_account_restored, UiR.drawable.pulse_horizon),
        "category_added" to ActivityPresentation(R.string.pulse_home_activity_category_added, UiR.drawable.pulse_horizon),
    )
