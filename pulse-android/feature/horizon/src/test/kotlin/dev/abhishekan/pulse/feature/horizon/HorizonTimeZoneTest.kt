package dev.abhishekan.pulse.feature.horizon

import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HorizonTimeZoneTest {
    @After fun reset() = UserTimeZone.reset()

    @Test fun entryUsesProfileZoneAndRestoredDraftKeepsItsZone() {
        UserTimeZone.select("Asia/Kolkata")
        val draft = HorizonDraft(HorizonForm.Transaction, name = "Lunch", amount = "12", date = "2026-10-28", time = "12:00")
        UserTimeZone.select("UTC")
        val change = HorizonDraft.restore(draft.saved()).change() as HorizonChange.Transaction
        assertEquals(Instant.parse("2026-10-28T06:30:00Z"), change.date)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonexistentDaylightSavingTimeIsRejected() {
        HorizonDraft(HorizonForm.Transaction, amount = "1", date = "2026-03-08", time = "02:30", zone = "America/New_York").change()
    }

    @Test fun unchangedOverlapKeepsOriginalInstant() {
        val original = "2026-11-01T06:30:00Z"
        val change =
            HorizonDraft(
                HorizonForm.Transaction,
                amount = "1",
                date = "2026-11-01",
                time = "01:30",
                zone = "America/New_York",
                originalInstant = original,
            ).change() as HorizonChange.Transaction
        assertEquals(Instant.parse(original), change.date)
    }
}
