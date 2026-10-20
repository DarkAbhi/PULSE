package dev.abhishekan.pulse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseNavigationItem
import dev.abhishekan.pulse.core.ui.components.PulseNavigationRail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import dev.abhishekan.pulse.core.ui.R as UiR

class FloatingNavigationRailDeviceTest {
    @get:Rule val compose = createComposeRule()

    private val labels = listOf("Home", "Fitness", "Horizon", "Garage", "Meals", "You")

    @Test fun wrapsDestinationsAndResizesAroundViewportCenter() {
        val count = mutableIntStateOf(3)
        val selected = mutableIntStateOf(0)
        compose.setContent {
            PulseTheme {
                Box(Modifier.fillMaxSize().testTag("viewport"), contentAlignment = Alignment.CenterStart) {
                    PulseNavigationRail(
                        labels.take(count.intValue).map { label ->
                            PulseNavigationItem(label) { Icon(painterResource(UiR.drawable.pulse_home), null) }
                        },
                        selected.intValue,
                        { selected.intValue = it },
                        Modifier.padding(start = 24.dp).testTag("rail"),
                    )
                }
            }
        }
        val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot

        fun railBounds() = compose.onNodeWithTag("rail").fetchSemanticsNode().boundsInRoot
        val initial = railBounds()
        assertEquals(viewport.center.y, initial.center.y, 1f)
        assertTrue(initial.height < viewport.height)
        compose.runOnIdle { count.intValue = 6 }
        compose.waitForIdle()
        val expanded = railBounds()
        assertTrue(expanded.height > initial.height)
        assertEquals(viewport.center.y, expanded.center.y, 1f)
        labels.forEachIndexed { index, label ->
            compose
                .onNode(hasText(label) and isSelectable())
                .performScrollTo()
                .performClick()
                .assertIsSelected()
            compose.runOnIdle { assertEquals(index, selected.intValue) }
        }
        compose.runOnIdle { selected.intValue = 1 }
        compose.onNode(hasText("Fitness") and isSelectable()).assertIsSelected()
        compose.runOnIdle { count.intValue = 3 }
        compose.waitForIdle()
        assertEquals(initial.height, railBounds().height, 1f)
        assertEquals(viewport.center.y, railBounds().center.y, 1f)
    }

    @Test fun shortViewportAndLargeTextKeepEveryDestinationReachable() {
        val selected = mutableIntStateOf(0)
        compose.setContent {
            PulseTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                    Box(Modifier.height(240.dp).testTag("viewport"), contentAlignment = Alignment.CenterStart) {
                        PulseNavigationRail(
                            labels.map { label ->
                                PulseNavigationItem(label) { Icon(painterResource(UiR.drawable.pulse_home), null) }
                            },
                            selected.intValue,
                            { selected.intValue = it },
                            Modifier.testTag("rail"),
                        )
                    }
                }
            }
        }
        val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        val rail = compose.onNodeWithTag("rail").fetchSemanticsNode().boundsInRoot
        assertTrue(rail.height <= viewport.height)
        labels.reversed().forEach { label ->
            compose
                .onNode(hasText(label) and isSelectable())
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
                .assertIsSelected()
        }
        assertEquals(rail, compose.onNodeWithTag("rail").fetchSemanticsNode().boundsInRoot)
    }
}
