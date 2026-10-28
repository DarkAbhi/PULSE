package dev.abhishekan.pulse.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.NotificationTarget
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
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
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.uiText
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter

@Composable
fun NotificationsRoute(
    onOpen: (NotificationTarget) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(timeZone) { viewModel.refresh() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseSnackbarEffect(state.message.takeUnless { state.uncertain }, viewModel::consumeMessage)
    androidx.compose.runtime.key(timeZone) {
        NotificationsScreen(state, viewModel::refresh, viewModel::dismiss, viewModel::acknowledge, onOpen)
    }
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
                    val target = NotificationTarget.parse(notification.targetPath)
                    val openLabel = stringResource(R.string.pulse_notifications_open_title, notification.title)
                    val dismissLabel = stringResource(R.string.pulse_notifications_dismiss_title, notification.title)
                    val swipeState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(swipeState.settledValue) {
                        if (swipeState.settledValue != SwipeToDismissBoxValue.Settled) {
                            if (enabled) dismiss(notification.id)
                            // Keep the confirmed card available if the server rejects the change.
                            swipeState.reset()
                        }
                    }
                    SwipeToDismissBox(
                        state = swipeState,
                        gesturesEnabled = enabled,
                        modifier = Modifier.animateItem(),
                        backgroundContent = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(PulseTokens.cardShape)
                                    .background(PulseTheme.colors.errorSoft)
                                    .padding(PulseTokens.cardPadding)
                                    .clearAndSetSemantics {},
                                contentAlignment =
                                    if (swipeState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                        Alignment.CenterStart
                                    } else {
                                        Alignment.CenterEnd
                                    },
                            ) {
                                PulseText(stringResource(R.string.pulse_notifications_dismiss), color = PulseTheme.colors.error)
                            }
                        },
                    ) {
                        PulseCard(
                            Modifier
                                .fillMaxWidth()
                                .clip(PulseTokens.cardShape)
                                .then(
                                    if (target != null) {
                                        Modifier.clickable(
                                            enabled = !state.busy,
                                            onClickLabel = openLabel,
                                            role = Role.Button,
                                        ) { open(target) }
                                    } else {
                                        Modifier
                                    },
                                ).semantics(mergeDescendants = true) {
                                    if (enabled) {
                                        customActions =
                                            listOf(
                                                CustomAccessibilityAction(dismissLabel) {
                                                    dismiss(notification.id)
                                                    true
                                                },
                                            )
                                    }
                                },
                        ) {
                            PulseText(
                                notification.source.ifBlank {
                                    stringResource(R.string.pulse_notifications_source)
                                },
                                style = PulseTextStyle.Label,
                                color = PulseTheme.colors.action,
                            )
                            PulseText(notification.title, Modifier.semantics { heading() }, PulseTextStyle.Title)
                            notification.body?.takeIf(String::isNotBlank)?.let { PulseText(it) }
                            notification.createdAt?.let {
                                PulseText(
                                    DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm").withZone(UserTimeZone.zone).format(it),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
