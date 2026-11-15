package dev.abhishekan.pulse.feature.auth

import dev.abhishekan.pulse.core.ui.UiText

internal data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val usernameError: UiText? = null,
    val passwordError: UiText? = null,
    val submission: LoginSubmission = LoginSubmission.Idle,
) {
    val isSubmitting: Boolean get() = submission == LoginSubmission.Submitting
    val canEdit: Boolean get() = !isSubmitting && submission != LoginSubmission.Succeeded
}

internal sealed interface LoginSubmission {
    data object Idle : LoginSubmission

    data object Submitting : LoginSubmission

    // Keep the form locked until the app observes the authenticated session and replaces it.
    data object Succeeded : LoginSubmission

    data class Failed(
        val message: UiText,
    ) : LoginSubmission
}

internal sealed interface LoginAction {
    data class UsernameChanged(
        val value: String,
    ) : LoginAction

    data class PasswordChanged(
        val value: String,
    ) : LoginAction

    data object Submit : LoginAction
}
