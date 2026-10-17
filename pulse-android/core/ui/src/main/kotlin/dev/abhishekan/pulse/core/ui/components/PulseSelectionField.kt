package dev.abhishekan.pulse.core.ui.components

import dev.abhishekan.pulse.core.ui.R

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.core.ui.asString

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseSelectionField(label: String, selected: String, options: List<String>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Box(modifier) {
        OutlinedButton({ focus.clearFocus(); keyboard?.hide(); expanded = true }, Modifier.fillMaxWidth().heightIn(min = PulseTokens.target), enabled = enabled, shape = PulseTokens.controlShape) {
            PulseText(uiText(R.string.pulse_ui_selection_label , label, selected))
        }
        DropdownMenu(expanded && enabled, { expanded = false }) {
            options.forEachIndexed { index, text -> DropdownMenuItem(text = { PulseText(text) },
                onClick = { expanded = false; onSelect(index) }, modifier = Modifier.heightIn(min = PulseTokens.target)) }
        }
    }
}

@Composable
fun PulseSelectionField(label: UiText, selected: String, options: List<String>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = PulseSelectionField(label.asString(), selected, options, onSelect, modifier, enabled)

@Composable
fun PulseSelectionField(label: UiText, selected: UiText, options: List<UiText>, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    PulseSelectionField(label.asString(), selected.asString(), options.map { it.asString() }, onSelect, modifier, enabled)
