package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    switchAtEnd: Boolean = false,
) {
    Row(
        modifier
            .defaultMinSize(
                minHeight = PulseTokens.target,
            ).toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        val control: @Composable () -> Unit = {
            Switch(
                checked,
                null,
                enabled = enabled,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = PulseTheme.colors.onPrimary,
                        checkedTrackColor = PulseTheme.colors.primary,
                        checkedBorderColor = PulseTheme.colors.action,
                        uncheckedThumbColor = PulseTheme.colors.textSecondary,
                        uncheckedTrackColor = PulseTheme.colors.surfaceMuted,
                        uncheckedBorderColor = PulseTheme.colors.outline,
                    ),
            )
        }
        if (!switchAtEnd) {
            control()
            Spacer(Modifier.width(PulseTokens.small))
        }
        Column((if (switchAtEnd) Modifier.weight(1f) else Modifier).padding(vertical = PulseTokens.small)) {
            PulseText(label)
            supportingText?.let { PulseText(it, color = PulseTheme.colors.textSecondary) }
        }
        if (switchAtEnd) {
            Spacer(Modifier.width(PulseTokens.small))
            control()
        }
    }
}
