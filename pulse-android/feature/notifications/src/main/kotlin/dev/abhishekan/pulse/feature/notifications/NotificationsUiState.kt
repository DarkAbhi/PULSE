package dev.abhishekan.pulse.feature.notifications

import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.ui.UiText

internal data class NotificationsUiState(
    val confirmedRows: List<NotificationRow>? = null,
    val operation: NotificationsOperation = NotificationsOperation.Idle,
    val review: NotificationsReview = NotificationsReview.None,
    val message: UiText? = null,
) {
    val rows: List<NotificationRow> get() = confirmedRows.orEmpty()
    val canRefresh: Boolean
        get() = operation == NotificationsOperation.Idle || operation is NotificationsOperation.RefreshFailed
    val canOpen: Boolean get() = operation != NotificationsOperation.Mutating
    val canChange: Boolean get() = canRefresh && review == NotificationsReview.None && rows.isNotEmpty()
    val canAcknowledge: Boolean get() = canRefresh && review == NotificationsReview.Ready
}

internal sealed interface NotificationsOperation {
    data object Idle : NotificationsOperation

    data class Refreshing(
        val fromPull: Boolean,
    ) : NotificationsOperation

    data object Mutating : NotificationsOperation

    data class RefreshFailed(
        val failure: Failure,
    ) : NotificationsOperation
}

internal enum class NotificationsReview {
    None,
    NeedsRefresh,
    Ready,
}

internal data class NotificationRow(
    val notification: Notification,
    val target: NotificationTarget?,
) {
    val id: Long get() = notification.id
}
