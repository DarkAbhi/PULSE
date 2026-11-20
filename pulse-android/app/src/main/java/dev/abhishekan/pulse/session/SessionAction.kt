package dev.abhishekan.pulse.session

internal enum class SessionAction {
    Retry,
    CheckExpiry,
    LogOut,
    Forget,
    ClearError,
}
