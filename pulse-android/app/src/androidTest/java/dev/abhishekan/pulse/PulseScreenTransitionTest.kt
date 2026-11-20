package dev.abhishekan.pulse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PulseScreenTransitionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nestedPagesEnterFromTheRightAndParentsReturnFromTheLeft() {
        val depth = mutableIntStateOf(0)
        compose.setContent {
            PulseScreenTransition(depth.intValue, screenDepth = { it }) { page, active ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (active) Box(Modifier.size(24.dp).testTag("page-$page"))
                }
            }
        }

        fun center(page: Int): Float =
            compose
                .onNodeWithTag("page-$page")
                .fetchSemanticsNode()
                .boundsInRoot.center.x

        val restingCenter = center(0)
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { depth.intValue = 1 }
        compose.mainClock.advanceTimeBy(64)
        assertTrue(center(1) > restingCenter)
        compose.onNodeWithTag("page-0").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(500)
        assertEquals(restingCenter, center(1), 1f)

        compose.runOnIdle { depth.intValue = 2 }
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { depth.intValue = 1 }
        compose.mainClock.advanceTimeBy(64)
        assertTrue(center(1) < restingCenter)
        compose.mainClock.advanceTimeBy(500)
        assertEquals(restingCenter, center(1), 1f)
    }
}
