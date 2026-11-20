package dev.abhishekan.pulse.feature.auth

internal sealed interface LoginAction {
    data class UsernameChanged(
        val value: String,
    ) : LoginAction

    data class PasswordChanged(
        val value: String,
    ) : LoginAction

    data object Submit : LoginAction
}
