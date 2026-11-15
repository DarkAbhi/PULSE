package dev.abhishekan.pulse.feature.auth

import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.usecase.Login
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LoginViewModel(
    private val login: Login,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow(LoginUiState(username = savedState[USERNAME_KEY] ?: ""))
    val uiState = state.asStateFlow()

    @MainThread
    fun onAction(action: LoginAction) {
        val input = state.value
        if (!input.canEdit) return
        when (action) {
            is LoginAction.UsernameChanged -> {
                savedState[USERNAME_KEY] = action.value
                state.update {
                    it.copy(username = action.value, usernameError = null, submission = LoginSubmission.Idle)
                }
            }
            is LoginAction.PasswordChanged ->
                state.update {
                    it.copy(password = action.value, passwordError = null, submission = LoginSubmission.Idle)
                }
            LoginAction.Submit -> submit(input)
        }
    }

    private fun submit(input: LoginUiState) {
        val usernameError = if (input.username.isBlank()) uiText(R.string.pulse_auth_enter_your_username) else null
        val passwordError = if (input.password.isEmpty()) uiText(R.string.pulse_auth_enter_your_password) else null
        if (usernameError != null || passwordError != null) {
            state.update {
                it.copy(usernameError = usernameError, passwordError = passwordError, submission = LoginSubmission.Idle)
            }
            return
        }
        state.update { it.copy(submission = LoginSubmission.Submitting) }
        viewModelScope.launch {
            val result =
                try {
                    login(input.username, input.password)
                } catch (error: CancellationException) {
                    state.update { it.copy(submission = LoginSubmission.Idle) }
                    throw error
                }
            when (result) {
                is Outcome.Success ->
                    state.update { it.copy(password = "", submission = LoginSubmission.Succeeded) }
                is Outcome.Error ->
                    state.update { it.copy(submission = LoginSubmission.Failed(result.failure.message)) }
            }
        }
    }

    private companion object {
        const val USERNAME_KEY = "username"
    }
}
