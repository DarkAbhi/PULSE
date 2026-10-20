package dev.abhishekan.pulse.feature.notifications
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Notification
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.usecase.ClearNotifications
import dev.abhishekan.pulse.core.domain.usecase.DismissNotification
import dev.abhishekan.pulse.core.domain.usecase.GetNotificationCenter
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val items: List<Notification> = emptyList(),
    val loading: Boolean = true,
    val loaded: Boolean = false,
    val failure: Failure? = null,
    val busy: Boolean = false,
    val message: UiText? = null,
    val uncertain: Boolean = false,
    val reviewed: Boolean = false,
)

class NotificationsViewModel(
    private val get: GetNotificationCenter,
    private val dismiss: DismissNotification,
    private val clear: ClearNotifications,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val state =
        MutableStateFlow(
            NotificationsUiState(
                uncertain = saved["pending"] ?: false,
                message = if (saved.get<Boolean>("pending") == true) uiText(R.string.pulse_notifications_uncertain) else null,
            ),
        )
    val uiState = state.asStateFlow()

    fun consumeMessage() {
        state.update { it.copy(message = null) }
    }

    private var load: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (state.value.busy || load?.isActive == true) return
        state.update { it.copy(loading = true, failure = null, reviewed = false) }
        load =
            viewModelScope.launch {
                when (val result = get()) {
                    is Outcome.Success -> state.update { it.copy(items = result.value, loading = false, loaded = true, reviewed = true) }
                    is Outcome.Error -> state.update { it.copy(loading = false, failure = result.failure) }
                }
            }
    }

    fun acknowledge() {
        if (!state.value.reviewed) return
        saved.remove<Boolean>("pending")
        state.update { it.copy(uncertain = false, message = null) }
    }

    fun dismiss(id: Long) = mutate(id)

    fun clearAll() = mutate(null)

    private fun mutate(id: Long?) {
        if (state.value.busy || state.value.loading || state.value.uncertain) return
        state.update { it.copy(busy = true, message = null) }
        saved["pending"] = true
        viewModelScope.launch {
            val result = if (id == null) clear() else dismiss.invoke(id)
            when (result) {
                is Outcome.Success -> {
                    saved.remove<Boolean>("pending")
                    state.update {
                        it.copy(
                            busy = false,
                            items =
                                if (id ==
                                    null
                                ) {
                                    emptyList()
                                } else {
                                    it.items.filterNot { item -> item.id == id }
                                },
                            message = uiText(R.string.pulse_notifications_dismissed),
                        )
                    }
                    refresh()
                }
                is Outcome.Error -> {
                    val uncertain = result.failure in setOf(Failure.Connection, Failure.Timeout, Failure.Server, Failure.Malformed)
                    if (!uncertain) saved.remove<Boolean>("pending")
                    state.update {
                        it.copy(
                            busy = false,
                            uncertain = uncertain,
                            reviewed = false,
                            message = if (uncertain) uiText(R.string.pulse_notifications_uncertain) else result.failure.message,
                        )
                    }
                    if (uncertain) refresh()
                }
            }
        }
    }
}
