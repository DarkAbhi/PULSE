package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseSelectionField(label: String, selected: String, options: List<String>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton({ expanded = true }, Modifier.fillMaxWidth().heightIn(min = PulseTokens.target), enabled = enabled, shape = PulseTokens.controlShape) {
            PulseText("$label: $selected")
        }
        DropdownMenu(expanded && enabled, { expanded = false }) {
            options.forEachIndexed { index, text -> DropdownMenuItem(text = { PulseText(text) },
                onClick = { expanded = false; onSelect(index) }, modifier = Modifier.heightIn(min = PulseTokens.target)) }
        }
    }
}
