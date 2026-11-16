package dev.abhishekan.pulse.feature.garage.maintenance.reminders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MaintenanceSchedule
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.shared.garageOption

@Composable
internal fun reminderPageTitle(page: ReminderPage): String =
    if (page == ReminderPage.List) {
        stringResource(R.string.pulse_garage_reminder_settings)
    } else {
        stringResource(R.string.pulse_garage_reminder_page_title, garageOption(page.savedKey))
    }

@Composable
internal fun MaintenanceReminderEntry(
    enabled: Boolean,
    open: () -> Unit,
) {
    ReminderNavigationRow(
        stringResource(R.string.pulse_garage_reminder_settings),
        stringResource(R.string.pulse_garage_reminder_settings_hint),
        "maintenance-reminders",
        enabled,
        open,
    )
}

@Composable
internal fun MaintenanceReminderTasks(
    settings: List<MaintenanceSchedule>,
    enabled: Boolean,
    open: (ReminderPage) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PulseText(
            stringResource(R.string.pulse_garage_reminder_choose),
            style = PulseTextStyle.Body2,
            color = PulseTheme.colors.textSecondary,
        )
        settings.forEach { setting ->
            val page = ReminderPage.forSchedule(setting.kind) ?: return@forEach
            ReminderNavigationRow(
                garageOption(setting.kind),
                scheduleSourceLabel(setting.activeSource),
                "reminder-task-${setting.kind}",
                enabled,
            ) { open(page) }
        }
    }
}

@Composable
private fun ReminderNavigationRow(
    title: String,
    subtitle: String,
    tag: String,
    enabled: Boolean,
    open: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth().testTag(tag).clickable(enabled = enabled, role = Role.Button, onClick = open)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PulseText(title, style = PulseTextStyle.Title3)
                PulseText(subtitle, style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary)
            }
            Icon(painterResource(R.drawable.garage_chevron_right), null, Modifier.size(24.dp), tint = PulseTheme.colors.textSecondary)
        }
    }
}
