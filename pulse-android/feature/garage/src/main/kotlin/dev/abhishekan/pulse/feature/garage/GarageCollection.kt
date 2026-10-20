package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.Vehicle
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun GarageCollectionHeader(
    enabled: Boolean,
    add: () -> Unit,
) {
    val colors = PulseTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 720.dp && LocalDensity.current.fontScale <= 1.3f
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PulseText(
                    stringResource(R.string.pulse_garage_your_garage),
                    Modifier.semantics { heading() },
                    PulseTextStyle.Display,
                )
                PulseText(
                    stringResource(R.string.pulse_garage_keep_every_journey_running_smoothly),
                    color = colors.textSecondary,
                )
                Button(
                    onClick = add,
                    enabled = enabled,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                ) {
                    Icon(painterResource(R.drawable.garage_add), null, Modifier.size(20.dp))
                    Spacer(Modifier.size(12.dp))
                    PulseText(stringResource(R.string.pulse_garage_add_vehicle), style = PulseTextStyle.Label)
                }
            }
            if (wide) {
                Image(painterResource(R.drawable.garage_workshop), null, Modifier.size(300.dp, 208.dp), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
internal fun GarageVehicleGrid(
    vehicles: List<Vehicle>,
    selected: Long?,
    enabled: Boolean,
    select: (Long) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 960.dp && LocalDensity.current.fontScale <= 1.3f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            vehicles.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { vehicle ->
                        GarageVehicleCard(vehicle, vehicle.id == selected, enabled, { select(vehicle.id) }, Modifier.weight(1f))
                    }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GarageVehicleCard(
    vehicle: Vehicle,
    selected: Boolean,
    enabled: Boolean,
    open: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PulseTheme.colors
    PulseCard(modifier.fillMaxWidth(), highlighted = selected) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 440.dp && LocalDensity.current.fontScale <= 1.3f
            val title: @Composable (Modifier) -> Unit = { titleModifier ->
                Column(titleModifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(
                        Modifier.size(56.dp).background(colors.primarySoft, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(dev.abhishekan.pulse.core.ui.R.drawable.pulse_garage),
                            null,
                            Modifier.size(24.dp),
                            tint = colors.action,
                        )
                    }
                    PulseText(vehicle.name, Modifier.semantics { heading() }, PulseTextStyle.Title)
                    PulseText(
                        stringResource(
                            R.string.pulse_garage_vehicle_summary,
                            garageOption(vehicle.vehicleType),
                            vehicle.fuelTypes.map { garageOption(it) }.joinToString(" + "),
                        ),
                        color = colors.textSecondary,
                    )
                }
            }
            if (wide) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    title(Modifier.weight(1f))
                    GarageVehicleArtwork(Modifier.weight(1.2f).height(208.dp))
                }
            } else {
                Column {
                    title(Modifier.fillMaxWidth())
                    GarageVehicleArtwork(Modifier.fillMaxWidth().height(184.dp))
                }
            }
        }
        Button(
            onClick = open,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(20.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.primarySoft.copy(alpha = 0.5f),
                    contentColor = colors.action,
                ),
        ) {
            PulseText(stringResource(R.string.pulse_garage_view_1_s, vehicle.name), Modifier.weight(1f), PulseTextStyle.Label)
            Spacer(Modifier.size(16.dp))
            Icon(painterResource(dev.abhishekan.pulse.core.ui.R.drawable.pulse_back), null, Modifier.size(24.dp).rotate(180f))
        }
    }
}

@Composable
private fun GarageVehicleArtwork(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(240.dp, 144.dp)
                .rotate(-12f)
                .background(PulseTheme.colors.primarySoft.copy(alpha = 0.4f), RoundedCornerShape(48)),
        )
        // shortcut: shared artwork for all vehicles, replace when vehicle photos are supported.
        Image(painterResource(R.drawable.garage_vehicle), null, Modifier.matchParentSize().padding(8.dp), contentScale = ContentScale.Fit)
    }
}
