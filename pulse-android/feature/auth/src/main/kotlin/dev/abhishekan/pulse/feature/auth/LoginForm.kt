package dev.abhishekan.pulse.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulsePasswordField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import kotlinx.coroutines.CompletableDeferred

@Composable
internal fun LoginForm(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    notice: String? = null,
) {
    val passwordFocus = remember { FocusRequester() }
    PulseCard(modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_auth_welcome_back),
            Modifier.semantics { heading() },
            PulseTextStyle.Title,
        )
        PulseText(
            stringResource(R.string.pulse_auth_sign_in_to_pick_up_where_you_left_off),
            color = PulseTheme.colors.textSecondary,
        )
        if (notice != null) {
            PulseText(notice, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Spacer(Modifier.height(PulseTokens.small))
        PulseTextField(
            value = state.username,
            onValueChange = { onAction(LoginAction.UsernameChanged(it)) },
            label = stringResource(R.string.pulse_auth_username),
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
            enabled = !state.isSubmitting,
            error = state.usernameError,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
        )
        PulsePasswordField(
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            label = stringResource(R.string.pulse_auth_password),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocus)
                    .semantics { contentType = ContentType.Password },
            enabled = !state.isSubmitting,
            error = state.passwordError,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        val submission = state.submission
        if (submission is LoginSubmission.Failed) {
            val errorRequester = remember { BringIntoViewRequester() }
            val errorPlaced = remember { CompletableDeferred<Unit>() }
            PulseText(
                submission.message,
                Modifier
                    .bringIntoViewRequester(errorRequester)
                    .onGloballyPositioned { errorPlaced.complete(Unit) }
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = PulseTheme.colors.error,
            )
            LaunchedEffect(submission) {
                errorPlaced.await()
                errorRequester.bringIntoView()
            }
        }
        PulseText(
            stringResource(R.string.pulse_auth_your_account_your_own_quiet_corner),
            color = PulseTheme.colors.textSecondary,
        )
    }
}
