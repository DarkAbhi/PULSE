package dev.abhishekan.pulse.feature.auth

data class LoginUiState(
    val username: String = "", val password: String = "", val loading: Boolean = false,
    val usernameError: String? = null, val passwordError: String? = null, val error: String? = null,
)
