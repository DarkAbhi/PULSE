package dev.abhishekan.pulse.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginRoute(notice: String?) {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    LoginScreen(
        state = state,
        onAction = { action ->
            if (action == LoginAction.Submit) keyboard?.hide()
            viewModel.onAction(action)
        },
        notice = notice,
    )
}
