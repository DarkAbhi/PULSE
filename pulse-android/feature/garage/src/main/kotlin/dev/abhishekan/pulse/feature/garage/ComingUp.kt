package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.UpcomingMaintenance
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseLinearProgressIndicator
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun ComingUp(
    data: VehicleHistory,
    enabled: Boolean,
    openCare: () -> Unit,
    openAir: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PulseText(stringResource(R.string.pulse_garage_coming_up), Modifier.semantics { heading() }, PulseTextStyle.Title2)
        data.upcoming.forEach { item ->
            val lastAt = item.lastAt
            val progress = item.progress
            val pressure = item.kind == "tire_pressure"
            val title =
                stringResource(
                    when (item.kind) {
                        "engine_oil" -> R.string.pulse_garage_upcoming_oil
                        "tire_pressure" -> R.string.pulse_garage_upcoming_pressure
                        else -> R.string.pulse_garage_upcoming_service
                    },
                )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = enabled,
                        onClickLabel = title,
                        onClick = if (pressure) openAir else openCare,
                    ).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).background(PulseTheme.colors.surfaceMuted, PulseTheme.shapes.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(if (pressure) R.drawable.garage_air else R.drawable.garage_service),
                        null,
                        Modifier.size(24.dp),
                        tint = PulseTheme.colors.action,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PulseText(title, style = PulseTextStyle.Title3)
                    PulseText(scheduleSourceLabel(item.source), style = PulseTextStyle.Caption, color = PulseTheme.colors.textSecondary)
                    PulseText(
                        upcomingText(item),
                        style = PulseTextStyle.Body2,
                        color = if (item.status == "overdue") PulseTheme.colors.error else PulseTheme.colors.textSecondary,
                    )
                    if (item.distanceUnavailable &&
                        item.status != "needs_odometer"
                    ) {
                        PulseText(stringResource(R.string.pulse_garage_schedule_needs_odometer), style = PulseTextStyle.Caption)
                    }
                    if (pressure && lastAt != null) {
                        PulseText(
                            stringResource(R.string.pulse_garage_upcoming_last_air, garageDate(lastAt)),
                            style = PulseTextStyle.Caption,
                            color = PulseTheme.colors.textSecondary,
                        )
                    }
                    if (!pressure && progress != null) {
                        PulseLinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = PulseTheme.colors.action,
                            trackColor = PulseTheme.colors.divider,
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                        )
                    }
                }
                Icon(painterResource(R.drawable.garage_chevron_right), null, Modifier.size(20.dp), tint = PulseTheme.colors.textSecondary)
            }
        }
        PulseText(
            stringResource(R.string.pulse_garage_upcoming_estimates),
            style = PulseTextStyle.Caption,
            color = PulseTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun upcomingText(item: UpcomingMaintenance): String {
    if (item.status ==
        "no_history"
    ) {
        return stringResource(
            if (item.kind ==
                "tire_pressure"
            ) {
                R.string.pulse_garage_upcoming_no_air
            } else {
                R.string.pulse_garage_upcoming_no_history
            },
        )
    }
    if (item.status == "needs_odometer")return stringResource(R.string.pulse_garage_schedule_needs_odometer)
    val days = item.remainingDays
    val date = item.dueDate?.let { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
    val time =
        when {
            days != null && days < 0 -> pluralStringResource(R.plurals.pulse_garage_upcoming_overdue_days, -days, -days)
            days == 0 -> stringResource(R.string.pulse_garage_upcoming_today)
            days != null -> pluralStringResource(R.plurals.pulse_garage_upcoming_days, days, days)
            else -> ""
        }
    val distance = item.remainingKm ?: return time
    val km = decimal(distance.abs().setScale(0, java.math.RoundingMode.CEILING))
    if (days != null && days <= 0) return time
    val text =
        stringResource(
            if (distance.signum() <
                0
            ) {
                R.string.pulse_garage_upcoming_overdue_km
            } else {
                R.string.pulse_garage_upcoming_km
            },
            km,
        )
    return if (date == null) text else "$text · ${stringResource(R.string.pulse_garage_upcoming_by, date)}"
}
