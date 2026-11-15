package dev.abhishekan.pulse.feature.auth

import dev.abhishekan.pulse.core.ui.UiText

internal sealed interface LoginSubmission {
    data object Idle : LoginSubmission

    data object Submitting : LoginSubmission

    // Keep the form locked until the app observes the authenticated session and replaces it.
    data object Succeeded : LoginSubmission

    data class Failed(
        val message: UiText,
    ) : LoginSubmission
}
