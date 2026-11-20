package dev.abhishekan.pulse.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun NotificationsScreenContent(
    state: NotificationsUiState,
    timeZone: ZoneId,
    onAction: (NotificationsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val operation = state.operation
    val pullRefreshing = operation is NotificationsOperation.Refreshing && operation.fromPull
    val reviewRequired = state.review != NotificationsReview.None
    val loadingLabel = stringResource(R.string.pulse_notifications_loading)
    val locale = LocalConfiguration.current.locales[0]
    val formatter =
        remember(locale, timeZone) {
            DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", locale).withZone(timeZone)
        }

    PulseAdaptiveLayout(modifier.fillMaxSize()) { size ->
        val padding = size.pagePadding
        val scaffoldPadding = LocalPulseContentPadding.current
        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = { if (state.canRefresh) onAction(NotificationsAction.PullToRefresh) },
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = padding,
                        top = padding + scaffoldPadding.calculateTopPadding(),
                        end = padding,
                        bottom = padding + scaffoldPadding.calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
            ) {
                if (reviewRequired) {
                    item(key = "review", contentType = "review") {
                        NotificationReviewCard(
                            canAcknowledge = state.canAcknowledge,
                            onAcknowledge = { onAction(NotificationsAction.AcknowledgeReview) },
                        )
                    }
                }
                if (operation == NotificationsOperation.Mutating ||
                    (reviewRequired && operation is NotificationsOperation.Refreshing && !pullRefreshing)
                ) {
                    item(key = "progress", contentType = "progress") { PulseLoadingState(loadingLabel) }
                }
                when {
                    operation is NotificationsOperation.RefreshFailed -> {
                        item(key = "error", contentType = "error") {
                            PulseErrorState(operation.failure.message, { onAction(NotificationsAction.Refresh) })
                        }
                    }
                    state.confirmedRows?.isEmpty() == true -> {
                        item(key = "empty", contentType = "empty") {
                            PulseEmptyState(
                                stringResource(R.string.pulse_notifications_empty),
                                stringResource(R.string.pulse_notifications_empty_body),
                            )
                        }
                    }
                }
                items(state.rows, key = { it.id }, contentType = { "notification" }) { row ->
                    NotificationCard(
                        row = row,
                        formatter = formatter,
                        canDismiss = state.canChange,
                        canOpen = state.canOpen,
                        onDismiss = { onAction(NotificationsAction.Dismiss(row.id)) },
                        onOpen = { onAction(NotificationsAction.Open(row.id)) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            if (state.confirmedRows == null && operation !is NotificationsOperation.RefreshFailed && !reviewRequired && !pullRefreshing) {
                PulseCenteredLoadingState(loadingLabel, Modifier.fillMaxSize().padding(scaffoldPadding))
            }
        }
    }
}

@Composable
private fun NotificationReviewCard(
    canAcknowledge: Boolean,
    onAcknowledge: () -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_notifications_uncertain),
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        PulseButton(
            stringResource(R.string.pulse_notifications_reviewed),
            onAcknowledge,
            enabled = canAcknowledge,
        )
    }
}
