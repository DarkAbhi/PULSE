package dev.abhishekan.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {
    @Test fun onlyRootScreensUseTabTransitions() {
        AppDestination.tabs.forEach { assertTrue(isTabRoot(it, editName = false)) }
        assertFalse(isTabRoot(AppDestination.Profile, editName = true))
        assertFalse(isTabRoot(AppDestination.Profile, editName = false, fromHome = true))
        assertFalse(isTabRoot(AppDestination.Vehicle, editName = false))
        assertFalse(isTabRoot(AppDestination.WorkoutExercise, editName = false))
        assertFalse(isTabRoot(AppDestination.Notifications, editName = false))
    }

    @Test fun exerciseEditorBelongsToTheWorkoutAndFitnessTab() {
        val destination = AppDestination.WorkoutExercise
        assertEquals(AppDestination.Fitness, destination.tab)
        assertEquals(AppDestination.Workout.withId(100L) + "/exercise", destination.withId(100L))
    }
}
