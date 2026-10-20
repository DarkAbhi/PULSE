package dev.abhishekan.pulse.feature.notifications
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.pulseDescription
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationsRoute(
    onOpen: (NotificationTarget) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseSnackbarEffect(state.message.takeUnless { state.uncertain }, viewModel::consumeMessage)
    NotificationsScreen(state, viewModel::refresh, viewModel::dismiss, viewModel::acknowledge, onOpen)
}

@Composable
internal fun NotificationsScreen(
    state: NotificationsUiState,
    refresh: () -> Unit,
    dismiss: (Long) -> Unit,
    reviewed: () -> Unit,
    open: (NotificationTarget) -> Unit,
) {
    val enabled = !state.busy && !state.loading && !state.uncertain
    PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
        val padding = size.pagePadding
        PullToRefreshBox(
            isRefreshing = state.loading && state.loaded,
            onRefresh = { if (!state.busy && !state.loading) refresh() },
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.TopCenter,
        ) {
            LazyColumn(
                Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = padding,
                        top = padding + LocalPulseContentPadding.current.calculateTopPadding(),
                        end = padding,
                        bottom = padding + LocalPulseContentPadding.current.calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
            ) {
                if (state.uncertain) {
                    item {
                        PulseCard(Modifier.fillMaxWidth()) {
                            PulseText(
                                state.message ?: uiText(R.string.pulse_notifications_uncertain),
                                Modifier.semantics {
                                    liveRegion =
                                        LiveRegionMode.Polite
                                },
                            )
                            PulseButton(
                                stringResource(R.string.pulse_notifications_reviewed),
                                reviewed,
                                enabled =
                                    state.reviewed && !state.loading,
                            )
                        }
                    }
                }
                if ((state.loading && !state.loaded) ||
                    state.busy
                ) {
                    item { PulseLoadingState(stringResource(R.string.pulse_notifications_loading)) }
                }
                state.failure?.let { failure -> item { PulseErrorState(failure.message, refresh) } }
                if (state.loaded &&
                    state.items.isEmpty()
                ) {
                    item {
                        PulseEmptyState(
                            stringResource(R.string.pulse_notifications_empty),
                            stringResource(R.string.pulse_notifications_empty_body),
                        )
                    }
                }
                items(state.items, key = { it.id }) { notification ->
                    PulseCard(Modifier.fillMaxWidth()) {
                        PulseText(
                            notification.source.ifBlank {
                                stringResource(R.string.pulse_notifications_source)
                            },
                            style = PulseTextStyle.Label,
                            color = PulseTheme.colors.action,
                        )
                        PulseText(notification.title, Modifier.semantics { heading() }, PulseTextStyle.Title)
                        notification.body?.takeIf(String::isNotBlank)?.let { PulseText(it) }
                        if (notification.priority in
                            1..2
                        ) {
                            PulseText(
                                stringResource(R.string.pulse_notifications_priority, notification.priority),
                                style = PulseTextStyle.Label,
                            )
                        }
                        notification.createdAt?.let {
                            PulseText(
                                DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(it),
                            )
                        }
                        NotificationTarget.parse(notification.targetPath)?.let { target ->
                            PulseTextButton(
                                stringResource(R.string.pulse_notifications_open),
                                {
                                    open(target)
                                },
                                Modifier.pulseDescription(
                                    stringResource(R.string.pulse_notifications_open_title, notification.title),
                                ),
                                enabled = !state.busy,
                            )
                        }
                        PulseTextButton(
                            stringResource(R.string.pulse_notifications_dismiss),
                            {
                                dismiss(notification.id)
                            },
                            Modifier.pulseDescription(
                                stringResource(R.string.pulse_notifications_dismiss_title, notification.title),
                            ),
                            enabled = enabled,
                        )
                    }
                }
            }
        }
    }
}
