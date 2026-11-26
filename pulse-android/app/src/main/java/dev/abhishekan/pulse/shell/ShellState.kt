package dev.abhishekan.pulse.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import dev.abhishekan.pulse.navigation.AppDestination

internal enum class OnboardingPrompt { NotOffered, Showing, Dismissed }

internal class ShellState(
    onboarding: OnboardingPrompt = OnboardingPrompt.NotOffered,
) {
    var onboarding by mutableStateOf(onboarding)
        private set
    var pendingDestination by mutableStateOf<AppDestination?>(null)
        private set

    fun offerOnboarding() {
        if (onboarding == OnboardingPrompt.NotOffered) onboarding = OnboardingPrompt.Showing
    }

    fun dismissOnboarding() {
        onboarding = OnboardingPrompt.Dismissed
    }

    fun requestLeave(destination: AppDestination) {
        pendingDestination = destination
    }

    fun cancelLeave() {
        pendingDestination = null
    }

    fun confirmLeave(): AppDestination? {
        val destination = pendingDestination
        pendingDestination = null
        return destination
    }

    companion object {
        // Drafts report from their owners; only the session's prompt survives recreation here.
        val Saver =
            Saver<ShellState, String>(
                save = { it.onboarding.name },
                restore = { ShellState(OnboardingPrompt.valueOf(it)) },
            )
    }
}
