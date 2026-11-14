package dev.abhishekan.pulse

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import dev.abhishekan.pulse.core.ui.LocalPulseNavigationHidingScreens
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationVisibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun returningToRootAndDisposingNestedScreensRestoreNavigation() {
        val screens = mutableStateMapOf<Any, Unit>()
        val nested = mutableStateOf(false)
        val mounted = mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(LocalPulseNavigationHidingScreens provides screens) {
                if (mounted.value) PulseHideNavigation(nested.value)
            }
        }
        compose.runOnIdle {
            assertTrue(screens.isEmpty())
            nested.value = true
        }
        compose.runOnIdle {
            assertEquals(1, screens.size)
            nested.value = false
        }
        compose.runOnIdle {
            assertTrue(screens.isEmpty())
            nested.value = true
        }
        compose.runOnIdle {
            assertEquals(1, screens.size)
            mounted.value = false
        }
        compose.runOnIdle { assertTrue(screens.isEmpty()) }
    }

    @Test fun outgoingScreenCannotRestoreNavigationWhileAnotherNestedScreenIsOpen() {
        val screens = mutableStateMapOf<Any, Unit>()
        val outgoing = mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(LocalPulseNavigationHidingScreens provides screens) {
                if (outgoing.value) PulseHideNavigation(true)
                PulseHideNavigation(true)
            }
        }
        compose.runOnIdle {
            assertEquals(2, screens.size)
            outgoing.value = false
        }
        compose.runOnIdle { assertEquals(1, screens.size) }
    }
}
