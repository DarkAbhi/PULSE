package dev.abhishekan.pulse.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf

val LocalPulseNavigationHidingScreens = staticCompositionLocalOf<SnapshotStateMap<Any, Unit>?> { null }

@Composable
fun PulseHideNavigation(hidden: Boolean) {
    val screens = LocalPulseNavigationHidingScreens.current
    val owner = remember { Any() }
    DisposableEffect(screens, hidden) {
        if (hidden) screens?.put(owner, Unit)
        onDispose { screens?.remove(owner) }
    }
}
