package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abhishekan.pulse.core.domain.model.TirePressure
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun GarageOverview(
    data: VehicleHistory,
    enabled: Boolean,
    addFuel: () -> Unit,
    addCare: () -> Unit,
    editPressure: () -> Unit,
    recordAir: () -> Unit,
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
                    PressureCard(data.pressure, enabled, editPressure, recordAir, minimumHeight = 377.dp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                EfficiencyCards(data)
                OverviewCare(data, enabled, addFuel, addCare)
                PressureCard(data.pressure, enabled, editPressure, recordAir)
            }
        }
    }
}

@Composable
internal fun EfficiencyCards(
    data: VehicleHistory,
    minimumHeight: androidx.compose.ui.unit.Dp = 0.dp,
) {
    if (data.efficiency.isEmpty()) {
        PulseEmptyState(
            stringResource(R.string.pulse_garage_your_story_starts_here),
            stringResource(R.string.pulse_garage_add_fuel_fill_ups_to_build_an_efficiency_history),
        )
    }
    data.efficiency.forEach { (type, stat) ->
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
    Box(Modifier.fillMaxWidth().height(1.dp).background(PulseTheme.colors.primary.copy(alpha = .25f)))
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
    PulseCard(
        Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, PulseTheme.colors.divider),
        shape = PulseTheme.shapes.extraLarge,
    ) {
        PulseText(stringResource(R.string.pulse_garage_care_at_a_glance), Modifier.semantics { heading() }, PulseTextStyle.Title2)
        Spacer(Modifier.size(PulseTokens.tiny))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.compact)) {
            CareCount(data.fuel.size, R.plurals.pulse_garage_fuel_label, Modifier.weight(1f))
            CareCount(data.maintenance.size, R.plurals.pulse_garage_maintenance_label, Modifier.weight(1f))
            CareCount(data.air.size, R.plurals.pulse_garage_air_label, Modifier.weight(1f))
        }
        Spacer(Modifier.size(PulseTokens.tiny))
        GarageActionPair(
            stringResource(R.string.pulse_garage_add_fuel_fill_up),
            addFuel,
            stringResource(R.string.pulse_garage_add_maintenance),
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
    PulseCard(
        modifier.heightIn(min = 96.dp),
        border = BorderStroke(1.dp, PulseTheme.colors.divider),
        shape = PulseTheme.shapes.medium,
        contentPadding = PulseTokens.medium,
    ) {
        PulseText(
            count.toString(),
            style = PulseTheme.typography.displayMedium.copy(fontSize = 32.sp, lineHeight = 36.sp),
            color = PulseTheme.colors.primary,
        )
        PulseText(pluralStringResource(label, count), style = PulseTextStyle.Caption, color = PulseTheme.colors.textSecondary)
    }
}

@Composable
internal fun PressureCard(
    pressure: TirePressure,
    enabled: Boolean,
    edit: () -> Unit,
    fill: () -> Unit,
    minimumHeight: androidx.compose.ui.unit.Dp = 0.dp,
) {
    PulseCard(
        Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, PulseTheme.colors.divider),
        shape = PulseTheme.shapes.extraLarge,
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = (minimumHeight - PulseTokens.cardPadding * 2).coerceAtLeast(0.dp)),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
        ) {
            Column {
                PulseText(
                    stringResource(R.string.pulse_garage_a_little_air_a_smoother_ride),
                    Modifier.semantics { heading() },
                    PulseTextStyle.Title2,
                )
                PulseText(
                    stringResource(R.string.pulse_garage_target_tire_pressure_psi),
                    style = PulseTextStyle.Body2,
                    color = PulseTheme.colors.textSecondary,
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 560.dp && LocalDensity.current.fontScale <= 1.3f) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.compact)) {
                        PressureValues(
                            stringResource(R.string.pulse_garage_solo),
                            decimal(pressure.frontSolo),
                            decimal(pressure.rearSolo),
                            Modifier.weight(1f),
                            true,
                        )
                        PressureValues(
                            stringResource(R.string.pulse_garage_pillion),
                            decimal(pressure.frontPillion),
                            decimal(pressure.rearPillion),
                            Modifier.weight(1f),
                            false,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.compact)) {
                        PressureValues(
                            stringResource(R.string.pulse_garage_solo),
                            decimal(pressure.frontSolo),
                            decimal(pressure.rearSolo),
                            Modifier.fillMaxWidth(),
                            true,
                        )
                        PressureValues(
                            stringResource(R.string.pulse_garage_pillion),
                            decimal(pressure.frontPillion),
                            decimal(pressure.rearPillion),
                            Modifier.fillMaxWidth(),
                            false,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            GarageActionPair(
                stringResource(R.string.pulse_garage_set_tire_pressure),
                edit,
                stringResource(R.string.pulse_garage_record_air_fill_now),
                fill,
                enabled,
            )
        }
    }
}

@Composable
private fun PressureValues(
    label: String,
    front: String,
    rear: String,
    modifier: Modifier,
    highlighted: Boolean,
) {
    PulseCard(
        modifier,
        highlighted = highlighted,
        border = BorderStroke(1.dp, if (highlighted) PulseTheme.colors.primary else PulseTheme.colors.divider),
        shape = PulseTheme.shapes.medium,
        contentPadding = PulseTheme.spacing.comfortable,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = LocalDensity.current.fontScale > 1.3f || maxWidth < 200.dp
            val values: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.End) {
                    PulseText(stringResource(R.string.pulse_garage_front), style = PulseTextStyle.Caption)
                    PulseText(front, style = PulseTextStyle.Metric)
                }
                Column(horizontalAlignment = Alignment.End) {
                    PulseText(stringResource(R.string.pulse_garage_rear), style = PulseTextStyle.Caption)
                    PulseText(rear, style = PulseTextStyle.Metric)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                if (stacked) {
                    PulseText(label, style = PulseTextStyle.Label, color = if (highlighted) PulseTheme.colors.action else Color.Unspecified)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { values() }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                    ) {
                        PulseText(
                            label,
                            Modifier.weight(1f),
                            PulseTextStyle.Label,
                            color = if (highlighted) PulseTheme.colors.action else Color.Unspecified,
                        )
                        values()
                    }
                }
            }
        }
    }
}

@Composable
private fun GarageActionPair(
    first: String,
    onFirst: () -> Unit,
    second: String,
    onSecond: () -> Unit,
    enabled: Boolean,
    primaryFirst: Boolean = false,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val firstVariant = if (primaryFirst) PulseButtonVariant.Primary else PulseButtonVariant.Secondary
        val secondVariant = if (primaryFirst) PulseButtonVariant.Secondary else PulseButtonVariant.Primary
        if (maxWidth >= 480.dp && LocalDensity.current.fontScale <= 1.3f) {
            Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.compact)) {
                PulseButton(first, onFirst, Modifier.weight(1f).heightIn(min = 52.dp), enabled, variant = firstVariant)
                PulseButton(second, onSecond, Modifier.weight(1f).heightIn(min = 52.dp), enabled, variant = secondVariant)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                PulseButton(first, onFirst, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled, variant = firstVariant)
                PulseButton(second, onSecond, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled, variant = secondVariant)
            }
        }
    }
}

// Decorative line icons inherit the same theme colors and stroke treatment as the shared UI.
internal enum class GarageIcon { Fuel, Delete }

@Composable
internal fun GarageDetailIcon(
    kind: GarageIcon,
    tint: Color = androidx.compose.material3.LocalContentColor.current,
) {
    Icon(garageIcons.getValue(kind), contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
}

private val garageIcons =
    GarageIcon.entries.associateWith { kind ->
        ImageVector
            .Builder(kind.name, 24.dp, 24.dp, 24f, 24f)
            .apply {
                path(
                    stroke =
                        androidx.compose.ui.graphics
                            .SolidColor(Color.Black),
                    strokeLineWidth = 1.8f,
                    fill = null,
                    strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
                ) {
                    when (kind) {
                        GarageIcon.Fuel -> {
                            moveTo(4f, 21f)
                            lineTo(4f, 3f)
                            lineTo(14f, 3f)
                            lineTo(14f, 21f)
                            moveTo(2f, 21f)
                            lineTo(16f, 21f)
                            moveTo(4f, 10f)
                            lineTo(14f, 10f)
                            moveTo(14f, 7f)
                            lineTo(18f, 7f)
                            lineTo(21f, 10f)
                            lineTo(21f, 17f)
                            curveTo(21f, 20f, 17f, 20f, 17f, 17f)
                            lineTo(17f, 13f)
                            moveTo(18f, 4f)
                            lineTo(21f, 7f)
                            lineTo(21f, 10f)
                        }
                        GarageIcon.Delete -> {
                            moveTo(3f, 6f)
                            lineTo(21f, 6f)
                            moveTo(9f, 6f)
                            lineTo(9f, 3f)
                            lineTo(15f, 3f)
                            lineTo(15f, 6f)
                            moveTo(5f, 6f)
                            lineTo(6f, 21f)
                            lineTo(18f, 21f)
                            lineTo(19f, 6f)
                        }
                    }
                }
            }.build()
    }
