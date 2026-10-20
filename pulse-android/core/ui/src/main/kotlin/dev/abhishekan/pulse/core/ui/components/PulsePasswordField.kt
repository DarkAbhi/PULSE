package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import dev.abhishekan.pulse.core.ui.R
import dev.abhishekan.pulse.core.ui.UiText

@Composable
fun PulsePasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: UiText? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    PulseTextField(
        value,
        onValueChange,
        label,
        modifier,
        enabled,
        error,
        keyboardOptions,
        keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            PulseTextButton(
                if (visible) {
                    stringResource(
                        R.string.pulse_ui_hide,
                    )
                } else {
                    stringResource(R.string.pulse_ui_show)
                },
                {
                    visible =
                        !visible
                },
                enabled = enabled,
            )
        },
    )
}
