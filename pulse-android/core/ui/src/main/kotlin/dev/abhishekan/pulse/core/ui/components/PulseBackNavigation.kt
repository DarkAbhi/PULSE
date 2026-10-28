package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.R

@Composable
fun PulseBackNavigation(
    label: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PulseIconButton(label, onBack, enabled = enabled) {
            Icon(painterResource(R.drawable.pulse_back), null)
        }
        PulseText(label, style = PulseTextStyle.Label)
    }
}
