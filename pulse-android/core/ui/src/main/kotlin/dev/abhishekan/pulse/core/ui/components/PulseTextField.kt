package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.VisualTransformation
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseTextField(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None, trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(value, onValueChange, modifier, enabled = enabled, singleLine = true,
        label = { PulseText(label) }, isError = error != null,
        supportingText = error?.let { { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } },
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        visualTransformation = visualTransformation, trailingIcon = trailingIcon, shape = PulseTokens.controlShape)
}
