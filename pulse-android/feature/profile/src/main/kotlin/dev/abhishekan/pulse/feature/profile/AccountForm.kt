package dev.abhishekan.pulse.feature.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulsePasswordField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField

@Composable
internal fun AccountForm(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editor = checkNotNull(state.editor)
    val focus = LocalFocusManager.current
    PulseCard(modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        when (editor) {
            AccountEditor.Name ->
                PulseTextField(
                    state.name,
                    { onAction(ProfileAction.ChangeName(it)) },
                    stringResource(R.string.pulse_profile_name),
                    Modifier.fillMaxWidth().semantics {
                        contentType =
                            ContentType.PersonFullName
                    },
                    enabled = state.canMutate,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAction(ProfileAction.SaveEditor) }),
                )
            AccountEditor.Key ->
                PulseTextField(
                    state.keyName,
                    { onAction(ProfileAction.ChangeKeyName(it)) },
                    stringResource(R.string.pulse_profile_key_name),
                    Modifier.fillMaxWidth(),
                    enabled = state.canMutate,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAction(ProfileAction.SaveEditor) }),
                )
            AccountEditor.Password -> {
                PulsePasswordField(
                    state.passwords.current,
                    {
                        onAction(ProfileAction.ChangePasswordField(PasswordField.Current, it))
                    },
                    stringResource(R.string.pulse_profile_current),
                    Modifier.fillMaxWidth().semantics {
                        contentType =
                            ContentType.Password
                    },
                    enabled = state.canMutate,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(onNext = {
                            focus.moveFocus(FocusDirection.Down)
                        }),
                )
                PulsePasswordField(
                    state.passwords.next,
                    {
                        onAction(ProfileAction.ChangePasswordField(PasswordField.Next, it))
                    },
                    stringResource(R.string.pulse_profile_new),
                    Modifier.fillMaxWidth().semantics {
                        contentType =
                            ContentType.NewPassword
                    },
                    enabled = state.canMutate,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(onNext = {
                            focus.moveFocus(FocusDirection.Down)
                        }),
                )
                PulsePasswordField(
                    state.passwords.confirmation,
                    {
                        onAction(ProfileAction.ChangePasswordField(PasswordField.Confirmation, it))
                    },
                    stringResource(R.string.pulse_profile_confirmation),
                    Modifier.fillMaxWidth().semantics {
                        contentType =
                            ContentType.NewPassword
                    },
                    enabled = state.canMutate,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(onDone = {
                            onAction(ProfileAction.SaveEditor)
                        }),
                )
            }
        }
        state.error?.let {
            PulseText(
                it,
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = PulseTheme.colors.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountFormPreview() {
    PulseTheme {
        AccountForm(ProfileUiState(editor = AccountEditor.Password), onAction = {})
    }
}
