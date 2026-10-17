package dev.abhishekan.pulse.feature.auth

import dev.abhishekan.pulse.core.ui.UiText

data class LoginUiState(
    val username: String = "", val password: String = "", val loading: Boolean = false,
    val usernameError: UiText? = null, val passwordError: UiText? = null, val error: UiText? = null,
)
