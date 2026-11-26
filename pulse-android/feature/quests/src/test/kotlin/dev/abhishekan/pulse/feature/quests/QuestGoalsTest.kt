package dev.abhishekan.pulse.feature.quests

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestGoalsTest {
    @Test fun sleepGoalsUseHoursAndMinutesWithoutRounding() {
        assertEquals(480, parseSleepGoal("8"))
        assertEquals(450, parseSleepGoal(" 7:30 "))
        assertEquals(1, parseSleepGoal("0:01"))
        assertEquals(1_440, parseSleepGoal("24:00"))
    }

    @Test fun invalidOrBlankGoalsCannotBeSavedAsAValue() {
        listOf("", "0", "0:00", "8:60", "7:5", "24:01", "-1", "8.5").forEach {
            assertNull(parseSleepGoal(it))
        }
    }
}
