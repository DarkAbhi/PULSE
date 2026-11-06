package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .defaultMinSize(
                minHeight = PulseTokens.target,
            ).toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked,
            null,
            enabled = enabled,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = PulseTheme.colors.primary,
                    checkmarkColor = PulseTheme.colors.onPrimary,
                    uncheckedColor = PulseTheme.colors.outline,
                ),
        )
        Spacer(Modifier.width(PulseTokens.small))
        PulseText(label, Modifier.padding(vertical = PulseTokens.small))
    }
}
