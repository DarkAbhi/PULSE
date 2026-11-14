package dev.abhishekan.pulse

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {
    @Test fun onlyRootScreensUseTabTransitions() {
        AppDestination.tabs.forEach { assertTrue(isTabRoot(it, editName = false)) }
        assertFalse(isTabRoot(AppDestination.Profile, editName = true))
        assertFalse(isTabRoot(AppDestination.Vehicle, editName = false))
        assertFalse(isTabRoot(AppDestination.Notifications, editName = false))
    }
}
