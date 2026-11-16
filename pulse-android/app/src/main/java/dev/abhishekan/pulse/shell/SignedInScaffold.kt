package dev.abhishekan.pulse.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.LocalPulseNavigationHidingScreens
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseBottomNavigationBar
import dev.abhishekan.pulse.core.ui.components.PulseNavigationItem
import dev.abhishekan.pulse.core.ui.components.PulseNavigationRail
import dev.abhishekan.pulse.core.ui.components.PulseSnackbar
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarProvider
import dev.abhishekan.pulse.navigation.AppDestination

@Composable
internal fun SignedInScaffold(
    destination: AppDestination?,
    isTablet: Boolean,
    snackbar: SnackbarHostState,
    selectDestination: (AppDestination) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val hidingScreens = remember { mutableStateMapOf<Any, Unit>() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Some keyboards resize the root before IME insets arrive; preserve space for the form.
        val showBars = maxHeight >= PulseTokens.minimumNavigationHeight
        val showNavigation = destination != AppDestination.Gallery && showBars
        val tabs = if (isTablet) AppDestination.tabs else AppDestination.bottomTabs
        val selectedIndex = tabs.indexOf(destination?.tab)
        val navigationItems =
            tabs.map { tab ->
                PulseNavigationItem(stringResource(tab.label)) {
                    Icon(painterResource(checkNotNull(tab.icon)), null)
                }
            }
        val onNavigationSelected: (Int) -> Unit = { index ->
            val target = tabs[index]
            if (target != destination?.tab || destination == AppDestination.Notifications) selectDestination(target)
        }
        Row(Modifier.fillMaxSize().background(PulseTheme.colors.background)) {
            if (isTablet && showNavigation) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                        .padding(
                            start = PulseTheme.spacing.cardPadding,
                            end = PulseTheme.spacing.medium,
                            top = PulseTheme.spacing.medium,
                            bottom = PulseTheme.spacing.medium,
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    PulseNavigationRail(navigationItems, selectedIndex, onNavigationSelected)
                }
            }
            Scaffold(
                containerColor = PulseTheme.colors.background,
                contentColor = PulseTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isTablet && showNavigation && destination in tabs && hidingScreens.isEmpty()) {
                        PulseBottomNavigationBar(
                            navigationItems,
                            selectedIndex,
                            onNavigationSelected,
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) { PulseSnackbar(it) } },
            ) { insets ->
                CompositionLocalProvider(LocalPulseNavigationHidingScreens provides hidingScreens) {
                    PulseSnackbarProvider(snackbar) {
                        content(Modifier.padding(insets).consumeWindowInsets(insets).clipToBounds())
                    }
                }
            }
        }
    }
}
