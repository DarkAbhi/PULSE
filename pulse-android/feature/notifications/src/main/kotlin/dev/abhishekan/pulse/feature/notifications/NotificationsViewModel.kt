package dev.abhishekan.pulse.feature.notifications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.NotificationsRepository
import dev.abhishekan.pulse.core.domain.usecase.DismissNotification
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PENDING_MUTATION_KEY = "pending"

class NotificationsViewModel(
    private val notifications: NotificationsRepository,
    private val dismiss: DismissNotification,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val state =
        MutableStateFlow(
            NotificationsUiState(
                review =
                    if (saved.get<Boolean>(PENDING_MUTATION_KEY) == true) {
                        NotificationsReview.NeedsRefresh
                    } else {
                        NotificationsReview.None
                    },
            ),
        )
    internal val uiState = state.asStateFlow()
    private val navigationEvents = MutableSharedFlow<NotificationsEvent>(extraBufferCapacity = 1)
    internal val events = navigationEvents.asSharedFlow()

    internal fun onAction(action: NotificationsAction) {
        when (action) {
            NotificationsAction.Refresh -> refresh()
            NotificationsAction.PullToRefresh -> refresh(fromPull = true)
            is NotificationsAction.Open -> open(action.id)
            is NotificationsAction.Dismiss -> mutate(action.id)
            NotificationsAction.ClearAll -> mutate()
            NotificationsAction.AcknowledgeReview -> acknowledgeReview()
            NotificationsAction.MessageShown -> state.update { it.copy(message = null) }
        }
    }

    private fun open(id: Long) {
        val current = state.value
        if (!current.canOpen) return

        val target = current.rows.firstOrNull { it.id == id }?.target ?: return
        navigationEvents.tryEmit(NotificationsEvent.OpenTarget(target))
    }

    private fun refresh(fromPull: Boolean = false) {
        if (!state.value.canRefresh) return

        state.update {
            it.copy(
                operation = NotificationsOperation.Refreshing(fromPull),
                review = if (it.review == NotificationsReview.Ready) NotificationsReview.NeedsRefresh else it.review,
            )
        }
        viewModelScope.launch { fetchRows() }
    }

    private suspend fun fetchRows() {
        when (val result = notifications.list()) {
            is Outcome.Success -> {
                val rows = result.value.map { NotificationRow(it, NotificationTarget.parse(it.targetPath)) }
                state.update {
                    it.copy(
                        confirmedRows = rows,
                        operation = NotificationsOperation.Idle,
                        review = if (it.review == NotificationsReview.NeedsRefresh) NotificationsReview.Ready else it.review,
                    )
                }
            }
            is Outcome.Error -> state.update { it.copy(operation = NotificationsOperation.RefreshFailed(result.failure)) }
        }
    }

    private fun acknowledgeReview() {
        if (!state.value.canAcknowledge) return
        saved.remove<Boolean>(PENDING_MUTATION_KEY)
        state.update { it.copy(review = NotificationsReview.None) }
    }

    private fun mutate(id: Long? = null) {
        val current = state.value
        if (!current.canChange || (id != null && current.rows.none { it.id == id })) return

        saved[PENDING_MUTATION_KEY] = true
        state.update { it.copy(operation = NotificationsOperation.Mutating, message = null) }
        viewModelScope.launch {
            val result = if (id == null) notifications.clear() else dismiss(id)
            when (result) {
                is Outcome.Success -> {
                    saved.remove<Boolean>(PENDING_MUTATION_KEY)
                    state.update {
                        it.copy(
                            operation = NotificationsOperation.Refreshing(fromPull = false),
                            confirmedRows = if (id == null) emptyList() else it.rows.filterNot { row -> row.id == id },
                            message = uiText(R.string.pulse_notifications_dismissed),
                        )
                    }
                    fetchRows()
                }
                is Outcome.Error -> {
                    val reviewRequired =
                        when (result.failure) {
                            Failure.Connection, Failure.Timeout, Failure.Server, Failure.Malformed -> true
                            else -> false
                        }
                    if (!reviewRequired) saved.remove<Boolean>(PENDING_MUTATION_KEY)
                    state.update {
                        it.copy(
                            operation =
                                if (reviewRequired) {
                                    NotificationsOperation.Refreshing(fromPull = false)
                                } else {
                                    current.operation
                                },
                            review = if (reviewRequired) NotificationsReview.NeedsRefresh else NotificationsReview.None,
                            message = if (reviewRequired) null else result.failure.message,
                        )
                    }
                    if (reviewRequired) fetchRows()
                }
            }
        }
    }
}
