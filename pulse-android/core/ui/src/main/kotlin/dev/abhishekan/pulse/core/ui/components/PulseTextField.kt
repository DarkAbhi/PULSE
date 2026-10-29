package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
    leadingIcon: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = PulseTheme.typography.body1,
    labelAbove: Boolean = false,
) {
    val field: @Composable () -> Unit = {
        OutlinedTextField(
            value,
            onValueChange,
            modifier.semantics { if (labelAbove) contentDescription = label },
            enabled = enabled,
            singleLine = true,
            label = if (labelAbove) null else ({ PulseText(label) }),
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
                } ?: supportingText,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            leadingIcon = leadingIcon,
            placeholder = placeholder,
            shape = PulseTokens.controlShape,
            textStyle = textStyle,
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
    if (labelAbove) {
        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseText(label, style = PulseTextStyle.Label)
            field()
        }
    } else {
        field()
    }
}
