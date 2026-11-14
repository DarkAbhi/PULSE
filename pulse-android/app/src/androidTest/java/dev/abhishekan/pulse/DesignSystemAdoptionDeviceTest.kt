package dev.abhishekan.pulse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseExtendedFloatingActionButton
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseSwitch
import dev.abhishekan.pulse.core.ui.components.PulseText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DesignSystemAdoptionDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun extendedFloatingActionKeepsItsVisibleLabelAccessible() {
        var clicks = 0
        compose.setContent {
            PulseTheme {
                PulseExtendedFloatingActionButton(
                    text = { PulseText("Add custom meal time") },
                    icon = { Box(Modifier.size(24.dp)) },
                    onClick = { clicks++ },
                )
            }
        }
        compose.onNodeWithText("Add custom meal time").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun longActionLabelsReserveSpaceForTrailingIcons() {
        compose.setContent {
            PulseTheme {
                PulseButton(
                    "View a vehicle with a very long descriptive name",
                    {},
                    Modifier.width(240.dp),
                    trailingIcon = { Box(Modifier.size(24.dp).testTag("trailing-icon")) },
                )
            }
        }
        compose.onNodeWithTag("trailing-icon", useUnmergedTree = true).assertWidthIsEqualTo(24.dp)
    }

    @Test fun sharedSelectionControlsKeepSingleCallbacksAndSeparateMetadata() {
        var checked by mutableStateOf(false)
        var selected by mutableStateOf(false)
        var switchCalls = 0
        var radioCalls = 0
        var chipCalls = 0
        compose.setContent {
            PulseTheme {
                Column {
                    PulseSwitch(
                        checked,
                        {
                            checked = it
                            switchCalls++
                        },
                        "Active",
                        Modifier.testTag("active"),
                        supportingText = "Include in forecast",
                        switchAtEnd = true,
                    )
                    PulseRadioButton(
                        selected,
                        {
                            selected = true
                            radioCalls++
                        },
                        "Exercise",
                        Modifier.testTag("exercise"),
                        supportingText = "Equipment and muscles",
                    )
                    PulseFilterChip(
                        "Meal",
                        selected,
                        { chipCalls++ },
                        Modifier.testTag("meal"),
                        leadingIcon = { Box(Modifier.size(24.dp)) },
                    )
                    PulseFilterChip("Disabled", false, { chipCalls++ }, enabled = false)
                }
            }
        }
        compose.onNodeWithTag("active").performClick().assertIsOn()
        compose.onNodeWithText("Include in forecast", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("exercise").performClick().assertIsSelected()
        compose.onNodeWithText("Exercise", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Equipment and muscles", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("meal").assertIsSelected().performClick()
        compose.onNodeWithText("Disabled").assertIsNotEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, switchCalls)
            assertEquals(1, radioCalls)
            assertEquals(1, chipCalls)
        }
    }

    @Test fun selectionTriggerAndMenuKeepEnabledStatesAndDismissal() {
        var selected = -1
        var menuOpen by mutableStateOf(true)
        var menuCalls = 0
        compose.setContent {
            PulseTheme {
                Column {
                    PulseSelectionField(
                        "Budget",
                        "None",
                        listOf("None", "Household"),
                        { selected = it },
                        triggerModifier = Modifier.testTag("budget-trigger"),
                    )
                    PulseDropdownMenu(menuOpen, { menuOpen = false }) {
                        PulseDropdownMenuItem({ PulseText("Unavailable") }, { menuCalls++ }, enabled = false)
                        PulseDropdownMenuItem({ PulseText("Choose") }, {
                            menuCalls++
                            menuOpen = false
                        })
                    }
                }
            }
        }
        compose.onNodeWithText("Unavailable").assertIsNotEnabled().performClick()
        compose.onNodeWithText("Choose").performClick()
        compose.onNodeWithText("Choose").assertDoesNotExist()
        compose.onNodeWithTag("budget-trigger").performClick()
        compose.onNodeWithText("Household").performClick()
        compose.onNodeWithText("Household").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, selected)
            assertEquals(1, menuCalls)
        }
    }
}
