package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseRadioButton(selected: Boolean, onClick: () -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(modifier.defaultMinSize(minHeight = PulseTokens.target).selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        RadioButton(selected, null, enabled = enabled)
        Spacer(Modifier.width(PulseTokens.small))
        PulseText(label, Modifier.padding(vertical = PulseTokens.small))
    }
}
