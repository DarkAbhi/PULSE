package dev.abhishekan.pulse.core.ui

import dev.abhishekan.pulse.core.ui.R

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText

import dev.abhishekan.pulse.core.domain.model.Failure

val Failure.message: UiText get() = when (this) {
    Failure.InvalidInput -> uiText(R.string.pulse_ui_check_your_username_and_password)
    Failure.InvalidCredentials -> uiText(R.string.pulse_ui_that_username_or_password_doesn_t_match_please_try)
    Failure.Unauthorized -> uiText(R.string.pulse_ui_your_session_has_expired_please_sign_in_again)
    Failure.Connection -> uiText(R.string.pulse_ui_we_couldn_t_connect_check_your_connection_and_try)
    Failure.Timeout -> uiText(R.string.pulse_ui_the_request_took_too_long_please_try_again)
    Failure.Server -> uiText(R.string.pulse_ui_pulse_is_temporarily_unavailable_please_try_again_shortly)
    Failure.Malformed -> uiText(R.string.pulse_ui_we_couldn_t_read_this_update_please_try_again)
    Failure.NotFound -> uiText(R.string.pulse_ui_this_record_is_no_longer_available_refresh_to_see)
    Failure.Storage -> uiText(R.string.pulse_ui_we_couldn_t_access_secure_session_storage_please_try)
}
