package dev.abhishekan.pulse

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry

internal fun usesNavigationRail(): Boolean =
    InstrumentationRegistry
        .getInstrumentation()
        .targetContext.resources.configuration.smallestScreenWidthDp >= 600

internal fun ComposeTestRule.openProfileFromNavigation() {
    if (usesNavigationRail()) {
        onNode(hasText("You") and isSelectable()).performScrollTo().performClick()
    } else {
        onNode(hasText("Home") and isSelectable()).performClick()
        onNodeWithContentDescription("Profile").performScrollTo().performClick()
    }
}

internal fun ComposeTestRule.assertNestedNavigationForLayout() {
    listOf("Home", "Fitness", "Horizon", "Garage", "Meals", "You").forEach { label ->
        val item = onNode(hasText(label) and isSelectable())
        if (usesNavigationRail()) {
            item.performScrollTo().assertIsDisplayed()
        } else {
            item.assertDoesNotExist()
        }
    }
}

internal fun ComposeTestRule.returnHomeFromProfile() {
    if (usesNavigationRail()) {
        onNode(hasText("Home") and isSelectable()).performScrollTo().performClick()
    } else {
        onNodeWithContentDescription("Back to Home").performClick()
    }
}
