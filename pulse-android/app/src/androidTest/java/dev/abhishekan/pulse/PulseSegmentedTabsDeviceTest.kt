package dev.abhishekan.pulse

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseSegmentedTabs
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PulseSegmentedTabsDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tabsFillAvailableWidthEquallyAndRemainSelectable() {
        var selected by mutableIntStateOf(0)
        compose.setContent {
            PulseTheme {
                PulseSegmentedTabs(listOf("Choose vehicle", "Manual"), selected, { selected = it }, Modifier.width(320.dp))
            }
        }
        val first = compose.onNodeWithText("Choose vehicle").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("Manual").getUnclippedBoundsInRoot()
        assertEquals((first.right - first.left).value, (second.right - second.left).value, 1f)
        assertEquals(316f, second.right.value, 1f)
        compose.onNodeWithText("Manual").performClick().assertIsSelected()
    }
}
