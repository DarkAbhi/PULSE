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
