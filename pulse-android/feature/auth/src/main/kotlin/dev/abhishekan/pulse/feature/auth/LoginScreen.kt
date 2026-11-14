package dev.abhishekan.pulse.feature.auth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.components.PulsePasswordField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.pagePadding
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginRoute(
    notice: String?,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(
        state,
        viewModel::usernameChanged,
        viewModel::passwordChanged,
        viewModel::submit,
        notice,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
    notice: String? = null,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        keyboard?.hide()
        onSubmit()
    }
    Scaffold(containerColor = PulseTheme.colors.background, contentColor = PulseTheme.colors.textPrimary) { insets ->
        PulseAdaptiveLayout(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .consumeWindowInsets(insets)
                .imePadding(),
        ) { size ->
            Column(
                Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxSize().padding(horizontal = size.pagePadding),
            ) {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (size == PulseWindowSize.Compact) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = size.pagePadding),
                            verticalArrangement = Arrangement.spacedBy(PulseTokens.large),
                        ) {
                            BrandIntroduction(false)
                            LoginForm(state, onUsername, onPassword, submit, notice)
                        }
                    } else {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = size.pagePadding),
                            horizontalArrangement = Arrangement.spacedBy(PulseTokens.large),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f)) { BrandIntroduction(true) }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                Box(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
                                    LoginForm(state, onUsername, onPassword, submit, notice)
                                }
                            }
                        }
                    }
                }
                val continueAction = PulseFormAction(stringResource(R.string.pulse_auth_continue), submit, loading = state.loading)
                if (size == PulseWindowSize.Compact) {
                    PulseFormActions(continueAction)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PulseTokens.large)) {
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.weight(1f)) {
                            PulseFormActions(continueAction, modifier = Modifier.widthIn(max = PulseTokens.readingWidth))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandIntroduction(expanded: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseText(
            stringResource(R.string.pulse_auth_pulse),
            style = PulseTextStyle.Label,
            color = PulseTheme.colors.action,
        )
        PulseText(
            if (expanded) {
                stringResource(
                    R.string.pulse_auth_your_everyday_in_one_place,
                )
            } else {
                stringResource(R.string.pulse_auth_your_everyday_considered)
            },
            Modifier.semantics { heading() },
            PulseTextStyle.Display,
        )
        PulseText(
            stringResource(R.string.pulse_auth_a_private_space_for_your_fitness_finances_meals_and),
            color = PulseTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun LoginForm(
    state: LoginUiState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    submit: () -> Unit,
    notice: String?,
) {
    val focus = LocalFocusManager.current
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_auth_welcome_back),
            Modifier.semantics {
                heading()
            },
            PulseTextStyle.Title,
        )
        PulseText(
            stringResource(R.string.pulse_auth_sign_in_to_pick_up_where_you_left_off),
            color = PulseTheme.colors.textSecondary,
        )
        if (notice !=
            null
        ) {
            PulseText(notice, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Spacer(Modifier.height(PulseTokens.small))
        PulseTextField(
            state.username,
            onUsername,
            stringResource(R.string.pulse_auth_username),
            Modifier.fillMaxWidth().semantics {
                contentType =
                    ContentType.Username
            },
            enabled = !state.loading,
            error = state.usernameError,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
        )
        PulsePasswordField(
            state.password,
            onPassword,
            stringResource(R.string.pulse_auth_password),
            Modifier.fillMaxWidth().semantics {
                contentType =
                    ContentType.Password
            },
            enabled = !state.loading,
            error = state.passwordError,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        if (state.error !=
            null
        ) {
            PulseText(
                state.error,
                Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                },
                color = PulseTheme.colors.error,
            )
        }
        PulseText(
            stringResource(R.string.pulse_auth_your_account_your_own_quiet_corner),
            color = PulseTheme.colors.textSecondary,
        )
    }
}
