package dev.abhishekan.pulse

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.gallery.PulseComponentGallery
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GalleryDeviceTest {
    private lateinit var restoration: StateRestorationTester
    @Before fun setup() {
        restoration = StateRestorationTester(compose)
        restoration.setContent { PulseTheme { PulseComponentGallery() } }
    }
    @get:Rule val compose = createComposeRule()

    @Test fun galleryActionsAndStateTabWorkAfterRecreation() {
        compose.onNodeWithText("DESIGN SYSTEM · DEBUG PREVIEW").assertIsDisplayed()
        compose.onNodeWithText("Continue · sample").performScrollTo().performClick()
        compose.onNodeWithText("Sample action completed").assertIsDisplayed()
        compose.onNodeWithText("States").performClick()
        compose.onNodeWithText("Loading sample section").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Loading sample section").assertIsDisplayed()
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        compose.onNodeWithText("Sample retry complete").assertIsDisplayed()
    }

    @Test fun labeledCheckboxExposesCheckedSemantics() {
        compose.onNodeWithText("Sample selection").performScrollTo().assertIsOn().performClick().assertIsOff()
    }
}
