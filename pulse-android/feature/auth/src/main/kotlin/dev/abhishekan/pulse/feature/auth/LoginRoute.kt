package dev.abhishekan.pulse.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginRoute(notice: String?) {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(state = state, onAction = viewModel::onAction, notice = notice)
}
