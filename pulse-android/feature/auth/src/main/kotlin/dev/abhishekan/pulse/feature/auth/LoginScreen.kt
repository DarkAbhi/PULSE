package dev.abhishekan.pulse.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginRoute(notice: String?, viewModel: LoginViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(state, viewModel::usernameChanged, viewModel::passwordChanged, viewModel::submit, notice)
}

@Composable
fun LoginScreen(state: LoginUiState, onUsername: (String) -> Unit, onPassword: (String) -> Unit, onSubmit: () -> Unit, notice: String? = null) {
    Scaffold { insets ->
        PulseAdaptiveLayout(Modifier.fillMaxSize().padding(insets).imePadding()) { size ->
            val margin = if (size == PulseWindowSize.Compact) PulseTokens.medium else PulseTokens.large
            Box(Modifier.fillMaxSize().padding(horizontal = margin), contentAlignment = Alignment.Center) {
                if (size == PulseWindowSize.Compact) {
                    Column(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(vertical = PulseTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(PulseTokens.large)) {
                        BrandIntroduction(false)
                        LoginForm(state, onUsername, onPassword, onSubmit, notice)
                    }
                } else {
                    Row(Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(vertical = PulseTokens.large), horizontalArrangement = Arrangement.spacedBy(PulseTokens.large),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { BrandIntroduction(true) }
                        Box(Modifier.weight(1f).widthIn(max = PulseTokens.readingWidth)) { LoginForm(state, onUsername, onPassword, onSubmit, notice) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandIntroduction(expanded: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseText("PULSE", style = PulseTextStyle.Label, color = MaterialTheme.colorScheme.primary)
        PulseText(if (expanded) "Your everyday,\nin one place." else "Your everyday, considered.",
            Modifier.semantics { heading() }, PulseTextStyle.Display)
        PulseText("A private space for your fitness, finances, meals, and vehicle care.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoginForm(state: LoginUiState, onUsername: (String) -> Unit, onPassword: (String) -> Unit, onSubmit: () -> Unit, notice: String?) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = { keyboard?.hide(); onSubmit() }
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText("Welcome back", Modifier.semantics { heading() }, PulseTextStyle.Title)
        PulseText("Sign in to pick up where you left off.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (notice != null) PulseText(notice, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Spacer(Modifier.height(PulseTokens.small))
        PulseTextField(state.username, onUsername, "Username", Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
            enabled = !state.loading, error = state.usernameError,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }))
        PulsePasswordField(state.password, onPassword, "Password", Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
            enabled = !state.loading, error = state.passwordError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }))
        if (state.error != null) PulseText(state.error, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(PulseTokens.small))
        PulseButton("Continue", submit, Modifier.fillMaxWidth(), loading = state.loading)
        PulseText("Your account. Your own quiet corner.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
