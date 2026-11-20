package dev.abhishekan.pulse.feature.notifications

internal sealed interface NotificationsAction {
    data object Refresh : NotificationsAction

    data object PullToRefresh : NotificationsAction

    data class Open(
        val id: Long,
    ) : NotificationsAction

    data class Dismiss(
        val id: Long,
    ) : NotificationsAction

    data object ClearAll : NotificationsAction

    data object AcknowledgeReview : NotificationsAction

    data object MessageShown : NotificationsAction
}
