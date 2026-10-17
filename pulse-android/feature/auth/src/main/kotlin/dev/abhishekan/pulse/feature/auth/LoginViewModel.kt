package dev.abhishekan.pulse.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.usecase.Login
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.auth.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val login: Login,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow(LoginUiState(username = savedState["username"] ?: ""))
    val uiState = state.asStateFlow()

    fun usernameChanged(value: String) {
        if (state.value.loading) return
        savedState["username"] = value
        state.update { it.copy(username = value, usernameError = null, error = null) }
    }

    fun passwordChanged(value: String) {
        if (!state.value.loading) {
            state.update {
                it.copy(password = value, passwordError = null, error = null)
            }
        }
    }

    fun submit() {
        val input = state.value
        if (input.loading) return
        val usernameError =
            if (input.username.isBlank()) {
                uiText(
                    R.string.pulse_auth_enter_your_username,
                )
            } else {
                null
            }
        val passwordError =
            if (input.password.isEmpty()) {
                uiText(
                    R.string.pulse_auth_enter_your_password,
                )
            } else {
                null
            }
        if (usernameError != null || passwordError != null) {
            state.update {
                it.copy(usernameError = usernameError, passwordError = passwordError, error = null)
            }
            return
        }
        state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = login(input.username, input.password)) {
                is Outcome.Success -> state.update { it.copy(password = "", loading = false) }
                is Outcome.Error ->
                    state.update {
                        it.copy(loading = false, error = result.failure.message)
                    }
            }
        }
    }
}
