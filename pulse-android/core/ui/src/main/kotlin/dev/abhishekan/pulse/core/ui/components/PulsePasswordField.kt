package dev.abhishekan.pulse.core.ui.components

import dev.abhishekan.pulse.core.ui.R

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.core.ui.asString

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*

@Composable
fun PulsePasswordField(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, error: UiText? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    PulseTextField(value, onValueChange, label, modifier, enabled, error, keyboardOptions, keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { PulseTextButton(if (visible) uiText(R.string.pulse_ui_hide) else uiText(R.string.pulse_ui_show), { visible = !visible }, enabled = enabled) })
}

@Composable
fun PulsePasswordField(value: String, onValueChange: (String) -> Unit, label: UiText, modifier: Modifier = Modifier,
    enabled: Boolean = true, error: UiText? = null, keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default) =
    PulsePasswordField(value, onValueChange, label.asString(), modifier, enabled, error, keyboardOptions, keyboardActions)
