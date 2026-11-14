package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FuelFill
import dev.abhishekan.pulse.core.domain.model.FuelItem
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.pulseDescription
import dev.abhishekan.pulse.core.ui.uiText

@Composable
internal fun FuelHistoryHeader(
    count: Int,
    enabled: Boolean,
    add: () -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = PulseTokens.medium)) {
            PulseText(stringResource(R.string.pulse_garage_fuel_history), Modifier.semantics { heading() }, PulseTextStyle.Title2)
            PulseText(
                pluralStringResource(R.plurals.pulse_garage_fuel_count, count, count),
                style = PulseTextStyle.Caption,
                color = PulseTheme.colors.textSecondary,
            )
        }
        PulseButton(
            stringResource(R.string.pulse_garage_add_fuel_fill_up),
            add,
            enabled = enabled,
            leadingIcon = { Icon(painterResource(R.drawable.garage_add), null, Modifier.size(PulseTheme.spacing.comfortable)) },
        )
    }
}

@Composable
internal fun FuelHistoryGrid(
    fuel: List<FuelFill>,
    vehicle: Long,
    enabled: Boolean,
    edit: (GarageDraft) -> Unit,
    remove: (GarageChange.Delete, UiText) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 840.dp && LocalDensity.current.fontScale <= 1.3f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
            fuel.chunked(columns).forEach { entries ->
                Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                    entries.forEach { item ->
                        FuelHistoryCard(item, vehicle, enabled, edit, remove, Modifier.weight(1f))
                    }
                    if (entries.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun FuelHistoryCard(
    item: FuelFill,
    vehicle: Long,
    enabled: Boolean,
    edit: (GarageDraft) -> Unit,
    remove: (GarageChange.Delete, UiText) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PulseTheme.colors
    val label = stringResource(R.string.pulse_garage_fuel_fill_up_1_s, item.id)
    PulseCard(
        modifier.fillMaxWidth().testTag("garage-fuel-${item.id}"),
        border = BorderStroke(PulseTokens.focusWidth / 2, colors.divider),
        shape = PulseTheme.shapes.extraLarge,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            Icon(painterResource(R.drawable.garage_calendar), null, Modifier.size(PulseTokens.medium), tint = colors.textSecondary)
            PulseText(garageDate(item.date), style = PulseTextStyle.Caption, color = colors.textSecondary)
        }
        item.items.forEach { FuelTankSummary(it) }
        PulseText(
            stringResource(R.string.pulse_garage_fuel_history_odometer, decimal(item.odometer)),
            style = PulseTextStyle.Body2,
            color = colors.textSecondary,
        )
        item.station?.takeIf { it.isNotBlank() }?.let { PulseText(it, style = PulseTextStyle.Body2, color = colors.textSecondary) }
        item.notes?.takeIf { it.isNotBlank() }?.let { PulseText(it, style = PulseTextStyle.Body2, color = colors.textSecondary) }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = PulseTokens.small)
                .height(PulseTokens.focusWidth / 2)
                .background(colors.divider),
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PulseButton(
                stringResource(R.string.pulse_garage_edit),
                { edit(item.draft(vehicle)) },
                Modifier.pulseDescription(stringResource(R.string.pulse_garage_edit_1_s, label)),
                enabled,
                variant = PulseButtonVariant.Secondary,
                leadingIcon = { Icon(painterResource(R.drawable.garage_edit), null, Modifier.size(PulseTokens.medium)) },
            )
            PulseButton(
                stringResource(R.string.pulse_garage_delete),
                {
                    remove(
                        GarageChange.Delete(GarageRecord.Fuel, vehicle, item.id),
                        uiText(R.string.pulse_garage_permanently_remove_this_fill_up_and_all_of_its),
                    )
                },
                Modifier.pulseDescription(stringResource(R.string.pulse_garage_delete_1_s, label)),
                enabled,
                variant = PulseButtonVariant.Destructive,
                style = PulseButtonStyle.Text,
                leadingIcon = { GarageDetailIcon(GarageIcon.Delete) },
            )
        }
    }
}

@Composable
private fun FuelTankSummary(tank: FuelItem) {
    val colors = PulseTheme.colors
    val fuelType = garageOption(tank.type)
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            PulseText(
                tank.quantity?.let { stringResource(R.string.pulse_garage_volume_litres, decimal(it)) } ?: fuelType,
                style = PulseTextStyle.Heading3,
                color = colors.action,
            )
            if (tank.quantity != null) FuelBadge(fuelType)
            FuelBadge(
                if (tank.fill == "missed") stringResource(R.string.pulse_garage_missed_fill) else garageOption(tank.fill),
            )
        }
        if (tank.total != null || tank.price != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
            ) {
                tank.total?.let {
                    PulseText(
                        stringResource(R.string.pulse_garage_fuel_history_total, UserCurrency.format(it)),
                        style = PulseTextStyle.Label,
                        color = colors.textPrimary,
                    )
                }
                tank.price?.let {
                    PulseText(
                        stringResource(R.string.pulse_garage_fuel_history_unit_price, UserCurrency.format(it)),
                        style = PulseTextStyle.Body2,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FuelBadge(label: String) {
    val colors = PulseTheme.colors
    Box(
        Modifier
            .background(colors.surfaceMuted, PulseTheme.shapes.pill)
            .border(
                BorderStroke(PulseTokens.focusWidth / 2, colors.divider),
                PulseTheme.shapes.pill,
            ).padding(horizontal = PulseTheme.spacing.compact, vertical = PulseTokens.tiny),
    ) {
        PulseText(label, style = PulseTextStyle.Label2, color = colors.textSecondary)
    }
}
