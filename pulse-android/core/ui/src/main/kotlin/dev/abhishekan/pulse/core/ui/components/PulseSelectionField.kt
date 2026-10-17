package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
) {
    var expanded by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Box(modifier) {
        OutlinedButton(
            {
                focus.clearFocus()
                keyboard?.hide()
                expanded = true
            },
            Modifier.fillMaxWidth().heightIn(
                min = PulseTokens.target,
            ),
            enabled = enabled,
            shape = PulseTokens.controlShape,
        ) {
            PulseText(stringResource(R.string.pulse_ui_selection_label, label, selected))
        }
        DropdownMenu(expanded && enabled, { expanded = false }) {
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
