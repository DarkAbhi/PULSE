package dev.abhishekan.pulse.feature.garage.detail.overview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abhishekan.pulse.core.domain.model.FuelEfficiency
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.shared.GarageActionPair
import dev.abhishekan.pulse.feature.garage.shared.GarageDetailIcon
import dev.abhishekan.pulse.feature.garage.shared.GarageIcon
import dev.abhishekan.pulse.feature.garage.shared.decimal
import dev.abhishekan.pulse.feature.garage.shared.garageOption
import java.math.BigDecimal

@Composable
internal fun GarageOverview(
    data: VehicleHistory,
    enabled: Boolean,
    addFuel: () -> Unit,
    addCare: () -> Unit,
    openCare: () -> Unit,
    openAir: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 840.dp && LocalDensity.current.fontScale <= 1.3f
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.comfortable)) {
                Column(Modifier.weight(.37f), verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                    EfficiencyCards(data, minimumHeight = 654.dp)
                }
                Column(Modifier.weight(.63f), verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.comfortable)) {
                    OverviewCare(data, enabled, addFuel, addCare)
                    ComingUp(data, enabled, openCare, openAir)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                EfficiencyCards(data)
                OverviewCare(data, enabled, addFuel, addCare)
                GarageRule()
                ComingUp(data, enabled, openCare, openAir)
            }
        }
    }
}

@Composable
internal fun EfficiencyCards(
    data: VehicleHistory,
    minimumHeight: Dp = 0.dp,
) {
    val efficiency =
        if (data.efficiency.isEmpty() && data.fuel.isEmpty()) {
            data.fuelTypes.associateWith {
                FuelEfficiency(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            }
        } else {
            data.efficiency
        }
    efficiency.forEach { (type, stat) ->
        PulseCard(
            Modifier.fillMaxWidth().heightIn(min = minimumHeight),
            highlighted = true,
            border = BorderStroke(1.dp, PulseTheme.colors.primary.copy(alpha = .3f)),
            shape = PulseTheme.shapes.extraLarge,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
            ) {
                GarageDetailIcon(GarageIcon.Fuel, PulseTheme.colors.action)
                PulseText(
                    stringResource(R.string.pulse_garage_fuel_efficiency_title, garageOption(type)),
                    style = PulseTextStyle.Label,
                    color = PulseTheme.colors.action,
                )
            }
            Spacer(Modifier.size(PulseTokens.small))
            if (stat.average != null) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val value = decimal(stat.average)
                    // Scale the condensed display token to the available width, including long values.
                    val fontSize =
                        minOf(
                            if (minimumHeight >
                                0.dp
                            ) {
                                144f
                            } else {
                                96f
                            },
                            maxWidth.value / (value.length * .48f + .6f) / LocalDensity.current.fontScale,
                        )
                    if (LocalDensity.current.fontScale <= 1.3f) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                            PulseText(
                                value,
                                Modifier.alignByBaseline(),
                                style = PulseTheme.typography.displayXL.copy(fontSize = fontSize.sp, lineHeight = (fontSize * 1.05f).sp),
                            )
                            PulseText(stringResource(R.string.pulse_garage_km_per_litre), Modifier.alignByBaseline(), PulseTextStyle.Title3)
                        }
                    } else {
                        Column {
                            PulseText(value, style = PulseTextStyle.DisplayXL)
                            PulseText(stringResource(R.string.pulse_garage_km_per_litre), style = PulseTextStyle.Label)
                        }
                    }
                }
            } else {
                PulseText(stringResource(R.string.pulse_garage_not_enough_history), style = PulseTextStyle.Title)
                PulseText(stringResource(R.string.pulse_garage_record_another_full_fill_after_a_full_tank_to))
            }
            GarageRule()
            if (LocalDensity.current.fontScale > 1.3f) {
                Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                    EfficiencyValue(stringResource(R.string.pulse_garage_last), decimal(stat.last))
                    EfficiencyValue(stringResource(R.string.pulse_garage_best), decimal(stat.max))
                    EfficiencyValue(stringResource(R.string.pulse_garage_lowest), decimal(stat.min))
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = PulseTokens.small),
                    horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
                ) {
                    EfficiencyValue(stringResource(R.string.pulse_garage_last), decimal(stat.last), Modifier.weight(1f))
                    EfficiencyValue(stringResource(R.string.pulse_garage_best), decimal(stat.max), Modifier.weight(1f))
                    EfficiencyValue(stringResource(R.string.pulse_garage_lowest), decimal(stat.min), Modifier.weight(1f))
                }
            }
            GarageRule()
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = PulseTokens.medium)) {
                val volume: @Composable () -> Unit = {
                    EfficiencyValue(
                        stringResource(R.string.pulse_garage_recorded_volume),
                        stringResource(R.string.pulse_garage_volume_litres, decimal(stat.totalVolume)),
                    )
                }
                val cost: @Composable () -> Unit = {
                    EfficiencyValue(stringResource(R.string.pulse_garage_recorded_cost), UserCurrency.format(stat.totalCost))
                }
                if (maxWidth >= 340.dp && LocalDensity.current.fontScale <= 1.3f) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                        Column(Modifier.weight(1f)) { volume() }
                        Column(Modifier.weight(1.1f)) { cost() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                        volume()
                        cost()
                    }
                }
            }
        }
    }
}

@Composable
private fun GarageRule() {
    PulseHorizontalDivider()
}

@Composable
private fun EfficiencyValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
        PulseText(label, style = PulseTextStyle.Caption)
        PulseText(value, style = PulseTheme.typography.displayMedium.copy(fontSize = 32.sp, lineHeight = 36.sp))
    }
}

@Composable
private fun OverviewCare(
    data: VehicleHistory,
    enabled: Boolean,
    addFuel: () -> Unit,
    addCare: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        PulseText(stringResource(R.string.pulse_garage_care_at_a_glance), Modifier.semantics { heading() }, PulseTextStyle.Title2)
        Spacer(Modifier.size(PulseTokens.medium))
        Row(Modifier.fillMaxWidth()) {
            CareCount(data.fuel.size, R.plurals.pulse_garage_fuel_label, Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(72.dp).background(PulseTheme.colors.divider))
            CareCount(
                data.maintenance.size,
                R.plurals.pulse_garage_maintenance_label,
                Modifier.weight(1f).padding(start = PulseTokens.medium),
            )
            Box(Modifier.width(1.dp).height(72.dp).background(PulseTheme.colors.divider))
            CareCount(data.air.size, R.plurals.pulse_garage_air_label, Modifier.weight(1f).padding(start = PulseTokens.medium))
        }
        Spacer(Modifier.size(PulseTokens.medium))
        GarageActionPair(
            stringResource(R.string.pulse_garage_fuel_action),
            addFuel,
            stringResource(R.string.pulse_garage_service_action),
            addCare,
            enabled,
            primaryFirst = true,
        )
    }
}

@Composable
private fun CareCount(
    count: Int,
    label: Int,
    modifier: Modifier,
) {
    Column(modifier) {
        PulseText(
            count.toString(),
            style = PulseTextStyle.Metric,
            color = PulseTheme.colors.primary,
        )
        PulseText(pluralStringResource(label, count), style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary)
    }
}
