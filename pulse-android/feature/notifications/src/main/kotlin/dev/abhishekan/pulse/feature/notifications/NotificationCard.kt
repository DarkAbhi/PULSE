package dev.abhishekan.pulse.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import java.time.format.DateTimeFormatter

@Composable
internal fun NotificationCard(
    row: NotificationRow,
    formatter: DateTimeFormatter,
    canDismiss: Boolean,
    canOpen: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notification = row.notification
    val target = row.target
    val openLabel = stringResource(R.string.pulse_notifications_open_title, notification.title)
    val dismissLabel = stringResource(R.string.pulse_notifications_dismiss_title, notification.title)
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    // Restoring a settled swipe would repeat a server mutation after recreation.
    val swipeState = remember(threshold) { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
    LaunchedEffect(swipeState.settledValue) {
        if (swipeState.settledValue != SwipeToDismissBoxValue.Settled) {
            if (canDismiss) onDismiss()
            swipeState.reset()
        }
    }

    SwipeToDismissBox(
        state = swipeState,
        gesturesEnabled = canDismiss,
        modifier = modifier,
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
        val openModifier =
            if (target == null) {
                Modifier
            } else {
                Modifier.clickable(enabled = canOpen, onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
            }
        PulseCard(
            Modifier
                .fillMaxWidth()
                .clip(PulseTokens.cardShape)
                .then(openModifier)
                .semantics(mergeDescendants = true) {
                    if (canDismiss) {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(dismissLabel) {
                                    onDismiss()
                                    true
                                },
                            )
                    }
                },
        ) {
            PulseText(
                notification.source.ifBlank { stringResource(R.string.pulse_notifications_source) },
                style = PulseTextStyle.Label,
                color = PulseTheme.colors.action,
            )
            PulseText(notification.title, Modifier.semantics { heading() }, PulseTextStyle.Title)
            notification.body?.takeIf(String::isNotBlank)?.let { PulseText(it) }
            notification.createdAt?.let { PulseText(formatter.format(it)) }
        }
    }
}
