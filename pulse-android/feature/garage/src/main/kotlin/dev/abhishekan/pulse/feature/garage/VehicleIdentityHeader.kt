package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun VehicleIdentityHeader(
    data: VehicleHistory,
    enabled: Boolean,
    edit: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
            val makeAndModel = listOf(data.makeName, data.modelName).filter(String::isNotBlank).joinToString(" ")
            if (makeAndModel.isNotBlank()) {
                PulseText(makeAndModel, Modifier.semantics { heading() }, PulseTextStyle.Title3)
            }
            PulseText(garageOption(data.vehicleKind), style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary)
            if (data.vehicleKind == "unknown") {
                PulseText(stringResource(R.string.pulse_garage_classify_hint), color = PulseTheme.colors.textSecondary)
            }
        }
        PulseButton(stringResource(R.string.pulse_garage_edit_vehicle), edit, enabled = enabled, style = PulseButtonStyle.Text)
    }
}
