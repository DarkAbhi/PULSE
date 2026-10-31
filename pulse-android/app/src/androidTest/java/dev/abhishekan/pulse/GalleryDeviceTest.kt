package dev.abhishekan.pulse

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
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

    private fun selectTab(label: String) {
        compose
            .onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .performScrollTo()
            .performClick()
            .assertIsSelected()
    }

    @Test fun galleryActionsAndStateTabWorkAfterRecreation() {
        compose.onNodeWithText("DESIGN SYSTEM · DEBUG PREVIEW").assertIsDisplayed()
        selectTab("Buttons")
        compose.onNodeWithText("Continue · sample").performScrollTo().performClick()
        compose.onNodeWithText("Sample action completed").assertIsDisplayed()
        selectTab("States")
        compose.onNodeWithText("Loading sample section").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Loading sample section").assertIsDisplayed()
        compose.onNodeWithText("Try again").performScrollTo().performClick()
        compose.onNodeWithText("Sample retry complete").assertIsDisplayed()
    }

    @Test fun labeledCheckboxRetainsStateAcrossTabsAndRecreation() {
        selectTab("Selection controls")
        compose
            .onNodeWithText("Sample selection")
            .performScrollTo()
            .assertIsOn()
            .performClick()
            .assertIsOff()
        selectTab("Cards")
        selectTab("Selection controls")
        compose.onNodeWithText("Sample selection").assertIsOff()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Sample selection").assertIsOff()
    }

    @Test fun inputRetainsValueAcrossTabs() {
        compose.onNodeWithText("Sample username").performScrollTo().performTextInput("alex")
        selectTab("Cards")
        selectTab("Inputs")
        compose.onNodeWithText("Sample username").assertTextContains("alex")
    }

    @Test fun everyTextStyleIsReachable() {
        selectTab("Typography")
        PulseTextStyle.entries.forEach { style ->
            val label = style.name + if (style == PulseTextStyle.Display) " · Heading1 alias" else ""
            compose.onNodeWithText(label).performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun everyCategoryShowsItsComponents() {
        listOf(
            "Buttons" to "Text actions",
            "Typography" to "Body",
            "Cards" to "Standard card",
            "Chips & pills" to "Interactive filter",
            "Selection controls" to "Checkboxes",
            "Navigation" to "Top app bar",
            "Feedback" to "Snackbar",
            "Dialogs" to "Alert dialog",
            "Menus" to "Dropdown menu",
            "Calendar" to "Today with activity",
            "States" to "Loading sample section",
            "Icons" to "Home",
            "Images" to "Pulse logo · 48dp",
            "Foundations" to "Color palette",
            "Appearance" to "Theme · applies to every gallery tab",
            "Inputs" to "Text fields · default, filled, supporting text",
        ).forEach { (tab, sample) ->
            selectTab(tab)
            compose.onNodeWithText(sample).performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun customDialogAndMenuAreInteractive() {
        selectTab("Dialogs")
        compose.onNodeWithText("Preview form dialog").performScrollTo().performClick()
        compose.onNodeWithText("Note").performTextInput("Sample note text")
        compose.onNodeWithText("Done").performClick()
        selectTab("Menus")
        compose.onNodeWithText("Open sample menu").performClick()
        compose.onNodeWithText("Plain item").performClick()
        compose.onNodeWithText("Plain item selected").assertIsDisplayed()
    }
}
