package dev.abhishekan.pulse.feature.garage

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.domain.model.VehicleHistory
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun VehicleIdentityCard(
    data: VehicleHistory,
    enabled: Boolean,
    edit: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(stringResource(R.string.pulse_garage_identity_title), Modifier.semantics { heading() }, PulseTextStyle.Title)
        PulseText(garageOption(data.vehicleKind), color = PulseTheme.colors.textSecondary)
        if (data.makeName.isNotBlank() || data.modelName.isNotBlank()) {
            PulseText(listOf(data.makeName, data.modelName).filter(String::isNotBlank).joinToString(" "))
        }
        if (data.vehicleKind ==
            "unknown"
        ) {
            PulseText(stringResource(R.string.pulse_garage_classify_hint), color = PulseTheme.colors.textSecondary)
        }
        PulseButton(stringResource(R.string.pulse_garage_edit_vehicle), edit, enabled = enabled, style = PulseButtonStyle.Text)
        VehicleDataCredits()
    }
}
