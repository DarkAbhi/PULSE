package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.abhishekan.pulse.core.ui.PulseTheme

/**
 * Compact centred numeric field for tables (for example sets, reps and weight), whose
 * visible column labels live outside the field. [label] is the TalkBack label.
 */
@Composable
fun PulseNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    suffix: (@Composable () -> Unit)? = null,
) {
    val colors = PulseTheme.colors
    OutlinedTextField(
        value,
        onValueChange,
        modifier.semantics { contentDescription = label },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = PulseTheme.typography.title2.copy(textAlign = TextAlign.Center, fontFeatureSettings = "tnum"),
        suffix = suffix,
        shape = PulseTheme.shapes.medium,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                disabledTextColor = colors.textSecondary,
                focusedContainerColor = colors.surfaceMuted,
                unfocusedContainerColor = colors.surfaceMuted,
                disabledContainerColor = colors.surfaceMuted,
                focusedBorderColor = colors.ring,
                unfocusedBorderColor = colors.outline,
                disabledBorderColor = colors.border,
                cursorColor = colors.action,
            ),
    )
}
