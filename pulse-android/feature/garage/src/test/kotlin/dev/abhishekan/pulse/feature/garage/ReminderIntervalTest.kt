package dev.abhishekan.pulse.feature.garage

import dev.abhishekan.pulse.core.domain.model.MaintenanceInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderIntervalTest {
    @Test fun intervalsUseMonthsAndAllowAnUnusedLimit() {
        assertEquals(MaintenanceInterval(3000, 6), reminderInterval("3000", "6"))
        assertEquals(MaintenanceInterval(3000, 0), reminderInterval("3000", ""))
        assertEquals(MaintenanceInterval(0, 1), reminderInterval("", "1"))
        assertEquals(MaintenanceInterval(), reminderInterval("", ""))
    }

    @Test fun invalidWholeNumberLimitsCannotBeSaved() {
        for (km in listOf("0", "-1", "1.5", "1000001", "999999999999")) assertNull(reminderInterval(km, "6"))
        for (months in listOf("0", "-1", "0.5", "1201", "999999999999")) assertNull(reminderInterval("3000", months))
    }
}
