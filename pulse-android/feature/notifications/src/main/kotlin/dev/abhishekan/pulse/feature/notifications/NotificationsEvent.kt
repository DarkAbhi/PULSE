package dev.abhishekan.pulse.feature.notifications

import dev.abhishekan.pulse.core.domain.model.NotificationTarget

internal sealed interface NotificationsEvent {
    data class OpenTarget(
        val target: NotificationTarget,
    ) : NotificationsEvent
}
