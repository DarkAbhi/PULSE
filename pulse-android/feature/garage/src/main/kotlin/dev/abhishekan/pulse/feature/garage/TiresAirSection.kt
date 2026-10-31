package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.AirFill
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.uiText

@Composable
internal fun TiresAirSection(
    data: VehicleHistory,
    vehicle: Long,
    enabled: Boolean,
    editPressure: () -> Unit,
    recordAir: () -> Unit,
    remove: (GarageChange.Delete, UiText) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 1000.dp && LocalDensity.current.fontScale <= 1.3f) {
            Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.comfortable)) {
                Column(Modifier.weight(.64f)) {
                    PressureCard(data.pressure, enabled, editPressure, recordAir, prominent = true)
                }
                Column(Modifier.weight(.36f)) {
                    AirFillHistory(data.air, vehicle, enabled, remove)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                PressureCard(data.pressure, enabled, editPressure, recordAir, prominent = true)
                AirFillHistory(data.air, vehicle, enabled, remove)
            }
        }
    }
}

@Composable
private fun AirFillHistory(
    entries: List<AirFill>,
    vehicle: Long,
    enabled: Boolean,
    remove: (GarageChange.Delete, UiText) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseText(stringResource(R.string.pulse_garage_air_fills), Modifier.semantics { heading() }, PulseTextStyle.Title2)
        if (entries.isEmpty()) {
            PulseEmptyState(
                stringResource(R.string.pulse_garage_no_air_fills_recorded),
                stringResource(R.string.pulse_garage_record_an_air_fill_after_checking_your_tires),
            )
        }
        entries.forEach { entry ->
            AirFillHistoryCard(entry, enabled) {
                remove(
                    GarageChange.Delete(GarageRecord.Air, vehicle, entry.id),
                    uiText(R.string.pulse_garage_delete_air_fill_confirmation),
                )
            }
        }
    }
}

@Composable
internal fun AirFillHistoryCard(
    entry: AirFill,
    enabled: Boolean,
    delete: () -> Unit,
) {
    val colors = PulseTheme.colors
    PulseCard(
        Modifier.fillMaxWidth().testTag("garage-air-${entry.id}"),
        border = BorderStroke(PulseTokens.focusWidth / 2, colors.divider),
        shape = PulseTheme.shapes.extraLarge,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
        ) {
            Box(
                Modifier.size(PulseTokens.target).background(colors.infoSoft, PulseTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.garage_air), null, Modifier.size(PulseTokens.cardPadding), tint = colors.info)
            }
            Column(Modifier.weight(1f)) {
                PulseText(stringResource(R.string.pulse_garage_air_fill_1_s, entry.id), style = PulseTextStyle.Title2)
                PulseText(garageDate(entry.date), style = PulseTextStyle.Body2, color = colors.textSecondary)
            }
            PulseButton(
                stringResource(R.string.pulse_garage_delete_air_fill_1_s, entry.id),
                delete,
                enabled = enabled,
                style = PulseButtonStyle.Icon,
                icon = { GarageDetailIcon(GarageIcon.Delete) },
            )
        }
    }
}
