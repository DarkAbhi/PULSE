package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    Row(
        modifier
            .defaultMinSize(
                minHeight = PulseTokens.target,
            ).selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected,
            null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = PulseTheme.colors.action, unselectedColor = PulseTheme.colors.outline),
        )
        Spacer(Modifier.width(PulseTokens.small))
        Column(Modifier.padding(vertical = PulseTokens.small)) {
            PulseText(label)
            supportingText?.let { PulseText(it, style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary) }
        }
    }
}
