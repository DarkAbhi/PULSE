package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.uiText

@Composable
internal fun VehiclePressureFields(
    draft: GarageDraft,
    enabled: Boolean,
    change: (GarageDraft) -> Unit,
) {
    val twoWheels = draft["vehicleKind"] in listOf("motorcycle", "moped")
    val fields =
        buildList {
            add("frontSolo" to if (twoWheels) R.string.pulse_garage_front_solo_psi else R.string.pulse_garage_front_axle_psi)
            add("rearSolo" to if (twoWheels) R.string.pulse_garage_rear_solo_psi else R.string.pulse_garage_rear_axle_psi)
            // Keep previously saved pillion targets editable when reclassifying a vehicle.
            if (twoWheels || draft["frontPillion"].isNotBlank() || draft["rearPillion"].isNotBlank()) {
                add("frontPillion" to R.string.pulse_garage_front_pillion_psi)
                add("rearPillion" to R.string.pulse_garage_rear_pillion_psi)
            }
        }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 480.dp && LocalDensity.current.fontScale <= 1.3f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.medium)) {
            VehicleAxleDiagram(twoWheels, Modifier.align(Alignment.CenterHorizontally))
            fields.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.medium)) {
                    row.forEach { (key, label) ->
                        val title = stringResource(label)
                        val decrease = stringResource(R.string.pulse_garage_pressure_decrease, title)
                        val increase = stringResource(R.string.pulse_garage_pressure_increase, title)
                        val valid = draft[key].isBlank() || draft.validPressure(key)
                        PulseCard(Modifier.weight(1f), border = BorderStroke(1.dp, PulseTheme.colors.border)) {
                            PulseTextField(
                                draft[key],
                                { change(draft.set(key, it)) },
                                title,
                                Modifier.fillMaxWidth(),
                                enabled,
                                error = if (!valid) uiText(R.string.pulse_garage_pressure_invalid) else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.small)) {
                                PulseButton(
                                    stringResource(R.string.pulse_garage_pressure_minus),
                                    { change(draft.adjustPressure(key, -1)) },
                                    Modifier.weight(1f).semantics { contentDescription = decrease },
                                    enabled = enabled && valid && draft[key].toBigDecimalOrNull()?.signum() == 1,
                                    variant = PulseButtonVariant.Secondary,
                                )
                                PulseButton(
                                    stringResource(R.string.pulse_garage_pressure_plus),
                                    { change(draft.adjustPressure(key, 1)) },
                                    Modifier.weight(1f).semantics { contentDescription = increase },
                                    enabled =
                                        enabled &&
                                            valid &&
                                            (draft[key].toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO) < "9999.9".toBigDecimal(),
                                    variant = PulseButtonVariant.Secondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleAxleDiagram(
    twoWheels: Boolean,
    modifier: Modifier = Modifier,
) {
    val body = PulseTheme.colors.primarySoft
    val outline = PulseTheme.colors.onPrimarySoft
    Canvas(modifier.size(160.dp, 72.dp)) {
        val unit = size.width / 160f
        if (twoWheels) {
            drawRoundRect(outline, Offset(20 * unit, 25 * unit), Size(30 * unit, 22 * unit), CornerRadius(8 * unit))
            drawRoundRect(outline, Offset(110 * unit, 25 * unit), Size(30 * unit, 22 * unit), CornerRadius(8 * unit))
            drawRoundRect(body, Offset(40 * unit, 20 * unit), Size(80 * unit, 32 * unit), CornerRadius(16 * unit))
            drawRoundRect(
                outline,
                Offset(40 * unit, 20 * unit),
                Size(80 * unit, 32 * unit),
                CornerRadius(16 * unit),
                style =
                    Stroke(
                        2 * unit,
                    ),
            )
        } else {
            listOf(42f, 104f).forEach { x ->
                listOf(10f, 50f).forEach { y ->
                    drawRoundRect(outline, Offset(x * unit, y * unit), Size(16 * unit, 12 * unit), CornerRadius(4 * unit))
                }
            }
            drawRoundRect(body, Offset(20 * unit, 16 * unit), Size(120 * unit, 40 * unit), CornerRadius(18 * unit))
            drawRoundRect(
                outline,
                Offset(20 * unit, 16 * unit),
                Size(120 * unit, 40 * unit),
                CornerRadius(18 * unit),
                style =
                    Stroke(
                        2 * unit,
                    ),
            )
            listOf(52f, 108f).forEach { x -> drawLine(outline, Offset(x * unit, 23 * unit), Offset(x * unit, 49 * unit), 2 * unit) }
        }
    }
}
