package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.R

@Composable
fun PulseSelectionField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    triggerModifier: Modifier = Modifier,
    labelAbove: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val description = stringResource(R.string.pulse_ui_selection_label, label, selected)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (labelAbove) 8.dp else 0.dp)) {
        if (labelAbove) PulseText(label, style = PulseTextStyle.Label)
        Box {
            OutlinedButton(
                {
                    focus.clearFocus()
                    keyboard?.hide()
                    expanded = true
                },
                triggerModifier.fillMaxWidth().then(
                    if (labelAbove) Modifier.semantics { contentDescription = description } else Modifier,
                ).heightIn(
                    min = PulseTokens.target,
                ),
                enabled = enabled,
                shape = PulseTokens.controlShape,
                border = BorderStroke(PulseTokens.focusWidth / 2, if (enabled) PulseTheme.colors.outline else PulseTheme.colors.border),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = PulseTheme.colors.textPrimary,
                        containerColor = PulseTheme.colors.surface,
                        disabledContentColor = PulseTheme.colors.textSecondary,
                    ),
            ) {
                PulseText(if (labelAbove) selected else description)
            }
            DropdownMenu(
                expanded && enabled,
                { expanded = false },
                shape = PulseTheme.shapes.medium,
                containerColor = PulseTheme.colors.surface,
                tonalElevation = PulseTheme.elevation.none,
                shadowElevation = PulseTheme.elevation.low,
            ) {
                options.forEachIndexed { index, text ->
                    DropdownMenuItem(
                        text = { PulseText(text) },
                        onClick = {
                            expanded = false
                            onSelect(index)
                        },
                        modifier = Modifier.heightIn(min = PulseTokens.target),
                    )
                }
            }
        }
    }
}
