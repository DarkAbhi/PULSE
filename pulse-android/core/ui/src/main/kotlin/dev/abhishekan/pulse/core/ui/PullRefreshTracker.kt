package dev.abhishekan.pulse.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Tracks only refreshes requested through the pull gesture or its accessibility action. */
class PullRefreshTracker {
    var requested by mutableStateOf(false)
        private set

    fun request(action: () -> Unit) {
        requested = true
        action()
    }

    internal fun finish() {
        requested = false
    }
}

@Composable
fun rememberPullRefreshTracker(loading: Boolean): PullRefreshTracker {
    val tracker = remember { PullRefreshTracker() }
    val currentLoading = rememberUpdatedState(loading)
    LaunchedEffect(tracker.requested) {
        if (tracker.requested) {
            val started = withTimeoutOrNull(2_000) { snapshotFlow { currentLoading.value }.first { it } }
            if (started == true) snapshotFlow { currentLoading.value }.first { !it }
            tracker.finish()
        }
    }
    return tracker
}
