package dev.abhishekan.pulse.feature.garage

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

@Composable
internal fun reminderPageTitle(page: String): String =
    if (page == "list") {
        stringResource(R.string.pulse_garage_reminder_settings)
    } else {
        stringResource(R.string.pulse_garage_reminder_page_title, garageOption(page))
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
    open: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PulseText(
            stringResource(R.string.pulse_garage_reminder_choose),
            style = PulseTextStyle.Body2,
            color = PulseTheme.colors.textSecondary,
        )
        settings.forEach { setting ->
            ReminderNavigationRow(
                garageOption(setting.kind),
                scheduleSourceLabel(setting.activeSource),
                "reminder-task-${setting.kind}",
                enabled,
            ) { open(setting.kind) }
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
