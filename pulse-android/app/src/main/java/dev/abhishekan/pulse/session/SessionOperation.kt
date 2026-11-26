package dev.abhishekan.pulse.session

import dev.abhishekan.pulse.core.ui.UiText

internal data class SessionOperation(
    val running: Boolean = false,
    val error: UiText? = null,
)
