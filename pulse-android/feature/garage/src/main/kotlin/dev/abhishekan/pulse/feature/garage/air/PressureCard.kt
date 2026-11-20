package dev.abhishekan.pulse.feature.garage.air

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.TirePressure
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.shared.GarageActionPair
import dev.abhishekan.pulse.feature.garage.shared.decimal
import java.math.BigDecimal
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable
internal fun PressureCard(
    pressure: TirePressure,
    enabled: Boolean,
    edit: () -> Unit,
    fill: () -> Unit,
    minimumHeight: Dp = 0.dp,
    prominent: Boolean = false,
    fillWithOdometer: ((BigDecimal?) -> Unit)? = null,
) {
    var odometer by rememberSaveable { mutableStateOf("") }
    var confirmFill by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = (minimumHeight - PulseTokens.cardPadding * 2).coerceAtLeast(0.dp)),
            verticalArrangement = Arrangement.spacedBy(if (prominent) PulseTokens.cardPadding else PulseTokens.medium),
        ) {
            Column {
                PulseText(
                    stringResource(R.string.pulse_garage_a_little_air_a_smoother_ride),
                    Modifier.semantics { heading() },
                    if (prominent) PulseTextStyle.Heading3 else PulseTextStyle.Title2,
                )
                PulseText(
                    stringResource(R.string.pulse_garage_target_tire_pressure_psi),
                    style = PulseTextStyle.Body2,
                    color = PulseTheme.colors.textSecondary,
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                PulseText(
                    stringResource(R.string.pulse_garage_front),
                    Modifier.weight(.3f),
                    PulseTextStyle.Body2,
                    color = PulseTheme.colors.textSecondary,
                )
                PulseText(
                    stringResource(R.string.pulse_garage_rear),
                    Modifier.weight(.3f),
                    PulseTextStyle.Body2,
                    color = PulseTheme.colors.textSecondary,
                )
            }
            PressureTableRow(stringResource(R.string.pulse_garage_solo), decimal(pressure.frontSolo), decimal(pressure.rearSolo), true)
            PressureTableRow(
                stringResource(R.string.pulse_garage_pillion),
                decimal(pressure.frontPillion),
                decimal(pressure.rearPillion),
                false,
            )
            Spacer(Modifier.weight(1f))
            GarageActionPair(
                stringResource(R.string.pulse_garage_set_pressure_action),
                edit,
                stringResource(R.string.pulse_garage_record_air_action),
                { confirmFill = true },
                enabled,
            )
        }
    }
    if (confirmFill) {
        val mileage = odometer.toBigDecimalOrNull()
        val valid =
            odometer.isBlank() ||
                (
                    mileage != null &&
                        mileage.signum() >= 0 &&
                        mileage < "100000000000".toBigDecimal() &&
                        mileage.stripTrailingZeros().scale() <= 1
                )
        PulseDialog(
            onDismissRequest = { confirmFill = false },
            title = { PulseText(stringResource(R.string.pulse_garage_confirm_air_fill_title), style = PulseTextStyle.Title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                    PulseText(stringResource(R.string.pulse_garage_confirm_air_fill_message))
                    if (fillWithOdometer != null) {
                        PulseTextField(
                            odometer,
                            { odometer = it },
                            stringResource(R.string.pulse_garage_odometer_km_optional),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                }
            },
            confirmButton = {
                PulseFormActions(
                    primary =
                        PulseFormAction(
                            stringResource(CoreUiR.string.pulse_ui_record),
                            {
                                confirmFill = false
                                if (fillWithOdometer != null) fillWithOdometer(mileage) else fill()
                                odometer = ""
                            },
                            enabled = valid,
                        ),
                    secondary = PulseFormAction(stringResource(CoreUiR.string.pulse_ui_cancel), { confirmFill = false }),
                )
            },
        )
    }
}

@Composable
private fun PressureTableRow(
    label: String,
    front: String,
    rear: String,
    highlighted: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier
                        .background(PulseTheme.colors.primarySoft, PulseTheme.shapes.large)
                        .then(Modifier.border(BorderStroke(1.dp, PulseTheme.colors.primary), PulseTheme.shapes.large))
                } else {
                    Modifier
                },
            ).padding(PulseTokens.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseText(
            label,
            Modifier.weight(1f),
            PulseTextStyle.Title3,
            color = if (highlighted) PulseTheme.colors.action else PulseTheme.colors.textPrimary,
        )
        PulseText(front, Modifier.weight(.3f), PulseTextStyle.Title3)
        PulseText(rear, Modifier.weight(.3f), PulseTextStyle.Title3)
    }
}

@Composable
private fun PressureValues(
    label: String,
    front: String,
    rear: String,
    modifier: Modifier,
    highlighted: Boolean,
    prominent: Boolean = false,
) {
    PulseCard(
        modifier,
        highlighted = highlighted,
        border = BorderStroke(1.dp, if (highlighted) PulseTheme.colors.primary else PulseTheme.colors.divider),
        shape = PulseTheme.shapes.medium,
        contentPadding = PulseTheme.spacing.comfortable,
    ) {
        if (prominent) {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                PulseText(
                    label,
                    style = PulseTextStyle.Title3,
                    color = if (highlighted) PulseTheme.colors.action else PulseTheme.colors.textSecondary,
                )
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val values: @Composable () -> Unit = {
                        PressureMetric(stringResource(R.string.pulse_garage_front), front)
                        PressureMetric(stringResource(R.string.pulse_garage_rear), rear)
                    }
                    if (maxWidth < 200.dp || LocalDensity.current.fontScale > 1.3f) {
                        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) { values() }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
                        ) { values() }
                    }
                }
            }
            return@PulseCard
        }
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
private fun PressureMetric(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        PulseText(label, style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary)
        PulseText(value, style = PulseTextStyle.DisplayXL)
    }
}
