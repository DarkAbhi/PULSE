package dev.abhishekan.pulse.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.pagePadding

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
    notice: String? = null,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        keyboard?.hide()
        onAction(LoginAction.Submit)
    }
    Scaffold(
        modifier = modifier,
        containerColor = PulseTheme.colors.background,
        contentColor = PulseTheme.colors.textPrimary,
    ) { insets ->
        PulseAdaptiveLayout(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .consumeWindowInsets(insets)
                .imePadding(),
        ) { size ->
            val compact = size == PulseWindowSize.Compact
            Column(
                Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxSize().padding(horizontal = size.pagePadding),
            ) {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val scrollModifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = size.pagePadding)
                    if (compact) {
                        Column(
                            modifier = scrollModifier,
                            verticalArrangement = Arrangement.spacedBy(PulseTokens.large),
                        ) {
                            LoginIntroduction(expanded = false)
                            LoginForm(state = state, onAction = onAction, onSubmit = submit, notice = notice)
                        }
                    } else {
                        Row(
                            modifier = scrollModifier,
                            horizontalArrangement = Arrangement.spacedBy(PulseTokens.large),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LoginIntroduction(expanded = true, modifier = Modifier.weight(1f))
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                LoginForm(
                                    state = state,
                                    onAction = onAction,
                                    onSubmit = submit,
                                    modifier = Modifier.widthIn(max = PulseTokens.readingWidth),
                                    notice = notice,
                                )
                            }
                        }
                    }
                }
                val continueAction =
                    PulseFormAction(stringResource(R.string.pulse_auth_continue), submit, loading = state.isSubmitting)
                if (compact) {
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
