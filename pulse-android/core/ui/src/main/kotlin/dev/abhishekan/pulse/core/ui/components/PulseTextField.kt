package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText

@Composable
fun PulseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: UiText? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value,
        onValueChange,
        modifier,
        enabled = enabled,
        singleLine = true,
        label = { PulseText(label) },
        isError = error != null,
        supportingText =
            error?.let {
                {
                    PulseText(
                        it,
                        Modifier.semantics {
                            liveRegion =
                                LiveRegionMode.Polite
                        },
                    )
                }
            },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        shape = PulseTokens.controlShape,
        textStyle = PulseTheme.typography.body1,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = PulseTheme.colors.textPrimary,
                unfocusedTextColor = PulseTheme.colors.textPrimary,
                disabledTextColor = PulseTheme.colors.textSecondary,
                focusedContainerColor = PulseTheme.colors.surface,
                unfocusedContainerColor = PulseTheme.colors.surface,
                disabledContainerColor = PulseTheme.colors.surfaceMuted,
                errorContainerColor = PulseTheme.colors.surface,
                focusedBorderColor = PulseTheme.colors.action,
                unfocusedBorderColor = PulseTheme.colors.outline,
                disabledBorderColor = PulseTheme.colors.border,
                errorBorderColor = PulseTheme.colors.error,
                cursorColor = PulseTheme.colors.action,
            ),
    )
}
