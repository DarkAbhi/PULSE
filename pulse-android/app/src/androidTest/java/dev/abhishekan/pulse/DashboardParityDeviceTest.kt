package dev.abhishekan.pulse

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.abhishekan.pulse.core.domain.model.FitnessToday
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.QuestActivity
import dev.abhishekan.pulse.core.domain.model.QuestActivityDay
import dev.abhishekan.pulse.core.domain.model.QuestSettings
import dev.abhishekan.pulse.core.domain.model.QuestSettingsView
import dev.abhishekan.pulse.core.domain.model.RecentActivityEntry
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.home.HomeScreen
import dev.abhishekan.pulse.feature.home.HomeUiState
import dev.abhishekan.pulse.feature.home.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DashboardParityDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dashboardShowsTheNewSectionsAndSupportsCompactDensity() {
        val date = LocalDate.parse("2026-11-26")
        val instant = Instant.parse("2026-11-26T12:00:00Z")
        val compact = mutableStateOf(false)
        var questOpens = 0
        val state =
            HomeUiState(
                profile = Section.Content(Profile("Test profile")),
                workout = Section.Empty,
                horizon = Section.Empty,
                fitnessToday = Section.Content(FitnessToday(date, 4_000, 312.0, 350.0, 21, 30, 9, 8, 26_400)),
                quests = Section.Content(QuestActivity(date, listOf(QuestActivityDay(date, listOf("daily_meals"))))),
                questSettings = Section.Content(QuestSettingsView(true, QuestSettings(stepTarget = 5_000), date, false)),
                recentActivity =
                    Section.Content(
                        listOf(
                            RecentActivityEntry(
                                "meal-1",
                                "meal_added",
                                instant,
                                mapOf("name" to "Paneer burrito bowl", "meal_time" to "Lunch"),
                            ),
                        ),
                    ),
            )
        compose.setContent {
            PulseTheme(false) {
                HomeScreen(
                    username = "fixture",
                    state = state,
                    onAction = {},
                    onHorizon = {},
                    onFitness = {},
                    onNotifications = {},
                    now = instant,
                    onQuests = { questOpens++ },
                    dashboardCompact = compact.value,
                )
            }
        }

        val full = compose.onNodeWithTag("dashboard-greeting").getUnclippedBoundsInRoot()
        compose.runOnIdle { compact.value = true }
        val dense = compose.onNodeWithTag("dashboard-greeting").getUnclippedBoundsInRoot()
        assertTrue(dense.bottom - dense.top < full.bottom - full.top)
        compose.onNodeWithText("Today at a glance").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Quest activity").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("View quests").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, questOpens) }
        compose.onNodeWithText("Recent activity").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Paneer burrito bowl · Lunch").performScrollTo().assertIsDisplayed()
    }
}
